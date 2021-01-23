package scott.financeserver.accounts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import scott.barleydb.api.core.Environment
import scott.barleydb.api.core.entity.EntityConstraint
import scott.barleydb.api.persist.Operation
import scott.barleydb.api.persist.OperationType
import scott.financeserver.batchesOf
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.model.Category
import scott.financeserver.data.model.Transaction
import scott.financeserver.data.query.QCategory
import scott.financeserver.data.query.QTransaction
import scott.financeserver.toSequence
import scott.financeserver.upload.CategoryMatcher
import scott.financeserver.upload.CategoryResponse
import scott.financeserver.upload.toPersistRequest

@RestController
class CategoriesController {

    @Autowired
    private lateinit var env: Environment

    @GetMapping("/category")
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

    @PostMapping("/category")
    fun saveCategories(@RequestBody categories : List<scott.financeserver.upload.Category>) = DataEntityContext(env).use { ctx ->
        ctx.autocommit = false
        try {
            ctx.performQuery(QCategory().apply { where(name().equal("unknown")) }).singleResult.let { unknown ->
                ctx.streamObjectQuery(QTransaction())
                    .toSequence()
                    .map { Operation(it.also { it.category = unknown }, OperationType.UPDATE) }
                    .batchesOf(100)
                    .forEach { ops ->
                        ctx.persist(ops.toPersistRequest())
                    }
            }
            ctx.streamObjectQuery(QCategory().apply { where(name().notEqual("unknown")) })
                .toSequence()
                .map { Operation(it, OperationType.DELETE)}
                .batchesOf(100)
                .forEach { ops ->
                    ctx.persist(ops.toPersistRequest())
                }

            ctx.clear()
            categories.asSequence()
                .filter { c -> c.name != "unknown" }
                .map { c ->
                ctx.newModel(Category::class.java, c.id, EntityConstraint.dontFetch()).also { ec ->
                    ec.name = c.name
                    ec.matchers.apply { clear() }.addAll(c.matchers.map { m ->
                        ctx.getModelOrNewModel(scott.financeserver.data.model.CategoryMatcher::class.java, m.id).also { ecm ->
                            ecm.category = ec
                            ecm.pattern = m.pattern
                        }
                    })
                }
            }
            .map { c ->
                Operation(c, OperationType.INSERT)
            }
            .batchesOf(100)
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

    @PostMapping("/category/apply")
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

/*
    @PostMapping("/category/apply")
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