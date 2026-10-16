package ufrn.br.poketlin.presentation.routes

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import ufrn.br.poketlin.configurarApi
import ufrn.br.poketlin.domain.carta.Carta
import ufrn.br.poketlin.domain.carta.CartaId
import ufrn.br.poketlin.domain.carta.CatalogoCartas
import ufrn.br.poketlin.domain.carta.FiltroCarta
import ufrn.br.poketlin.domain.comum.Pagina
import ufrn.br.poketlin.domain.comum.Paginacao
import ufrn.br.poketlin.infrastructure.persistencia.ItemColecaoRepositoryEmMemoria

class CartasRoutesTest {
    private val charizard = Carta(CartaId("base1-4"), "Charizard", "Base Set")
    private val pikachu = Carta(CartaId("base1-58"), "Pikachu", "Base Set")

    private val catalogo =
            object : CatalogoCartas {
                override suspend fun buscarPorId(id: CartaId) =
                        listOf(charizard, pikachu).find { it.id == id }
                override suspend fun buscar(
                        filtro: FiltroCarta,
                        paginacao: Paginacao
                ): Pagina<Carta> {
                    val nome = filtro.nome
                    val achadas =
                            listOf(charizard, pikachu).filter {
                                nome == null || it.nome.contains(nome, ignoreCase = true)
                            }
                    return Pagina(achadas, proximoCursor = "2")
                }
            }

    private fun teste(bloco: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        environment { config = MapApplicationConfig() }
        application { configurarApi(ItemColecaoRepositoryEmMemoria(), catalogo) }
        bloco()
    }

    @Test
    fun `GET cartas com nome responde 200 com os resultados e o cursor`() = teste {
        val resposta = client.get("/cartas?nome=char")
        val corpo = resposta.bodyAsText()
        assertEquals(HttpStatusCode.OK, resposta.status)
        assertTrue(corpo.contains("Charizard"))
        assertFalse(corpo.contains("Pikachu"))
        assertTrue(corpo.contains("\"proximoCursor\":\"2\""))
    }

    @Test
    fun `GET cartas sem nenhum filtro responde 400`() = teste {
        assertEquals(HttpStatusCode.BadRequest, client.get("/cartas").status)
    }

    @Test
    fun `GET cartas com limite invalido responde 400`() = teste {
        assertEquals(HttpStatusCode.BadRequest, client.get("/cartas?nome=char&limite=0").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/cartas?nome=char&limite=abc").status)
    }
}
