package scott.financeserver.accounts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import scott.barleydb.api.core.Environment
import scott.barleydb.api.core.entity.EntityConstraint
import scott.barleydb.api.persist.Operation
import scott.barleydb.api.persist.OperationType
import scott.barleydb.api.persist.PersistRequest
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.model.*
import scott.financeserver.data.model.Category
import scott.financeserver.data.model.Feed
import scott.financeserver.data.query.*
import scott.financeserver.import.parseBankAustria
import scott.financeserver.sequenceOfLists
import scott.financeserver.toDate
import scott.financeserver.toSequence
import scott.financeserver.toYearMonth
import scott.financeserver.upload.*
import java.math.BigDecimal
import java.util.*


data class FeedOverview(val feedId : UUID, val file : String, val dateImported : Long, val fromDate : Long, val toDate : Long, val numberOfTransactions : Int)
data class FeedOverviewResponse(val feeds: List<FeedOverview>)

@RestController
class FeedsController {

    @Autowired
    private lateinit var categoriesController: CategoriesController

    @Autowired
    private lateinit var endOfMonthStatementController: EndOfMonthStatementsController

    @Autowired
    private lateinit var env: Environment

    @GetMapping("/account/{accountName}/feed")
    fun getOverview(@PathVariable accountName: String) = DataEntityContext(env).use { ctx ->
        ctx.streamObjectQuery(QTransaction().apply {
            val feedJoin = joinToFeed()
            select(id(), feedId(), date(), feedJoin.file(), feedJoin.dateImported())
            whereExists(existsAccount().apply {
                where(name().equal(accountName))
            })
            orderBy(joinToFeed().dateImported(), true)
            orderBy(date(), true)
        }).toSequence()
            .fold(emptyList<FeedOverview>()) { r, t ->
                when {
                    r.isEmpty() || r.last().feedId != t.feed.id -> r + FeedOverview(
                        feedId = t.feed.id,
                        file = t.feed.file,
                        dateImported = t.feed.dateImported.time,
                        fromDate = t.date.time,
                        toDate = t.date.time,
                        numberOfTransactions = 1
                    )
                    else -> r.last().let { last ->
                        r.dropLast(1) + last.copy(
                            toDate = t.date.time,
                            numberOfTransactions = last.numberOfTransactions + 1
                        )
                    }
                }
            }.let { FeedOverviewResponse(it) }.also { Runtime.getRuntime().gc() }
    }

    @PostMapping("account/{accountName}/feed")
    fun uploadTransactions(
        @PathVariable accountName: String,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("currentBalance") currentBalance: BigDecimal?
    ): UploadResult {
        return DataEntityContext(env).use { ctx ->
            ctx.autocommit = false
            val categories = ctx.performQuery(QCategory()).list

            runCatching {
                val account = ctx.performQuery(QAccount().apply { name().equal(accountName) }).singleResult
                val feed = ctx.newModel(Feed::class.java).also {
                    it.contentHash = toHash(file.bytes)
                    it.account = account
                    it.dateImported = Date()
                    it.file = file.originalFilename
                    it.state = FeedState.IMPORTED
                }.also { ctx.persist(PersistRequest().insert(it)) }

                println("Processing bank austria upload...")
                val hashes = parseBankAustria(file.bytes) { seq ->
                    seq.map { row -> toHash(row.also { println(row.amount) }) }.toSet()
                }
                val matchingHashes = ctx.performQuery(QTransaction().apply {
                    select(id(), contentHash())
                    where(contentHash().`in`(hashes))
                }).list.map(Transaction::getContentHash).toSet()
                println("Found ${matchingHashes.size} matches")

                parseBankAustria(file.bytes) { seq ->
                    seq.mapIndexed { i, row ->
                        if (row.currency != "EUR") throw IllegalStateException("only EUR please")
                        println("$i, Processing row ${row.date} ${row.amount}")
                        Triple(toContent(row), toHash(row), row)
                    }.filter { (_, hash, _) -> matchingHashes.contains(hash).not() } //ignore if the content hash is already in the DB
                        .toList()
                        .map { (content, hash, row) ->
                            ctx.persist(PersistRequest().insert(ctx.newModel(Transaction::class.java).also { t ->
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
                                t.comment = null
                                t.duplicate = false
                                t.important = false
                            })).let { 1 }.also {
                                println("${row.lineNumber} inserting ${row.date} ${row.amount} $hash")
                            }
                        }.sum()
                }.let { count ->
                    if (count > 0) {
                        ctx.commit()
                        categoriesController.applyCategories()
                        applyDuplicateFlagOnFeedTransactions(feed.id)
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

    @DeleteMapping("/feed/{feedId}")
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

    @GetMapping("/duplicateCheck/{feedId}")
    fun duplicateCheck(@PathVariable feedId : UUID) : DuplicateCheckResult {
        return DataEntityContext(env).use { ctx ->
            ctx.performQuery(QFeed().apply {
                where(id().equal(feedId))
            }).singleResult.let { feed ->
                performDuplicateCheck(feed.account.id, feed.id)
            }
                .let { DuplicateCheckResult(it) }
        }
    }

    private fun performDuplicateCheck(accountId: UUID, feedId: UUID) : List<Duplicate> {
        return DataEntityContext(env).use { ctx ->
            ctx.performQuery(QFeed().apply { where(id().equal(feedId)) }).singleResult.let { feed ->
                ctx.performQuery(QDuplicates().apply {
                    feedHash().equal(feed.contentHash)
                }).list.let { existingDuplicates ->
                    ctx.streamObjectQuery(QTransaction().apply {
                        where(accountId().equal(accountId))
                        orderBy(date(), true)
                    }).toSequence()
                        .sequenceOfLists { t -> t.date.toLocalDate().dayOfMonth }
                        .flatMap { it.groupBy { t -> t.contentHash }.toList() }
                        .filter { (_, data) -> data.size > 1 }
                        .flatMap { (_, data) -> data }
                        .filter { it.feed.id == feedId }
                        .map {
                            existingDuplicates.find { ed -> ed.feedRecordNumber == it.feedRecordNumber && ed.contentHash == it.contentHash }?.let { match ->
                                Duplicate(
                                    id = match.id,
                                    recordNumber = match.feedRecordNumber,
                                    content = match.content,
                                    contentHash = match.contentHash,
                                    duplicate = match.duplicate)
                            }
                                ?: Duplicate(
                                    id = UUID.randomUUID(),
                                    recordNumber = it.feedRecordNumber,
                                    content = if (it.content.length > 120) it.content.substring(0, 120) else it.content,
                                    contentHash = it.contentHash,
                                    duplicate = null)
                        }
                        .toList()
                }
            }
        }
    }

    @PostMapping("duplicates/{feedId}")
    fun saveDuplicates(@PathVariable feedId : UUID, @RequestBody duplicates : Map<String, List<Duplicate>>) : DuplicateCheckResult {
        return DataEntityContext(env).use { ctx ->
            ctx.autocommit = false
            ctx.performQuery(QFeed().apply { where(id().equal(feedId)) }).singleResult.let { feed ->
                duplicates.values
                    .flatMap { dups ->
                        ctx.performQuery(QDuplicates().apply {
                            dups.forEach { d -> or(id().equal(d.id)) }
                        }).list.let { existingDups ->
                            dups.map { d ->
                                existingDups.find { d.id == it.id }?.let { existingD ->
                                    Operation(existingD, when (d.duplicate) {
                                        null -> OperationType.DELETE
                                        else -> OperationType.UPDATE

                                    })
                                } ?: Operation(ctx.newModel(Duplicates::class.java, d.id).apply {
                                    feedHash = feed.contentHash
                                    feedRecordNumber = d.recordNumber
                                    content = d.content
                                    contentHash = d.contentHash
                                    duplicate = d.duplicate
                                }, when (d.duplicate) {
                                    null -> OperationType.NONE
                                    else -> OperationType.INSERT
                                })
                            }
                        }
                    }
                    .filter { it.isNone.not() }
                    .toPersistRequest()
                    .let {
                        ctx.persist(it)
                        ctx.commit()
                        applyDuplicateFlagOnFeedTransactions(feedId)
                        endOfMonthStatementController.regenerateEndOfMonthStatements(feed.account.id)
                        performDuplicateCheck(feed.account.id, feed.id).let {
                            DuplicateCheckResult(it)
                        }
                    }
            }
        }
    }

    private fun applyDuplicateFlagOnFeedTransactions(feedId: UUID) {
        return DataEntityContext(env).use { it.run {
            autocommit = false
            performQuery((QFeed().apply { where(id().equal(feedId)) })).singleResult.let { feed ->
                performQuery(QDuplicates().apply {
                    where(feedHash().equal(feed.contentHash))
                }).list.also {
                    println("Found ${it.size} existing duplicates for Feed ${feedId} ")
                }.groupBy { d -> d.contentHash }.let { dups ->
                    performQuery(QTransaction().apply {
                        where(feedId().equal(feedId))
                    }).list.let { transactions ->
                        persist(PersistRequest().apply {
                            transactions.forEach { t -> save(t.also {
                                t.duplicate = dups[t.contentHash]?.find { d -> d.feedRecordNumber == t.feedRecordNumber && d.duplicate } != null
                            }) }
                        })
                    }
                }
            }
            commit()
        } }
    }

}

fun unknownCategory(categories : List<Category>): Category {
    return categories.filter { it.name == "unknown" }.first()
}
