package ufrn.br.poketlin.domain.colecao

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EstadoConservacaoTest {
    @Test
    fun `cada estado e reconhecido pela sigla e pelo nome`() {
        EstadoConservacao.entries.forEach {
            assertEquals(it, EstadoConservacao.deTexto(it.sigla))
            assertEquals(it, EstadoConservacao.deTexto(it.name))
        }
    }

    @Test
    fun `ignora maiusculas e espacos nas pontas`() {
        assertEquals(EstadoConservacao.NEAR_MINT, EstadoConservacao.deTexto(" nm "))
        assertEquals(EstadoConservacao.MODERATELY_PLAYED, EstadoConservacao.deTexto("moderately_played"))
    }

    @Test
    fun `valor desconhecido devolve null`() {
        assertNull(EstadoConservacao.deTexto("XX"))
        assertNull(EstadoConservacao.deTexto(""))
    }

    @Test
    fun `as siglas sao unicas`() {
        assertEquals(EstadoConservacao.entries.size, EstadoConservacao.entries.map { it.sigla }.toSet().size)
    }
}