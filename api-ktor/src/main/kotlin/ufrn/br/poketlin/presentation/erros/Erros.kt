package ufrn.br.poketlin.presentation.erros

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ufrn.br.poketlin.domain.CartaJaNaColecao
import ufrn.br.poketlin.domain.CartaNaoEncontrada
import ufrn.br.poketlin.domain.CatalogoIndisponivel
import ufrn.br.poketlin.domain.ErroDeDominio
import ufrn.br.poketlin.domain.ItemColecaoNaoEncontrado

class RequisicaoInvalida(mensagem: String) : IllegalArgumentException(mensagem)

@Serializable
data class ProblemDetails(
    val type: String = "about:blank",
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: String? = null,
    val errors: List<ErroCampo>? = null,
)

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    encodeDefaults = true
    explicitNulls = false
}

suspend fun ApplicationCall.responderProblema(
    status: HttpStatusCode,
    titulo: String,
    detalhe: String?,
    erros: List<ErroCampo>? = null,
) {
    val corpo = ProblemDetails(
        title = titulo,
        status = status.value,
        detail = detalhe,
        instance = request.path(),
        errors = erros,
    )
    respondText(json.encodeToString(corpo), ContentType("application", "problem+json"), status)
}

fun Application.instalarTratamentoDeErros() {
    install(StatusPages) {
        exception<ErroDeValidacao> { call, e ->
            call.responderProblema(
                HttpStatusCode.BadRequest,
                "Dados inválidos",
                "Corrija os campos indicados em 'errors'",
                e.erros,
            )
        }
        exception<ErroDeDominio> { call, e ->
            val (status, titulo) = when (e) {
                is CartaNaoEncontrada -> HttpStatusCode.NotFound to "Carta não encontrada"
                is ItemColecaoNaoEncontrado -> HttpStatusCode.NotFound to "Item da coleção não encontrado"
                is CartaJaNaColecao -> HttpStatusCode.Conflict to "Carta já está na coleção"
                is CatalogoIndisponivel -> HttpStatusCode.ServiceUnavailable to "Catálogo de cartas indisponível"
            }
            call.responderProblema(status, titulo, e.message)
        }
        exception<IllegalArgumentException> { call, e ->
            call.responderProblema(HttpStatusCode.BadRequest, "Requisição inválida", e.message)
        }
        exception<Throwable> { call, e ->
            call.application.log.error("Erro não tratado", e)
            call.responderProblema(HttpStatusCode.InternalServerError, "Erro interno", null)
        }
    }
}