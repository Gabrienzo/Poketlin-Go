package ufrn.br.poketlin.domain.carta

import ufrn.br.poketlin.domain.comum.Paginacao
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FiltroCartaTest {
    @Test
    fun `exige pelo menos um criterio de busca`() {
        assertFailsWith<IllegalArgumentException> { FiltroCarta() }
    }

    @Test
    fun `aceita apenas o nome`() {
        FiltroCarta(nome = "Pikachu")
    }

    @Test
    fun `limite de paginacao fora da faixa e rejeitado`() {
        assertFailsWith<IllegalArgumentException> { Paginacao(limite = 0) }
        assertFailsWith<IllegalArgumentException> { Paginacao(limite = 101) }
    }
}