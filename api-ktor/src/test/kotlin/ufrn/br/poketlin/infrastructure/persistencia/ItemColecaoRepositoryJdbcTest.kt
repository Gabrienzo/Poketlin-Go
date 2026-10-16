package ufrn.br.poketlin.infrastructure.persistencia

import kotlinx.coroutines.runBlocking
import ufrn.br.poketlin.domain.carta.Carta
import ufrn.br.poketlin.domain.carta.CartaId
import ufrn.br.poketlin.domain.colecao.EstadoConservacao
import ufrn.br.poketlin.domain.colecao.ItemColecao
import ufrn.br.poketlin.domain.colecao.ItemColecaoId
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemColecaoRepositoryJdbcTest {
    private val dataSource = criarDataSource()
    private val repositorio = ItemColecaoRepositoryJdbc(dataSource)

    @BeforeTest
    fun preparar() {
        migrarBanco(dataSource)
        dataSource.connection.use { it.prepareStatement("DELETE FROM item_colecao").execute() }
        dataSource.connection.use { it.prepareStatement("DELETE FROM carta").execute() }
        dataSource.connection.use { conexao ->
            conexao.prepareStatement("INSERT INTO carta (id, nome, edicao) VALUES ('base1-4', 'Charizard', 'Base Set')")
                .execute()
        }
    }

    @Test
    fun `salva e busca um item por id`() = runBlocking {
        val item = ItemColecao(
            id = ItemColecaoId.novo(),
            carta = Carta(CartaId("base1-4"), "Charizard", "Base Set"),
            estado = EstadoConservacao.NEAR_MINT,
            quantidade = 1,
            adicionadoEm = Instant.now(),
        )

        repositorio.salvar(item)
        val encontrado = repositorio.buscarPorId(item.id)

        assertEquals(item.carta.nome, encontrado?.carta?.nome)
    }
}