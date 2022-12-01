package scott.financeserver.accounts

import java.math.BigDecimal
import java.util.*

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

data class Transaction(val id : UUID, val account : String, val description : String, val day : Int, val month : Int, val year: Int, val feed : UUID, val category : String, val amount : BigDecimal, val duplicate: Boolean)
data class  TransactionsResponse(val transactions: List<Transaction>)
