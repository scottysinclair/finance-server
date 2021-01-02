package scott.financeserver.accounts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import scott.barleydb.api.core.Environment
import scott.barleydb.api.persist.Operation
import scott.barleydb.api.persist.OperationType
import scott.financeserver.batchesOf
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.model.Category
import scott.financeserver.data.model.Transaction
import scott.financeserver.data.query.QCategory
import scott.financeserver.data.query.QTransaction
import scott.financeserver.toSequence
import scott.financeserver.upload.toPersistRequest

@RestController
class CategoriesController {

    @Autowired
    private lateinit var env: Environment

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


}