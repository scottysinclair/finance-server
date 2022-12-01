package scott.financeserver.accounts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import scott.barleydb.api.core.Environment
import scott.barleydb.api.persist.Operation
import scott.barleydb.api.persist.OperationType
import scott.barleydb.api.persist.PersistRequest
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.model.Account
import scott.financeserver.data.model.EndOfMonthStatement
import scott.financeserver.data.model.Transaction
import scott.financeserver.data.query.QAccount
import scott.financeserver.data.query.QBalanceAt
import scott.financeserver.data.query.QEndOfMonthStatement
import scott.financeserver.data.query.QTransaction
import scott.financeserver.sequenceOfMonthlyTransactions
import scott.financeserver.toEndOfLastMonth
import scott.financeserver.toEndOfMonth
import scott.financeserver.toSequence
import java.util.*

@RestController
class EndOfMonthStatementsController {

    @Autowired
    private lateinit var env: Environment

    fun regenerateEndOfMonthStatements(accountId: UUID) {
        DataEntityContext(env).use { ctx ->
            ctx.autocommit = false
            runCatching {
                ctx.performQuery(QAccount().apply { where(id().equal(accountId)) }).singleResult.let { account  ->
                    ctx.performQuery(QEndOfMonthStatement().apply { accountId().equal(account.id) }).list
                        .map { stmt -> Operation(stmt, OperationType.DELETE) }
                        .toPersistRequest()
                        .let { ctx.persist(it) }

                    ctx.performQuery(QBalanceAt().apply { orderBy(time(), true) }).list.toMutableList().let { balanceAts ->
                        balanceAts.firstOrNull()?.let { oldestBalanceAt ->
                            //find oldest BalanceAt and work our way backwards, generating end month statements
                            var balance = oldestBalanceAt.amount
                            ctx.streamObjectQuery(QTransaction().apply {
                                where(duplicate().equal(false))
                                and(accountId().equal(account.id))
                                and(date().less(oldestBalanceAt.time))
                                orderBy(date(), false)
                            })
                                .toSequence()
                                .sequenceOfMonthlyTransactions()
                                .map {
                                    balance -= it.map(Transaction::getAmount)
                                        .reduce { a, b -> a + b }
                                    (it.first().date.toEndOfLastMonth() to balance).also {
                                        println("Month ${it.first}  has  stmt ${it.second}")
                                    }
                                }
                                .toList().let { list ->
                                    if (list.isNotEmpty())
                                        list.subList(0, list.size - 1).forEach { (endOfMonth, balance) ->
                                            ctx.persist(PersistRequest().insert(ctx.newModel(EndOfMonthStatement::class.java).also {
                                                it.account = account
                                                it.date = endOfMonth
                                                it.amount = balance
                                            }))
                                    }
                                }
                            //then work forward, adapting to new BalanceAt corrections as we come across them
                            balance = oldestBalanceAt.amount
                            balanceAts.removeFirstOrNull() //treat balanceAts as a queue we consume from as we process through the stream
                            ctx.streamObjectQuery(QTransaction().apply {
                                where(duplicate().equal(false))
                                and(accountId().equal(account.id))
                                and(date().greaterOrEqual(oldestBalanceAt.time))
                                orderBy(date(), true)
                            })
                                .toSequence()
                                .sequenceOfMonthlyTransactions()
                                .map {
                                    balance += (balanceAts.firstOrNull()?.let { nextBalanceAt ->
                                        var amountBeforeFixed = it.filter { t -> t.date < nextBalanceAt.time }
                                            .map(Transaction::getAmount).reduceOrNull { a, b -> a + b } ?: 0.toBigDecimal()

                                        var amountAfterFixed = it.filter { t -> t.date >= nextBalanceAt.time }
                                            .map(Transaction::getAmount).reduceOrNull { a, b -> a + b } ?: 0.toBigDecimal()
                                        when {
                                            amountAfterFixed > 0.toBigDecimal() -> nextBalanceAt.amount + amountAfterFixed.also { balanceAts.removeFirst() }
                                            else -> amountBeforeFixed + amountAfterFixed
                                        }
                                    } ?: it.map(Transaction::getAmount).reduce { a, b -> a + b })
                                    (it.first().date.toEndOfMonth() to balance).also {
                                        println("Month ${it.first}  has  stmt ${it.second}")
                                    }
                                }
                                .toList().let { list ->
                                    if (list.isNotEmpty())
                                        list.subList(0, list.size - 1).forEach { (endOfMonth, balance) ->
                                            ctx.persist(PersistRequest().save(ctx.newModel(EndOfMonthStatement::class.java).also {
                                                it.account = account
                                                it.date = endOfMonth
                                                it.amount = balance
                                            }))
                                        }
                                }
                        }
                    }
                }
            }.onFailure { ctx.rollback() }
            .onSuccess { ctx.commit() }
            .getOrThrow()
        }
    }

}