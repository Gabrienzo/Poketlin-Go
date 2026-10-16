package ufrn.br.poketlin

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import ufrn.br.poketlin.application.AdicionarItemColecao
import ufrn.br.poketlin.application.AtualizarItemColecao
import ufrn.br.poketlin.application.BuscarCartas
import ufrn.br.poketlin.application.ListarItensColecao
import ufrn.br.poketlin.application.ObterItemColecao
import ufrn.br.poketlin.application.RemoverItemColecao
import ufrn.br.poketlin.domain.carta.CatalogoCartas
import ufrn.br.poketlin.domain.colecao.ItemColecaoRepository
import ufrn.br.poketlin.infrastructure.catalogo.CatalogoCartasGrpc
import ufrn.br.poketlin.infrastructure.catalogo.criarCanalWorker
import ufrn.br.poketlin.infrastructure.persistencia.ItemColecaoRepositoryJdbc
import ufrn.br.poketlin.infrastructure.persistencia.criarDataSource
import ufrn.br.poketlin.infrastructure.persistencia.migrarBanco
import ufrn.br.poketlin.presentation.erros.instalarTratamentoDeErros
import ufrn.br.poketlin.presentation.routes.cartasRoutes
import ufrn.br.poketlin.presentation.routes.colecaoRoutes
import ufrn.br.poketlin.presentation.routes.healthRoutes
import ufrn.br.poketlin.presentation.openapi.documentacaoRoutes
import ufrn.br.poketlin.presentation.openapi.instalarOpenApi

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val dataSource = criarDataSource()
    migrarBanco(dataSource)

    val canal = criarCanalWorker()
    environment.monitor.subscribe(ApplicationStopped) { canal.shutdown() }

    configurarApi(ItemColecaoRepositoryJdbc(dataSource), CatalogoCartasGrpc(canal))
}

fun Application.configurarApi(repositorio: ItemColecaoRepository, catalogo: CatalogoCartas) {
    install(ContentNegotiation) { json() }
    instalarTratamentoDeErros()
    instalarOpenApi()
    healthRoutes()
    routing {
        documentacaoRoutes()
        cartasRoutes(BuscarCartas(catalogo))
        colecaoRoutes(
            adicionar = AdicionarItemColecao(catalogo, repositorio),
            obter = ObterItemColecao(repositorio),
            listar = ListarItensColecao(repositorio),
            atualizar = AtualizarItemColecao(repositorio),
            remover = RemoverItemColecao(repositorio),
        )
    }
}