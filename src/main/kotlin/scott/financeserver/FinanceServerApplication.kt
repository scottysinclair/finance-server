package scott.financeserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class FinanceServerApplication

fun main(args: Array<String>) {
	runApplication<FinanceServerApplication>(*args)
}
