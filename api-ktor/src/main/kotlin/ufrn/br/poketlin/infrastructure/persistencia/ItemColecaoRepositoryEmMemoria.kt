package ufrn.br.poketlin.infrastructure.persistencia

import ufrn.br.poketlin.domain.carta.CartaId
import ufrn.br.poketlin.domain.colecao.*
import ufrn.br.poketlin.domain.comum.Pagina
import ufrn.br.poketlin.domain.comum.Paginacao

class ItemColecaoRepositoryEmMemoria : ItemColecaoRepository {
    private val itens = mutableMapOf<ItemColecaoId, ItemColecao>()

    override suspend fun salvar(item: ItemColecao): ItemColecao {
        itens[item.id] = item
        return item
    }

    override suspend fun buscarPorId(id: ItemColecaoId): ItemColecao? = itens[id]

    override suspend fun buscarPorCartaEEstado(cartaId: CartaId, estado: EstadoConservacao): ItemColecao? =
        itens.values.find { it.carta.id == cartaId && it.estado == estado }

    override suspend fun listar(filtro: FiltroItemColecao, paginacao: Paginacao): Pagina<ItemColecao> {
        val filtrados = itens.values.filter { item ->
            (filtro.nome == null || item.carta.nome.contains(filtro.nome, ignoreCase = true)) &&
                (filtro.edicao == null || item.carta.edicao.contains(filtro.edicao, ignoreCase = true)) &&
                (filtro.estado == null || item.estado == filtro.estado)
        }
        return Pagina(filtrados.toList(), proximoCursor = null) // sem paginação real
    }

    override suspend fun remover(id: ItemColecaoId): Boolean = itens.remove(id) != null
}