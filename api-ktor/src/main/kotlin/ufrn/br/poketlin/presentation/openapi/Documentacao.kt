package ufrn.br.poketlin.presentation.openapi

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.routing.openApiSpec
import io.github.smiley4.ktorswaggerui.routing.swaggerUI
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Application.instalarOpenApi() {
    install(SwaggerUI) {
        info {
            title = "Poketlin API"
            version = "1.0.0"
            description = "Fichário de cartas Pokémon TCG: busca de cartas e gerenciamento da coleção."
        }
    }
}

fun Route.documentacaoRoutes() {
    route("api.json") { openApiSpec() }
    route("swagger") { swaggerUI("/api.json") }
}