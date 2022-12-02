package scott.financeserver.accounts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import scott.barleydb.api.core.Environment
import scott.barleydb.api.core.entity.EntityConstraint
import scott.barleydb.api.core.entity.EntityContext
import scott.barleydb.api.persist.PersistRequest
import scott.financeserver.*
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.model.FeedState
import scott.financeserver.data.model.Category   as ECategory
import scott.financeserver.data.model.Feed  as EFeed
import scott.financeserver.data.model.Transaction as ETransaction
import scott.financeserver.data.query.*
import scott.financeserver.import.parseBankAustria
import scott.financeserver.toDate
import java.math.BigDecimal
import java.util.*


data class FeedOverview(val feedId : UUID, val file : String, val dateImported : Long, val fromDate : Long?, val toDate : Long?, val numberOfTransactions : Int)
data class FeedOverviewResponse(val feeds: List<FeedOverview>)

@RestController
class FeedsController {

    @Autowired
    private lateinit var categoriesController: CategoriesController

    @Autowired
    private lateinit var endOfMonthStatementController: EndOfMonthStatementsController

    @Autowired
    private lateinit var env: Environment

    @GetMapping("/api/account/{accountName}/feed")
    fun getOverview(@PathVariable accountName: String) : FeedOverviewResponse = DataEntityContext(env).use { ctx ->
        ctx.performQuery(QFeed()).list.forEach { println(it.file + "  " + it.account.name) }
        ctx.streamObjectQuery(QFeed().apply {
            select(id(), file(), dateImported())
            whereExists(existsAccount().apply {
                where(name().equal(accountName))
            })
            orderBy(dateImported(), true)
        }).toSequence()
            .map { f ->
                val transactionsOverview = getTransactionsOverview(ctx, f.id)
                FeedOverview(
                    feedId = f.id,
                    file = f.file,
                    dateImported = f.dateImported.time,
                    fromDate = transactionsOverview.first,
                    toDate = transactionsOverview.last,
                    numberOfTransactions = transactionsOverview.count
                )
            }.toList().let {
                FeedOverviewResponse(it)
            }
    }

    data class TransactionsOverview(val first: Long?, val last: Long?, val count: Int)

    private fun getTransactionsOverview(ctx: EntityContext, feedId: UUID) : TransactionsOverview {
        return ctx.performQuery(QTransaction().apply {
            select(id(), date())
            where(feedId().equal(feedId))
            orderBy(date(), true)
        }).list.let {
            TransactionsOverview(it.firstOrNull()?.date?.time, it.lastOrNull()?.date?.time, it.size)
        }
    }

    @PostMapping("/api/account/{accountName}/feed")
    fun uploadTransactions(
        @PathVariable accountName: String,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("timestamp") timestamp: Long,
        @RequestParam("currentBalance") currentBalance: BigDecimal?
    ): UploadResult {
        println("Uploading new feed ${file.originalFilename}")
        return DataEntityContext(env).use { ctx ->
            ctx.autocommit = false
            val categories = ctx.performQuery(QCategory()).list

            runCatching {
                val account = ctx.performQuery(QAccount().apply { name().equal(accountName) }).singleResult
                val feed = ctx.newModel(EFeed::class.java).also {
                    it.contentHash = toHash(file.bytes)
                    it.account = account
                    it.dateImported = Date(timestamp)
                    it.file = file.originalFilename
                    it.state = FeedState.IMPORTED
                }.also { ctx.persist(PersistRequest().insert(it)) }

                val hashes = parseBankAustria(file.bytes) { seq ->
                    seq.map { row -> toHash(row) }.toSet()
                }
                val matchingHashes = ctx.performQuery(QTransaction().apply {
                    select(id(), contentHash())
                    where(contentHash().`in`(hashes))
                }).list.map(ETransaction::getContentHash).toSet()

                parseBankAustria(file.bytes) { seq ->
                    seq.mapIndexed { i, row ->
                        if (row.currency != "EUR") throw IllegalStateException("only EUR please")
//                        println("$i, Processing row ${row.date} ${row.amount}")
                        Triple(toContent(row), toHash(row), row)
                    }.filter { (_, hash, _) -> matchingHashes.contains(hash).not() } //ignore if the content hash is already in the DB
                        .toList()
                        .map { (content, hash, row) ->
                            ctx.persist(PersistRequest().insert(ctx.newModel(ETransaction::class.java).also { t ->
                                t.account = account
                                t.feed = feed
                                t.feedRecordNumber = row.lineNumber
                                t.content = content
                                t.contentHash = hash
                                t.description = "${row.bookingText} ${row.reason} ${row.senderBic}"
                                t.date = row.date
                                t.category = unknownCategory(categories)
                                t.userCategorized = false
                                t.amount = row.amount
                                t.duplicate = false
                            })).let { 1 }.also {
//                                println("${row.lineNumber} inserting ${row.date} ${row.amount} $hash")
                            }
                        }.sum()
                }.let { count ->
                    if (count > 0) {
                        ctx.commit()
                        categoriesController.applyCategories()
                        endOfMonthStatementController.regenerateEndOfMonthStatements(account.id)
                        UploadResult(feedId = feed.id, count = count)
                    } else {
                        ctx.rollback()
                        UploadResult(error = "no records")
                    }
                }
            }
                .getOrElse { x ->
                    x.printStackTrace()
                    runCatching { ctx.rollback() }.onFailure { x2 -> println(x2) }
                    UploadResult(error = x.stackTraceToString())
                }
        }
    }

    @DeleteMapping("/api/feed/{feedId}")
    fun deleteFeed(@PathVariable feedId: UUID) = DataEntityContext(env).use { ctx ->
        ctx.autocommit = false
        val feed = ctx.performQuery(QFeed().apply { where(id().equal(feedId)) }).singleResult
        if (ctx.performQuery(QFeed().apply {
                where(accountId().equal(feed.account.id))
                and(dateImported().greater(feed.dateImported))
            }).list.isNotEmpty()) {
            throw java.lang.IllegalStateException("Can only delete newest feed")
        }
        try {
            ctx.streamObjectQuery(QTransaction().apply {
                select(id(), date())
                where(feedId().equal(feedId))
                orderBy(date(), false)
            })
                .toSequence()
                .map { it.apply { entity.constraints.set(EntityConstraint.mustExistAndDontFetch()) } }
                .map {
                    it.also {
                        //     println("Deleting ${it.date}")
                        ctx.persist(PersistRequest().delete(it))
                    }
                }
                .map { it.date.toYearMonth() }
                .distinct()
                .toList()
                .last().let { yearAndMonth ->
                    println("Last year and month $yearAndMonth")
                    ctx.performQuery(QEndOfMonthStatement().apply {
                        where(accountId().equal(feed.account.id))
                        and(date().greaterOrEqual(yearAndMonth.atDay(1).atStartOfDay().toDate()))
                    }).list.let { stmts ->
                        if (stmts.isNotEmpty()) ctx.persist(PersistRequest().apply {
                            stmts.forEach { stmt -> delete(stmt) }
                        })
                    }
                }
            ctx.persist(PersistRequest().delete(feed))
            ctx.commit()
        } catch (x: Exception) {
            x.printStackTrace()
            runCatching { ctx.rollback() }.onFailure { x -> x.printStackTrace() }
        }
    }.also { Runtime.getRuntime().gc() }


    @GetMapping("/api/feed/{feedId}")
    fun getFeedTransactions(@PathVariable feedId: UUID) : TransactionsResponse {
        return DataEntityContext(env).use { ctx ->
            ctx.performQuery(QTransaction().apply {
                where(feedId().equal(feedId))
                orderBy(date(), true)
                orderBy(amount(), true)
            }).list.map {
                it.forClient()
            }.let {
                TransactionsResponse(it)
            }
        }
    }

}


fun unknownCategory(categories : List<ECategory>): ECategory {
    return categories.filter { it.name == "unknown" }.first()
}
