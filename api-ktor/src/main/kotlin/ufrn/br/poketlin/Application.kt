package ufrn.br.poketlin

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import ufrn.br.poketlin.application.AdicionarItemColecao
import ufrn.br.poketlin.application.AtualizarItemColecao
import ufrn.br.poketlin.application.ListarItensColecao
import ufrn.br.poketlin.application.ObterItemColecao
import ufrn.br.poketlin.application.RemoverItemColecao
import ufrn.br.poketlin.domain.carta.CatalogoCartas
import ufrn.br.poketlin.domain.colecao.ItemColecaoRepository
import ufrn.br.poketlin.infrastructure.CatalogoCartasProvisorio
import ufrn.br.poketlin.infrastructure.persistencia.ItemColecaoRepositoryJdbc
import ufrn.br.poketlin.infrastructure.persistencia.criarDataSource
import ufrn.br.poketlin.infrastructure.persistencia.migrarBanco
import ufrn.br.poketlin.presentation.erros.instalarTratamentoDeErros
import ufrn.br.poketlin.presentation.routes.colecaoRoutes
import ufrn.br.poketlin.presentation.routes.healthRoutes

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val dataSource = criarDataSource()
    migrarBanco(dataSource)
    configurarApi(ItemColecaoRepositoryJdbc(dataSource), CatalogoCartasProvisorio())
}

fun Application.configurarApi(repositorio: ItemColecaoRepository, catalogo: CatalogoCartas) {
    install(ContentNegotiation) { json() }
    instalarTratamentoDeErros()
    healthRoutes()
    routing {
        colecaoRoutes(
            adicionar = AdicionarItemColecao(catalogo, repositorio),
            obter = ObterItemColecao(repositorio),
            listar = ListarItensColecao(repositorio),
            atualizar = AtualizarItemColecao(repositorio),
            remover = RemoverItemColecao(repositorio),
        )
    }
}