package ufrn.br.poketlin.presentation.routes

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import ufrn.br.poketlin.configurarApi
import ufrn.br.poketlin.domain.carta.Carta
import ufrn.br.poketlin.domain.carta.CartaId
import ufrn.br.poketlin.domain.carta.CatalogoCartas
import ufrn.br.poketlin.domain.carta.FiltroCarta
import ufrn.br.poketlin.domain.comum.Pagina
import ufrn.br.poketlin.domain.comum.Paginacao
import ufrn.br.poketlin.infrastructure.persistencia.ItemColecaoRepositoryEmMemoria
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiContratoTest {
    private val catalogoVazio = object : CatalogoCartas {
        override suspend fun buscarPorId(id: CartaId): Carta? = null
        override suspend fun buscar(filtro: FiltroCarta, paginacao: Paginacao) =
            Pagina<Carta>(emptyList(), null)
    }

    private fun teste(bloco: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        environment { config = MapApplicationConfig() }
        application { configurarApi(ItemColecaoRepositoryEmMemoria(), catalogoVazio) }
        bloco()
    }

    @Test
    fun `especificacao OpenAPI lista as rotas documentadas`() = teste {
        val resposta = client.get("/api.json")
        val corpo = resposta.bodyAsText()
        assertEquals(HttpStatusCode.OK, resposta.status)
        assertTrue(corpo.contains("/colecao/itens"))
        assertTrue(corpo.contains("/cartas"))
    }

    @Test
    fun `POST com corpo invalido lista cada campo com erro`() = teste {
        val resposta = client.post("/colecao/itens") {
            contentType(ContentType.Application.Json)
            setBody("""{"quantidade":0}""")
        }
        val corpo = resposta.bodyAsText()
        assertEquals(HttpStatusCode.BadRequest, resposta.status)
        assertTrue(corpo.contains("\"campo\":\"cartaId\""))
        assertTrue(corpo.contains("\"campo\":\"estado\""))
        assertTrue(corpo.contains("\"campo\":\"quantidade\""))
    }

    @Test
    fun `GET cartas sem filtro e com limite invalido lista os dois erros`() = teste {
        val resposta = client.get("/cartas?limite=abc")
        val corpo = resposta.bodyAsText()
        assertEquals(HttpStatusCode.BadRequest, resposta.status)
        assertTrue(corpo.contains("\"campo\":\"limite\""))
        assertTrue(corpo.contains("\"campo\":\"filtro\""))
    }
}