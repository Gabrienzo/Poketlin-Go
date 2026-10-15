package ufrn.br.poketlin.presentation.routes

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import ufrn.br.poketlin.application.BuscarCartas
import ufrn.br.poketlin.domain.carta.CartaId
import ufrn.br.poketlin.domain.carta.FiltroCarta
import ufrn.br.poketlin.domain.comum.Paginacao
import ufrn.br.poketlin.presentation.dto.toCartasDto
import ufrn.br.poketlin.presentation.erros.RequisicaoInvalida

fun Route.cartasRoutes(buscar: BuscarCartas) {
    get("/cartas") {
        val p = call.request.queryParameters
        val filtro = FiltroCarta(
            nome = p["nome"]?.takeIf { it.isNotBlank() },
            id = p["id"]?.takeIf { it.isNotBlank() }?.let { CartaId(it) },
            edicao = p["edicao"]?.takeIf { it.isNotBlank() },
        )
        val limite = p["limite"]?.let { it.toIntOrNull() ?: throw RequisicaoInvalida("'limite' deve ser um número") }
            ?: Paginacao.LIMITE_PADRAO
        val cursor = p["cursor"]?.takeIf { it.isNotBlank() }

        call.respond(buscar(filtro, Paginacao(cursor, limite)).toCartasDto())
    }
}