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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EstadosRoutesTest {
    private val charizard = Carta(CartaId("base1-4"), "Charizard", "Base Set")

    private val catalogo = object : CatalogoCartas {
        override suspend fun buscarPorId(id: CartaId) = if (id == charizard.id) charizard else null
        override suspend fun buscar(filtro: FiltroCarta, paginacao: Paginacao) =
            Pagina(listOf(charizard), null)
    }

    private fun teste(bloco: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        environment { config = MapApplicationConfig() }
        application { configurarApi(ItemColecaoRepositoryEmMemoria(), catalogo) }
        bloco()
    }

    private suspend fun ApplicationTestBuilder.adicionar(corpo: String) =
        client.post("/colecao/itens") {
            contentType(ContentType.Application.Json)
            setBody(corpo)
        }

    @Test
    fun `POST aceita a sigla em qualquer caixa e devolve nome e sigla`() = teste {
        val resposta = adicionar("""{"cartaId":"base1-4","estado":"nm"}""")
        val corpo = resposta.bodyAsText()
        assertEquals(HttpStatusCode.Created, resposta.status)
        assertTrue(corpo.contains("\"estado\":\"NEAR_MINT\""))
        assertTrue(corpo.contains("\"estadoSigla\":\"NM\""))
    }

    @Test
    fun `POST com sigla desconhecida responde 400 apontando o campo estado`() = teste {
        val resposta = adicionar("""{"cartaId":"base1-4","estado":"XX"}""")
        assertEquals(HttpStatusCode.BadRequest, resposta.status)
        assertTrue(resposta.bodyAsText().contains("\"campo\":\"estado\""))
    }

    @Test
    fun `sigla e nome do mesmo estado contam como duplicata`() = teste {
        adicionar("""{"cartaId":"base1-4","estado":"M"}""")
        val resposta = adicionar("""{"cartaId":"base1-4","estado":"MINT"}""")
        assertEquals(HttpStatusCode.Conflict, resposta.status)
    }

    @Test
    fun `GET estados lista os seis estados com sigla`() = teste {
        val resposta = client.get("/colecao/estados")
        val corpo = resposta.bodyAsText()
        assertEquals(HttpStatusCode.OK, resposta.status)
        listOf("M", "NM", "LP", "MP", "HP", "DMG").forEach {
            assertTrue(corpo.contains("\"sigla\":\"$it\""))
        }
    }

    @Test
    fun `listagem filtra pela sigla do estado`() = teste {
        adicionar("""{"cartaId":"base1-4","estado":"NM"}""")
        assertTrue(client.get("/colecao/itens?estado=NM").bodyAsText().contains("Charizard"))
        assertFalse(client.get("/colecao/itens?estado=DMG").bodyAsText().contains("Charizard"))
    }
}