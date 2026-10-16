package ufrn.br.poketlin.presentation.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ufrn.br.poketlin.configurarApi
import ufrn.br.poketlin.domain.carta.*
import ufrn.br.poketlin.domain.comum.Pagina
import ufrn.br.poketlin.domain.comum.Paginacao
import ufrn.br.poketlin.infrastructure.persistencia.ItemColecaoRepositoryEmMemoria

class ColecaoRoutesTest {
    private val charizard = Carta(CartaId("base1-4"), "Charizard", "Base Set")

    private val catalogo =
            object : CatalogoCartas {
                override suspend fun buscarPorId(id: CartaId) =
                        if (id == charizard.id) charizard else null
                override suspend fun buscar(filtro: FiltroCarta, paginacao: Paginacao) =
                        Pagina(listOf(charizard), null)
            }

    private fun teste(bloco: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        environment {
            config = MapApplicationConfig()
        } // não carrega o application.conf (nem o banco)
        application { configurarApi(ItemColecaoRepositoryEmMemoria(), catalogo) }
        bloco()
    }

    private suspend fun ApplicationTestBuilder.adicionar(corpo: String) =
            client.post("/colecao/itens") {
                contentType(ContentType.Application.Json)
                setBody(corpo)
            }

    private val corpoValido = """{"cartaId":"base1-4","estado":"NEAR_MINT","quantidade":2}"""

    @Test
    fun `POST cria o item e responde 201 com Location`() = teste {
        val resposta = adicionar(corpoValido)
        assertEquals(HttpStatusCode.Created, resposta.status)
        assertTrue(resposta.headers[HttpHeaders.Location]!!.startsWith("/colecao/itens/"))
        assertTrue(resposta.bodyAsText().contains("Charizard"))
    }

    @Test
    fun `POST repetido da mesma carta no mesmo estado responde 409 em problem details`() = teste {
        adicionar(corpoValido)
        val resposta = adicionar(corpoValido)
        assertEquals(HttpStatusCode.Conflict, resposta.status)
        assertTrue(
                resposta.headers[HttpHeaders.ContentType]!!.startsWith("application/problem+json")
        )
    }

    @Test
    fun `POST com quantidade zero responde 400`() = teste {
        val resposta = adicionar("""{"cartaId":"base1-4","estado":"MINT","quantidade":0}""")
        assertEquals(HttpStatusCode.BadRequest, resposta.status)
    }

    @Test
    fun `POST de carta inexistente responde 404`() = teste {
        val resposta = adicionar("""{"cartaId":"xx-99","estado":"MINT"}""")
        assertEquals(HttpStatusCode.NotFound, resposta.status)
    }

    @Test
    fun `GET de id inexistente responde 404 e id malformado responde 400`() = teste {
        assertEquals(
                HttpStatusCode.NotFound,
                client.get("/colecao/itens/${java.util.UUID.randomUUID()}").status
        )
        assertEquals(HttpStatusCode.BadRequest, client.get("/colecao/itens/abc").status)
    }
}
