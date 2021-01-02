package scott.financeserver.accounts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import scott.barleydb.api.core.Environment
import scott.barleydb.api.persist.PersistRequest
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.model.BalanceAt
import scott.financeserver.data.query.QAccount
import scott.financeserver.data.query.QBalanceAt
import scott.financeserver.toDate
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

data class FixedBalance(val id : UUID, val time : Long, val amount : BigDecimal)
data class FixedBalanceResponse(val balances : List<FixedBalance>)
data class FixedBalancePost(val id : UUID, val time : String, val amount : BigDecimal)

@RestController
class FixedBalancesController {
    @Autowired
    private lateinit var env: Environment

    @GetMapping("/account/{accountName}/fixedbalance")
    fun getFixedBalances(@PathVariable accountName : String) = DataEntityContext(env).use { ctx ->
          ctx.performQuery(QBalanceAt().apply {
              whereExists(existsAccount().apply { where(name().equal(accountName)) })
          }).list.map {
              FixedBalance(
                  id = it.id,
                  time = it.time.time,
                  amount = it.amount
              )
          }.let { FixedBalanceResponse(it) }.also { Runtime.getRuntime().gc() }
    }

    @PostMapping("/account/{accountName}/fixedbalance")
    fun saveFixedBalances(@PathVariable accountName : String, @RequestBody fixedBalance : FixedBalancePost) = DataEntityContext(env).use { ctx ->
        ctx.performQuery(QAccount().apply {
            where(name().equal(accountName))
        }).singleResult.let { account ->
            ctx.persist(PersistRequest().apply {
                save(ctx.newModel(BalanceAt::class.java, fixedBalance.id).also {
                    it.account = account
                    it.time = parseLocalDateTime(fixedBalance.time)
                    it.amount = fixedBalance.amount
                })
            })
        }.let { getFixedBalances(accountName) }.also { Runtime.getRuntime().gc() }
    }

    private fun parseLocalDateTime(time : String): Date {
        return LocalDateTime.from(DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(time)).toDate()
    }
}

