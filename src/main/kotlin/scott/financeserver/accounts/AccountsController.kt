package scott.financeserver.accounts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import scott.barleydb.api.core.Environment
import scott.barleydb.api.persist.PersistRequest
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.model.Category
import scott.financeserver.data.query.QAccount
import scott.financeserver.data.query.QCategory
import scott.financeserver.data.query.QTransaction
import scott.financeserver.upload.Account
import scott.financeserver.upload.AccountResponse
import javax.annotation.PostConstruct

@RestController
class AccountsController {
    @Autowired
    private lateinit var env: Environment

    @PostConstruct
    fun postConstruct() {
        DataEntityContext(env).let { ctx ->
            ctx.performQuery(QAccount().apply { name().equal("Bank Austria") }).list.firstOrNull()
                ?: ctx.newModel(scott.financeserver.data.model.Account::class.java).also {
                    it.name = "Bank Austria"
                    ctx.persist(PersistRequest().insert(it))
                }

            ctx.performQuery(QCategory().apply { name().equal("unknown") }).list.firstOrNull()
                ?: ctx.newModel(Category::class.java).also {
                    it.name = "unknown"
                    ctx.persist(PersistRequest().insert(it))
                }
        }
    }


    @GetMapping("/account")
    fun getAccounts() = DataEntityContext(env).use { ctx ->
        ctx.performQuery(QAccount().apply {
            orderBy(name(), true)
        }).list.map {
            Account(
                id = it.id,
                name = it.name,
                numberOfTransactions = ctx.performQuery(QTransaction().apply{
                    where(accountId().equal(it.id))
                    and(duplicate().equal(false))
                }).list.size.toLong()
            ) }
            .let { AccountResponse(it) }
    }.also { Runtime.getRuntime().gc() }

}