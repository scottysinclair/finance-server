package scott.financeserver.accounts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import scott.barleydb.api.core.Environment
import scott.barleydb.api.core.entity.EntityConstraint.mustNotExistInDatabase
import scott.barleydb.api.persist.Operation
import scott.barleydb.api.persist.OperationType
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.model.Transaction
import scott.financeserver.data.model.Category as ECategory
import scott.financeserver.data.model.CategoryMatcher as ECategoryMatcher
import scott.financeserver.data.query.QCategory
import scott.financeserver.data.query.QTransaction
import scott.financeserver.toSequence
import scott.financeserver.upload.Category
import scott.financeserver.upload.CategoryMatcher
import scott.financeserver.upload.CategoryResponse
import scott.financeserver.upload.toPersistRequest

@RestController
class CategoriesController {

    @Autowired
    private lateinit var env: Environment

    @GetMapping("/api/category")
    fun getCategories() = DataEntityContext(env).use { ctx ->
        ctx.performQuery(QCategory().apply {
            joinToMatchers()
            orderBy(name(), true)
        }).list.map {
            scott.financeserver.upload.Category(
                id = it.id,
                name = it.name,
                matchers = it.matchers.map { m ->
                    CategoryMatcher(
                        id = m.id,
                        pattern = m.pattern
                    )
                }
            )
        }
            .let { CategoryResponse(it) }
    }.also { Runtime.getRuntime().gc() }

    @PostMapping("/api/category")
    fun saveCategories(@RequestBody categories : List<Category>) = DataEntityContext(env).use { ctx ->
        ctx.autocommit = false
        try {
            ctx.performQuery(QCategory().apply { where(name().equal("unknown")) }).singleResult.let { unknown ->
                ctx.performQuery(QCategory().apply {
                    where(name().notEqual("unknown"))
                    and(id().notIn(categories.map { it.id }.toSet()))
                }).list.let { categoriesToDelete ->
                    if (categoriesToDelete.isNotEmpty()) {
                        //change Ts to category unknown if the C is being deleted
                        ctx.streamObjectQuery(QTransaction().apply {
                            where(categoryId().`in`(categoriesToDelete.map { it.id }.toSet()))
                        })
                            .toSequence()
                            .map { Operation(it.also { it.category = unknown }, OperationType.UPDATE) }
                            .chunked(100)
                            .forEach { ops ->
                                ctx.persist(ops.toPersistRequest())
                            }
                        //actually delete the cateories which have been removed
                        categoriesToDelete.map {
                            Operation(it, OperationType.DELETE)
                        }
                            .toPersistRequest().let {
                                ctx.persist(it)
                            }
                    }
                }
            }
            ctx.clear()
            val updatedCats = mutableSetOf<Category>()
            ctx.performQuery(QCategory().apply {
                joinToMatchers()
                where(name().notEqual("unknown"))
                and(id().`in`(categories.map { it.id }.toSet()))
            }).list
                .asSequence()
                .map { ecat -> ecat.apply {
                    categories.find { c -> c.id == ecat.id }?.let { c ->
                        updatedCats.add(c)
                        ecat.name = c.name
                        ecat.matchers.apply {
                            val uimatchers = c.matchers.toMutableList()
                            addAll(map { orig -> orig to uimatchers.find { ui -> ui.id == orig.id }?.also { v -> uimatchers.remove(v) } }
                                .mapNotNull { (orig, ui) -> if (ui != null) orig.apply { pattern = ui.pattern } else null }
                                .also { clear() })
                            addAll(uimatchers.map { ui ->
                                ctx.newModel(ECategoryMatcher::class.java, ui.id, mustNotExistInDatabase()).apply {
                                    category = ecat
                                    pattern = ui.pattern
                                }
                            })
                        }
                    }
                } ?: throw IllegalStateException("Not found category which was just loaded: $ecat")
                }.map { Operation(it, OperationType.UPDATE) }
                .toList().let { updateOps ->
                    updateOps + categories.filterNot { c -> c.name == "unknown" || updatedCats.contains(c) }
                        .map { c -> ctx.newModel(ECategory::class.java, c.id, mustNotExistInDatabase()).also { ecat ->
                            println("ADDING NEW CATEGORY ${c.name}")
                            ecat.name = c.name
                            ecat.matchers.apply {
                              addAll(c.matchers.map { cm ->
                                  ctx.newModel(ECategoryMatcher::class.java, cm.id, mustNotExistInDatabase()).also {
                                      it.category = ecat
                                      it.pattern = cm.pattern
                                  }
                              })
                            }
                        } }
                        .map { c -> Operation(c, OperationType.INSERT) }
                }
                .asSequence()
                .chunked(100)
                .forEach { ops ->
                    ctx.persist(ops.toPersistRequest())
                }
            ctx.commit()
        }
        catch(x : Exception) {
            x.printStackTrace()
            runCatching { ctx.rollback() }.onFailure { x2 -> x2.printStackTrace() }
        }
    }.also { Runtime.getRuntime().gc() }
        .also { applyCategories() }

    @PostMapping("/api/category/apply")
    fun applyCategories() : Unit = DataEntityContext(env).use { ctx ->
        ctx.autocommit = false
        val categories = ctx.performQuery(QCategory().apply {
            joinToMatchers()
        }).list
        val unknownCategory = categories.find { c -> c.name == "unknown" }!!
        try {
            ctx.streamObjectQuery(QTransaction().apply {
                where(duplicate().equal(false))
                and(userCategorized().equal(false))
                orderBy(date(), true) })
                .toSequence()
                .map { t -> t.category to t.apply { anaylseCategory(t, categories, unknownCategory) } }
                .filter { (oldCat, t) ->  oldCat.id != t.category.id }
                .map { (_, t) -> t}
                .map { t -> Operation(t, OperationType.UPDATE) }
                .chunked(100)
                .forEach { ops ->
                    ctx.persist(ops.toPersistRequest())
                }
            ctx.commit()
        }
        catch(x : Exception) {
            x.printStackTrace()
            runCatching { ctx.rollback() }.onFailure { x2 -> x.printStackTrace() }
        }
    }

    private val cachedRegex = mutableMapOf<String, Regex>()
    @Synchronized
    private fun anaylseCategory(transaction: Transaction, categories : List<ECategory>, unknownCategory : ECategory)  {
        transaction.category = categories.find { c ->
            c.matchers.map { m -> cachedRegex.getOrPut(m.pattern, { Regex(m.pattern, RegexOption.IGNORE_CASE) }) }
                .any { regex -> regex.containsMatchIn(transaction.description) }
        } ?: unknownCategory
    }

/*
    @PostMapping("/api/category/apply")
    fun applyCategories() : Unit = DataEntityContext(env).use { ctx ->
        ctx.autocommit = false
        val categories = ctx.performQuery(QCategory().apply {
            joinToMatchers()
        }).list
        val unknownCategory = categories.find { c -> c.name == "unknown" }!!
        try {
            ctx.streamObjectQuery(QTransaction().apply {
                where(duplicate().equal(false))
                and(userCategorized().equal(false))
                orderBy(date(), true) })
                .toSequence()
                .map { t -> t.category to t.apply { anaylseCategory(t, categories, unknownCategory) } }
                .filter { (oldCat, t) ->  oldCat.id != t.category.id }
                .map { (_, t) -> t}
                .map { t -> Operation(t, OperationType.UPDATE) }
                .batchesOf(100)
                .forEach { ops ->
                    ctx.persist(ops.toPersistRequest())
                }
            ctx.commit()
        }
        catch(x : Exception) {
            x.printStackTrace()
            runCatching { ctx.rollback() }.onFailure { x2 -> x.printStackTrace() }
        }
    }

    private val cachedRegex = mutableMapOf<String, Regex>()
    @Synchronized
    private fun anaylseCategory(transaction: Transaction, categories : List<Category>, unknownCategory : Category)  {
        transaction.category = categories.find { c ->
            c.matchers.map { m -> cachedRegex.getOrPut(m.pattern, { Regex(m.pattern, RegexOption.IGNORE_CASE) }) }
                .any { regex -> regex.containsMatchIn(transaction.description) }
        } ?: unknownCategory
    }
*/

}