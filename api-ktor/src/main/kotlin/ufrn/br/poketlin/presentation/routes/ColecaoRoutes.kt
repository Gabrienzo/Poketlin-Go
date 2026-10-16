package ufrn.br.poketlin.presentation.routes

import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.patch
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import ufrn.br.poketlin.application.AdicionarItemColecao
import ufrn.br.poketlin.application.AtualizarItemColecao
import ufrn.br.poketlin.application.ListarItensColecao
import ufrn.br.poketlin.application.ObterItemColecao
import ufrn.br.poketlin.application.RemoverItemColecao
import ufrn.br.poketlin.domain.colecao.FiltroItemColecao
import ufrn.br.poketlin.domain.colecao.EstadoConservacao
import ufrn.br.poketlin.domain.colecao.ItemColecaoId
import ufrn.br.poketlin.presentation.dto.AtualizarItemColecaoRequest
import ufrn.br.poketlin.presentation.dto.EstadoDto
import ufrn.br.poketlin.presentation.dto.ItemColecaoDto
import ufrn.br.poketlin.presentation.dto.NovoItemColecaoRequest
import ufrn.br.poketlin.presentation.dto.PaginaDto
import ufrn.br.poketlin.presentation.dto.erroEstado
import ufrn.br.poketlin.presentation.dto.estadoOuNull
import ufrn.br.poketlin.presentation.dto.paginacao
import ufrn.br.poketlin.presentation.dto.toDto
import ufrn.br.poketlin.presentation.dto.validar
import ufrn.br.poketlin.presentation.erros.ErroCampo
import ufrn.br.poketlin.presentation.erros.ProblemDetails
import ufrn.br.poketlin.presentation.erros.RequisicaoInvalida
import ufrn.br.poketlin.presentation.erros.lancarSeHouver

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
        post({
            tags = listOf("Coleção")
            summary = "Adiciona uma carta à coleção"
            description =
                    "Registra uma carta do catálogo com o estado de conservação (nome, como NEAR_MINT, " +
                            "ou sigla, como NM) e a quantidade. A mesma carta no mesmo estado não pode ser adicionada duas vezes."
            request { body<NovoItemColecaoRequest>() }
            response {
                HttpStatusCode.Created to
                        {
                            description = "Item criado"
                            body<ItemColecaoDto>()
                        }
                HttpStatusCode.BadRequest to
                        {
                            description = "Dados inválidos"
                            body<ProblemDetails>()
                        }
                HttpStatusCode.NotFound to
                        {
                            description = "Carta inexistente no catálogo"
                            body<ProblemDetails>()
                        }
                HttpStatusCode.Conflict to
                        {
                            description = "Carta já está na coleção nesse estado"
                            body<ProblemDetails>()
                        }
            }
        }) {
            val novo = call.receberCorpo<NovoItemColecaoRequest>().validar()
            val item = adicionar(novo.cartaId, novo.estado, novo.quantidade)
            call.response.header(HttpHeaders.Location, "/colecao/itens/${item.id.valor}")
            call.respond(HttpStatusCode.Created, item.toDto())
        }

        get({
            tags = listOf("Coleção")
            summary = "Lista os itens da coleção"
            description =
                    "Paginação por cursor: envie o proximoCursor da resposta anterior para obter a página seguinte."
            request {
                queryParameter<String>("nome") {
                    description = "Trecho do nome da carta"
                    required = false
                }
                queryParameter<String>("edicao") {
                    description = "Trecho do nome da edição"
                    required = false
                }
                queryParameter<String>("estado") {
                    description = "Estado de conservação"
                    required = false
                }
                queryParameter<Int>("limite") {
                    description = "Itens por página (1 a 100, padrão 20)"
                    required = false
                }
                queryParameter<String>("cursor") {
                    description = "Cursor devolvido pela página anterior"
                    required = false
                }
                queryParameter<String>("estado") {
                    description = "Estado de conservação (nome ou sigla, por exemplo NM)"
                    required = false
                }
            }
            response {
                HttpStatusCode.OK to
                        {
                            description = "Página de itens"
                            body<PaginaDto<ItemColecaoDto>>()
                        }
                HttpStatusCode.BadRequest to
                        {
                            description = "Parâmetros inválidos"
                            body<ProblemDetails>()
                        }
            }
        }) {
            val p = call.request.queryParameters
            val erros = mutableListOf<ErroCampo>()
            val paginacao = p.paginacao(erros)
            val estado =
                    p["estado"]?.let { bruto ->
                        estadoOuNull(bruto).also { if (it == null) erros += erroEstado() }
                    }
            lancarSeHouver(erros)

            val filtro =
                    FiltroItemColecao(
                            nome = p["nome"]?.takeIf { it.isNotBlank() },
                            edicao = p["edicao"]?.takeIf { it.isNotBlank() },
                            estado = estado,
                    )
            call.respond(listar(filtro, paginacao).toDto())
        }

        get(
                "/{id}",
                {
                    tags = listOf("Coleção")
                    summary = "Busca um item da coleção pelo id"
                    request { pathParameter<String>("id") { description = "Id do item (UUID)" } }
                    response {
                        HttpStatusCode.OK to
                                {
                                    description = "Item encontrado"
                                    body<ItemColecaoDto>()
                                }
                        HttpStatusCode.BadRequest to
                                {
                                    description = "Id malformado"
                                    body<ProblemDetails>()
                                }
                        HttpStatusCode.NotFound to
                                {
                                    description = "Item inexistente"
                                    body<ProblemDetails>()
                                }
                    }
                }
        ) { call.respond(obter(call.itemId()).toDto()) }

        patch(
                "/{id}",
                {
                    tags = listOf("Coleção")
                    summary = "Altera o estado e/ou a quantidade de um item"
                    request {
                        pathParameter<String>("id") { description = "Id do item (UUID)" }
                        body<AtualizarItemColecaoRequest>()
                    }
                    response {
                        HttpStatusCode.OK to
                                {
                                    description = "Item atualizado"
                                    body<ItemColecaoDto>()
                                }
                        HttpStatusCode.BadRequest to
                                {
                                    description = "Dados inválidos"
                                    body<ProblemDetails>()
                                }
                        HttpStatusCode.NotFound to
                                {
                                    description = "Item inexistente"
                                    body<ProblemDetails>()
                                }
                        HttpStatusCode.Conflict to
                                {
                                    description = "Já existe a carta nesse estado"
                                    body<ProblemDetails>()
                                }
                    }
                }
        ) {
            val id = call.itemId()
            val mudanca = call.receberCorpo<AtualizarItemColecaoRequest>().validar()
            call.respond(atualizar(id, mudanca.estado, mudanca.quantidade).toDto())
        }

        delete(
                "/{id}",
                {
                    tags = listOf("Coleção")
                    summary = "Remove um item da coleção"
                    request { pathParameter<String>("id") { description = "Id do item (UUID)" } }
                    response {
                        HttpStatusCode.NoContent to { description = "Item removido" }
                        HttpStatusCode.BadRequest to
                                {
                                    description = "Id malformado"
                                    body<ProblemDetails>()
                                }
                        HttpStatusCode.NotFound to
                                {
                                    description = "Item inexistente"
                                    body<ProblemDetails>()
                                }
                    }
                }
        ) {
            remover(call.itemId())
            call.respond(HttpStatusCode.NoContent)
        }
    }
        get("/colecao/estados", {
        tags = listOf("Coleção")
        summary = "Lista os estados de conservação aceitos"
        description = "Use o código (por exemplo NEAR_MINT) ou a sigla (por exemplo NM) ao adicionar ou alterar um item."
        response {
            HttpStatusCode.OK to { description = "Estados disponíveis"; body<List<EstadoDto>>() }
        }
    }) {
        call.respond(EstadoConservacao.entries.map { it.toDto() })
    }
}
