package ufrn.br.poketlin.presentation.routes

import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import ufrn.br.poketlin.application.BuscarCartas
import ufrn.br.poketlin.domain.carta.CartaId
import ufrn.br.poketlin.domain.carta.FiltroCarta
import ufrn.br.poketlin.presentation.dto.CartaDto
import ufrn.br.poketlin.presentation.dto.PaginaDto
import ufrn.br.poketlin.presentation.dto.paginacao
import ufrn.br.poketlin.presentation.dto.toCartasDto
import ufrn.br.poketlin.presentation.erros.ErroCampo
import ufrn.br.poketlin.presentation.erros.ProblemDetails
import ufrn.br.poketlin.presentation.erros.lancarSeHouver

fun Route.cartasRoutes(buscar: BuscarCartas) {
    get("/cartas", {
        tags = listOf("Cartas")
        summary = "Busca cartas no catálogo"
        description = "Informe ao menos um filtro: nome, id ou edicao. Para o scroll infinito, " +
            "envie o proximoCursor da resposta anterior no parâmetro cursor."
        request {
            queryParameter<String>("nome") { description = "Trecho do nome da carta"; required = false }
            queryParameter<String>("id") { description = "Id exato da carta, por exemplo base1-4"; required = false }
            queryParameter<String>("edicao") { description = "Nome ou id da edição"; required = false }
            queryParameter<Int>("limite") { description = "Itens por página (1 a 100, padrão 20)"; required = false }
            queryParameter<String>("cursor") { description = "Cursor devolvido pela página anterior"; required = false }
        }
        response {
            HttpStatusCode.OK to { description = "Página de cartas"; body<PaginaDto<CartaDto>>() }
            HttpStatusCode.BadRequest to { description = "Parâmetros inválidos"; body<ProblemDetails>() }
            HttpStatusCode.ServiceUnavailable to { description = "Catálogo indisponível"; body<ProblemDetails>() }
        }
    }) {
        val p = call.request.queryParameters
        val erros = mutableListOf<ErroCampo>()
        val paginacao = p.paginacao(erros)

        val nome = p["nome"]?.takeIf { it.isNotBlank() }
        val id = p["id"]?.takeIf { it.isNotBlank() }
        val edicao = p["edicao"]?.takeIf { it.isNotBlank() }
        if (nome == null && id == null && edicao == null) {
            erros += ErroCampo("filtro", "informe pelo menos um entre nome, id e edicao")
        }
        lancarSeHouver(erros)

        val filtro = FiltroCarta(nome = nome, id = id?.let { CartaId(it) }, edicao = edicao)
        call.respond(buscar(filtro, paginacao).toCartasDto())
    }
}