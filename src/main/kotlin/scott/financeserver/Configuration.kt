package scott.financeserver

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import scott.barleydb.bootstrap.EnvironmentDef
import scott.barleydb.server.jdbc.persist.QuickHackSequenceGenerator

@Configuration
class Configuration {

    @Bean
    fun environmentDef() = EnvironmentDef()
            .withSpecs(AccountingSpec::class.java)
            .withDataSource()
                .withUrl("jdbc:hsqldb:file:db/hsqldb")
                .withDriver("org.hsqldb.jdbcDriver")
                .withUser("sa")
                .withPassword("")
                .end()
            .withDroppingSchema(false)
            .withSchemaCreation(true)
            .withSequenceGenerator(QuickHackSequenceGenerator::class.java)
            .create()

}