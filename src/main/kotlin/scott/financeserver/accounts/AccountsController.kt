package scott.financeserver.accounts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import scott.barleydb.api.core.Environment
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.query.QAccount
import scott.financeserver.data.query.QTransaction
import scott.financeserver.upload.Account
import scott.financeserver.upload.AccountResponse

@RestController
class AccountsController {
    @Autowired
    private lateinit var env: Environment

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