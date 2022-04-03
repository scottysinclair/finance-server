package scott.financeserver.upload

import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.multipart.MultipartFile
import scott.barleydb.api.core.Environment
import scott.barleydb.api.core.entity.EntityConstraint
import scott.barleydb.api.core.entity.EntityConstraint.mustExistInDatabase
import scott.barleydb.api.core.entity.EntityConstraint.mustNotExistInDatabase
import scott.barleydb.api.persist.Operation
import scott.barleydb.api.persist.OperationType
import scott.barleydb.api.persist.PersistRequest
import scott.financeserver.*
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.model.Duplicates
import scott.financeserver.data.model.FeedState

import scott.financeserver.data.query.*
import scott.financeserver.import.BankAustriaRow
import scott.financeserver.import.parseBankAustria
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.annotation.PostConstruct
import scott.financeserver.data.model.Account as EAccount
import scott.financeserver.data.model.Feed as EFeed
import scott.financeserver.data.model.Category as ECategory
import scott.financeserver.data.model.CategoryMatcher as ECategoryMatcher
import scott.financeserver.data.model.Transaction as ETransaction
import scott.financeserver.data.model.EndOfMonthStatement as EEndOfMonthStatement
import scott.financeserver.data.model.BalanceAt as EBalanceAt

data class Account(val id: UUID, val name: String, val numberOfTransactions: Long)
data class AccountResponse(val accounts: List<Account>)

data class Feed(val id: UUID, val file: String, val dateImported: Long, val state: String)
data class FeedResponse(val feeds: List<Feed>)

data class Statement(val id: UUID, val accountId: String, val date: Long, val amount: BigDecimal)
data class StatementResponse(val statements: List<Statement>)


data class Category(val id: UUID, val name: String, val matchers : List<CategoryMatcher>)
data class CategoryMatcher(val id: UUID, val pattern: String)
data class CategoryResponse(val categories: List<Category>)

data class UploadResult(val feedId : UUID? = null, val count: Int? = null, val error: String? = null)
data class Duplicate(val id : UUID, val recordNumber : Int, val contentHash : String, val content : String, val duplicate : Boolean?)
data class DuplicateCheckResult(val duplicates: List<Duplicate>)

data class BalancecAt(val id : UUID, val account : UUID, val time : Long, val amount : BigDecimal)
data class BalancecAtResponse(val balanceAts : List<BalancecAt>)
data class PutBalanceAt(val account : UUID, val time : Long, val amount : BigDecimal)

//@RestController
class UploadController {

    @Autowired
    private lateinit var env: Environment

    @PostConstruct
    fun postConstruct() {
        DataEntityContext(env).let { ctx ->
            ctx.performQuery(QAccount().apply { name().equal("Bank Austria") }).list.firstOrNull()
                ?: ctx.newModel(EAccount::class.java).also {
                    it.name = "Bank Austria"
                    ctx.persist(PersistRequest().insert(it))
                }

            ctx.performQuery(QCategory().apply { name().equal("unknown") }).list.firstOrNull()
                ?: ctx.newModel(ECategory::class.java).also {
                    it.name = "unknown"
                    ctx.persist(PersistRequest().insert(it))
                }
        }
    }


    @GetMapping("/statement")
    fun getStatements() = DataEntityContext(env).use { ctx ->
        ctx.performQuery(QEndOfMonthStatement().apply {
            orderBy(date(), true)
        }).list.map {
            Statement(
                id = it.id,
                accountId = it.account.id.toString(),
                date = it.date.time,
                amount = it.amount
            )}
            .let { StatementResponse(it) }
    }.also { Runtime.getRuntime().gc() }

    @GetMapping("/category")
    fun getCategories() = DataEntityContext(env).use { ctx ->
        ctx.performQuery(QCategory().apply {
            joinToMatchers()
            orderBy(name(), true)
        }).list.map {
            Category(
                id = it.id,
                name = it.name,
                matchers = it.matchers.map { m -> CategoryMatcher(
                    id = m.id,
                    pattern = m.pattern) }
            )}
            .let { CategoryResponse(it) }
    }.also { Runtime.getRuntime().gc() }

    @PostMapping("/category")
    fun saveCategories(@RequestBody categories : List<Category>) = DataEntityContext(env).use { ctx ->
        ctx.autocommit = false
        try {
            ctx.persist(ctx.performQuery(QCategory().apply {
                where(name().notEqual("unknown"))
                and(id().notIn(categories.map(Category::id).toSet()))
            }).list.map { c ->
                Operation(c, OperationType.DELETE)
            }.toPersistRequest())

            ctx.persist(categories.map { c ->
                ctx.newModel(ECategory::class.java, c.id, EntityConstraint.dontFetch()).also { ec ->
                    ec.name = c.name
                    ec.matchers.apply { clear() }.addAll(c.matchers.map { m ->
                        ctx.getModelOrNewModel(ECategoryMatcher::class.java, m.id).also { ecm ->
                            ecm.category = ec
                            ecm.pattern = m.pattern
                        }
                    })
                }
            }
                .map { c ->
                    Operation(c, OperationType.SAVE)
                }.toPersistRequest())
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
    private fun anaylseCategory(transaction: ETransaction, categories : List<ECategory>, unknownCategory : ECategory)  {
        transaction.category = categories.find { c ->
            c.matchers.map { m -> cachedRegex.getOrPut(m.pattern, { Regex(m.pattern, RegexOption.IGNORE_CASE) }) }
                    .any { regex -> regex.containsMatchIn(transaction.description) }
        } ?: unknownCategory
    }


    @GetMapping("/balanceat/{accountName}")
    fun getBalanceAts(@PathVariable accountName : String) = DataEntityContext(env).use { ctx ->
        ctx.performQuery(QAccount().apply {
            where(name().equal(accountName))
        }).list.map { account ->
            ctx.performQuery(QBalanceAt().apply {
                where(accountId().equal(account.id))
                orderBy(time(), true)
            }).list.map { balanceAt ->
                BalancecAt(
                    id = balanceAt.id,
                    account = balanceAt.account.id,
                    amount = balanceAt.amount,
                    time = balanceAt.time.time)
            }
            .let { BalancecAtResponse(it) }
        }
    }.also { Runtime.getRuntime().gc() }

    @PostMapping("/balanceAt/{id}")
    fun addBalanceAt(@PathVariable id : UUID, @RequestBody data : PutBalanceAt) = DataEntityContext(env).use { ctx ->
        ctx.persist(PersistRequest().save(ctx.newModel(EBalanceAt::class.java, id).apply {
            account = ctx.newModel(EAccount::class.java, data.account, mustExistInDatabase())
            time = Date(data.time)
            amount = data.amount
        }))
    }.also { Runtime.getRuntime().gc() }


        @GetMapping("/feed")
    fun getFeeds() = DataEntityContext(env).use { ctx ->
        ctx.performQuery(QFeed().apply {
            orderBy(dateImported(), true)
        }).list.map {
            Feed(
                id = it.id,
                file = it.file,
                dateImported = it.dateImported.time,
                state = it.state.name) }
            .let { FeedResponse(it) }
    }.also { Runtime.getRuntime().gc() }

    @PutMapping("/account/{accountName}")
    fun addAccount(@PathVariable accountName : String) = DataEntityContext(env).use { ctx ->
        ctx.persist(PersistRequest().insert(ctx.newModel(EAccount::class.java).apply {
            name = accountName
        }))
    }.also { Runtime.getRuntime().gc() }

    @DeleteMapping("/account/{accountId}")
    fun deleteAccount(@PathVariable accountId : UUID) = DataEntityContext(env).use { ctx ->
        if (ctx.performQuery(QTransaction().apply {
            select(id())
            where(accountId().equal(accountId))
        }).list.isNotEmpty()) throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "account has transactions...")

        ctx.persist(PersistRequest().delete(ctx.newModel(EAccount::class.java, accountId, EntityConstraint.mustExistAndDontFetch())))
    }.also { Runtime.getRuntime().gc() }


    @PostMapping("upload/{accountName}")
    fun uploadTransactions(@PathVariable accountName : String, @RequestParam("file") file : MultipartFile, @RequestParam("currentBalance") currentBalance : BigDecimal?) : UploadResult {
        return DataEntityContext(env).use { ctx ->
            ctx.autocommit = false
            val categories = ctx.performQuery(QCategory()).list

            runCatching {
                val account = ctx.performQuery(QAccount().apply { name().equal(accountName) }).singleResult
                val feed = ctx.newModel(EFeed::class.java).also {
                    it.contentHash = toHash(file.bytes)
                    it.account = account
                    it.dateImported = Date()
                    it.file = file.originalFilename
                    it.state = FeedState.IMPORTED
                }.also { ctx.persist(PersistRequest().insert(it)) }

                println("Processing bank austria upload...")
                val hashes = parseBankAustria(file.bytes) { seq ->
                    seq.map { row -> toHash(row.also { println(row.amount)}) }.toSet()
                }
                val matchingHashes = ctx.performQuery(QTransaction().apply {
                        select(id(), contentHash())
                        where(contentHash().`in`(hashes))
                    }).list.map(ETransaction::getContentHash).toSet()
                println("Found ${matchingHashes.size} matches")

                parseBankAustria(file.bytes) { seq ->
                    seq.mapIndexed {i, row ->
                        if (row.currency != "EUR") throw IllegalStateException("only EUR please")
                        println("$i, Processing row ${row.date} ${row.amount}")
                        Triple(toContent(row), toHash(row), row)
                    }.filter { (_, hash, _) -> matchingHashes.contains(hash).not()} //ignore if the content hash is already in the DB
                    .toList()
                    .map {(content, hash, row) ->
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
                            t.duplicate = false})).let { 1 }.also {
                            println("${row.lineNumber} inserting ${row.date} ${row.amount} $hash")
                        }
                        }.sum()
                }.let { count ->
                    if (count > 0) {
                        ctx.commit()
                        applyCategories()
                        regenerateEndOfMonthStatements(account)
                        UploadResult(feedId = feed.id, count = count)
                    }
                    else {
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

    private fun regenerateEndOfMonthStatements(account: EAccount) {
        DataEntityContext(env).use { ctx ->
            ctx.autocommit = false
            runCatching {
                ctx.performQuery(QEndOfMonthStatement().apply { accountId().equal(account.id) }).list
                    .map { stmt -> Operation(stmt, OperationType.DELETE) }
                    .toPersistRequest()
                    .let { ctx.persist(it) }

                ctx.performQuery(QBalanceAt().apply { orderBy(time(), true) }).list.let { balanceAts ->
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
                                balance -= it.map(ETransaction::getAmount)
                                    .reduce { a, b -> a + b }
                                it.first().date.toEndOfLastMonth() to balance
                            }
                            .toList().let { list ->
                                list.subList(0, list.size - 1).forEach { (endOfMonth, balance) ->
                                    ctx.persist(PersistRequest().insert(ctx.newModel(EEndOfMonthStatement::class.java).also {
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
                                it.first().date.toEndOfMonth() to (balanceAts.firstOrNull()?.let { nextBalanceAt ->
                                    var amountBeforeFixed = it.filter { t -> t.date < nextBalanceAt.time }
                                        .map(ETransaction::getAmount).reduceOrNull { a, b -> a + b } ?: 0.toBigDecimal()

                                    var amountAfterFixed = it.filter { t -> t.date >= nextBalanceAt.time }
                                        .map(ETransaction::getAmount).reduceOrNull { a, b -> a + b } ?: 0.toBigDecimal()
                                    when {
                                        amountAfterFixed > 0.toBigDecimal() -> nextBalanceAt.amount + amountAfterFixed.also { balanceAts.removeFirst() }
                                        else -> amountBeforeFixed + amountAfterFixed
                                    }
                                } ?: it.map(ETransaction::getAmount).reduce { a, b -> a + b })
                            }
                            .toList().let { list ->
                                list.subList(0, list.size - 1).forEach { (endOfMonth, balance) ->
                                    ctx.persist(PersistRequest().save(ctx.newModel(EEndOfMonthStatement::class.java).also {
                                        it.account = account
                                        it.date = endOfMonth
                                        it.amount = balance
                                    }))
                                }
                            }
                    }
                }
            }.onFailure { ctx.rollback() }
            .onSuccess { ctx.commit() }
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
                                    duplicate = true)
                            }
                            ?: Duplicate(
                                id = UUID.randomUUID(),
                                recordNumber = it.feedRecordNumber,
                                content = if (it.content.length > 120) it.content.substring(0, 120) else it.content,
                                contentHash = it.contentHash,
                                duplicate = false)
                        }
                        .toList()
                }
            }
        }
    }

    @GetMapping("duplicateCheck/{feedId}")
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
                         regenerateEndOfMonthStatements(feed.account)
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
                }).list.groupBy { d -> d.contentHash }.let { dups ->
                    performQuery(QTransaction().apply {
                        where(feedId().equal(feedId))
                    }).list.let { transactions ->
                        persist(PersistRequest().apply {
                            transactions.forEach { t -> save(t.also {
                                t.duplicate = dups[t.contentHash]?.find { d -> d.feedRecordNumber == t.feedRecordNumber } != null
                            }) }
                        })
                    }
                }
            }
            commit()
        } }
    }


    private fun unknownCategory(categories : List<ECategory>): ECategory {
        return categories.filter { it.name == "unknown" }.first()
    }
}

fun List<Operation>.toPersistRequest() = PersistRequest().also { pr ->forEach{ op -> pr.add(op) } }

fun Date.toLocalDate() = toInstant().atZone(ZoneId.of("Europe/Vienna")).toLocalDate()

fun LocalDateTime.toDate() = Date.from(atZone(ZoneId.of("Europe/Vienna")).toInstant())

fun toContent(row : BankAustriaRow) =
    row.run {listOf(date, currency, amount.toString(), bookingText, reason, senderBic) }
    .joinToString(" | ")

fun toHash(row : BankAustriaRow) = DigestUtils.sha1Hex(toContent(row))

fun toHash(data : ByteArray) = DigestUtils.sha1Hex(data)
