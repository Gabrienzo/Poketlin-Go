package ufrn.br.poketlin

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import ufrn.br.poketlin.presentation.routes.healthRoutes
import ufrn.br.poketlin.infrastructure.persistencia.criarDataSource
import ufrn.br.poketlin.infrastructure.persistencia.migrarBanco

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val dataSource = criarDataSource()
    migrarBanco(dataSource)
    install(ContentNegotiation) {
        json()
    }
    healthRoutes()
}