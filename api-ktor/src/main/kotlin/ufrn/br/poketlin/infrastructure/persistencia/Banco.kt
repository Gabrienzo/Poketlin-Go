// infrastructure/persistencia/Banco.kt
package ufrn.br.poketlin.infrastructure.persistencia

import org.flywaydb.core.Flyway
import javax.sql.DataSource
import org.postgresql.ds.PGSimpleDataSource

fun criarDataSource(): DataSource {
    val ds = PGSimpleDataSource()
    ds.setURL(System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/poketlin")
    ds.user = System.getenv("DATABASE_USER") ?: "poketlin"
    ds.password = System.getenv("DATABASE_PASSWORD") ?: "poketlin"
    return ds
}

fun migrarBanco(dataSource: DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .load()
        .migrate()
}