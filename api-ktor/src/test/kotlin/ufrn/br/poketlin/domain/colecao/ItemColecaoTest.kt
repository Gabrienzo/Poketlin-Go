package ufrn.br.poketlin.domain.colecao

import ufrn.br.poketlin.domain.carta.Carta
import ufrn.br.poketlin.domain.carta.CartaId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ItemColecaoTest {
    private val carta = Carta(CartaId("base1-4"), "Charizard", "Base Set")

    private fun item(quantidade: Int = 1) = ItemColecao(
        id = ItemColecaoId.novo(),
        carta = carta,
        estado = EstadoConservacao.NEAR_MINT,
        quantidade = quantidade,
        adicionadoEm = Instant.parse("2026-09-28T12:00:00Z"),
    )

    @Test
    fun `rejeita quantidade menor que 1`() {
        assertFailsWith<IllegalArgumentException> { item(quantidade = 0) }
    }

    @Test
    fun `alterar estado preserva o restante do item`() {
        val alterado = item().comEstado(EstadoConservacao.DAMAGED)
        assertEquals(EstadoConservacao.DAMAGED, alterado.estado)
        assertEquals(carta, alterado.carta)
    }

    @Test
    fun `alterar quantidade valida o novo valor`() {
        assertFailsWith<IllegalArgumentException> { item().comQuantidade(0) }
    }
}