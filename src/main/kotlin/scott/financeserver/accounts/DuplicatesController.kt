package scott.financeserver.accounts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import scott.barleydb.api.core.Environment
import scott.barleydb.api.persist.PersistRequest
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.query.QTransaction
import scott.financeserver.forClient
import java.util.*

data class Duplicate2(val id: UUID, val duplicate: Boolean)

@RestController
class DuplicatesController {

   @Autowired
   private lateinit var env: Environment

   @Autowired
   private lateinit var endOfMonthStatementController: EndOfMonthStatementsController


   @GetMapping("/api/duplicateCheck2")
   fun duplicateCheck(): TransactionsResponse {
      return performDuplicateCheck()
            .let { TransactionsResponse(it) }
   }

   private fun performDuplicateCheck() : List<Transaction> {
      return DataEntityContext(env).use { ctx ->
         ctx.performQuery(QTransaction().let { qt ->
            qt.whereExists(QTransaction().let { other ->
               other.where(other.id().notEqualProp(qt.id()))
                  .and(other.date().equalProp(qt.date()))
                  .and(other.amount().equalProp(qt.amount()))
            })
            .orderBy(qt.date(), true)
            .orderBy(qt.amount(), true)
         }).list.let { trans ->
            println("Found ${trans.size} duplicates")
//            trans.forEach { println("trans " + it.date + " " + it.amount) }
            trans.map { it.forClient() }
         }
      }
   }

   @PostMapping("/api/duplicates2")
   fun saveDuplicates(@RequestBody duplicates : Map<String, List<Duplicate2>>) {
       DataEntityContext(env).use { ctx ->
          ctx.autocommit = false
          ctx.use {
             ctx.performQuery(QTransaction().apply {
                duplicates.values.firstOrNull()?.forEach {
                   or(id().equal(it.id))
                }
             }).list.let { transactions ->
                val persistRequest = PersistRequest()
                duplicates.values.first().forEach { inputT ->
                   transactions.find { t -> t.id == inputT.id }?.run {
                      duplicate = inputT.duplicate
                      persistRequest.save(this)
                   }
                }
                ctx.persist(persistRequest)
                ctx.commit()
               transactions.groupBy { it.account.id }.keys.forEach { accountId ->
                  endOfMonthStatementController.regenerateEndOfMonthStatements(accountId)
               }
             }
          }
       }
   }

}
