package ufrn.br.poketlin.domain.colecao

import ufrn.br.poketlin.domain.carta.Carta
import java.time.Instant
import java.util.UUID

@JvmInline
value class ItemColecaoId(val valor: UUID) {
    companion object { fun novo() = ItemColecaoId(UUID.randomUUID()) }
}

data class ItemColecao(
    val id: ItemColecaoId,
    val carta: Carta,
    val estado: EstadoConservacao,
    val quantidade: Int,
    val adicionadoEm: Instant,
) {
    init { require(quantidade >= 1) { "A quantidade deve ser pelo menos 1" } }

    fun comEstado(novo: EstadoConservacao) = copy(estado = novo)
    fun comQuantidade(nova: Int) = copy(quantidade = nova)
}