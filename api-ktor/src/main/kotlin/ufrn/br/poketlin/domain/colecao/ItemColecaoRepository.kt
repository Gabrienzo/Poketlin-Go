package ufrn.br.poketlin.domain.colecao

import ufrn.br.poketlin.domain.carta.CartaId
import ufrn.br.poketlin.domain.comum.Pagina
import ufrn.br.poketlin.domain.comum.Paginacao

data class FiltroItemColecao(
    val nome: String? = null,
    val edicao: String? = null,
    val estado: EstadoConservacao? = null,
)

interface ItemColecaoRepository {
    suspend fun salvar(item: ItemColecao): ItemColecao
    suspend fun buscarPorId(id: ItemColecaoId): ItemColecao?
    suspend fun buscarPorCartaEEstado(cartaId: CartaId, estado: EstadoConservacao): ItemColecao?
    suspend fun listar(filtro: FiltroItemColecao, paginacao: Paginacao): Pagina<ItemColecao>
    suspend fun remover(id: ItemColecaoId): Boolean
}