package ufrn.br.poketlin.presentation.routes

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.application.call
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import ufrn.br.poketlin.application.*
import ufrn.br.poketlin.domain.carta.CartaId
import ufrn.br.poketlin.domain.colecao.FiltroItemColecao
import ufrn.br.poketlin.domain.colecao.ItemColecaoId
import ufrn.br.poketlin.domain.comum.Paginacao
import ufrn.br.poketlin.presentation.dto.*
import ufrn.br.poketlin.presentation.erros.RequisicaoInvalida

private suspend inline fun <reified T : Any> ApplicationCall.receberCorpo(): T =
        try {
            receive<T>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw RequisicaoInvalida("Corpo da requisição ausente ou malformado")
        }

private fun ApplicationCall.itemId(): ItemColecaoId {
    val bruto = parameters["id"].orEmpty()
    return runCatching { ItemColecaoId(UUID.fromString(bruto)) }.getOrElse {
        throw RequisicaoInvalida("'$bruto' não é um id válido")
    }
}

fun Route.colecaoRoutes(
        adicionar: AdicionarItemColecao,
        obter: ObterItemColecao,
        listar: ListarItensColecao,
        atualizar: AtualizarItemColecao,
        remover: RemoverItemColecao,
) {
    route("/colecao/itens") {
        post {
            val req = call.receberCorpo<NovoItemColecaoRequest>()
            val item = adicionar(CartaId(req.cartaId), parseEstado(req.estado), req.quantidade)
            call.response.header(HttpHeaders.Location, "/colecao/itens/${item.id.valor}")
            call.respond(HttpStatusCode.Created, item.toDto())
        }

        get {
            val p = call.request.queryParameters
            val filtro =
                    FiltroItemColecao(
                            nome = p["nome"],
                            edicao = p["edicao"],
                            estado = p["estado"]?.let(::parseEstado),
                    )
            val limite =
                    p["limite"]?.let {
                        it.toIntOrNull() ?: throw RequisicaoInvalida("'limite' deve ser um número")
                    }
                            ?: Paginacao.LIMITE_PADRAO
            call.respond(listar(filtro, Paginacao(p["cursor"], limite)).toDto())
        }

        get("/{id}") { call.respond(obter(call.itemId()).toDto()) }

        patch("/{id}") {
            val id = call.itemId()
            val req = call.receberCorpo<AtualizarItemColecaoRequest>()
            if (req.estado == null && req.quantidade == null) {
                throw RequisicaoInvalida("Informe 'estado' e/ou 'quantidade'")
            }
            call.respond(atualizar(id, req.estado?.let(::parseEstado), req.quantidade).toDto())
        }

        delete("/{id}") {
            remover(call.itemId())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
