package ufrn.br.poketlin.application

import ufrn.br.poketlin.domain.CartaJaNaColecao
import ufrn.br.poketlin.domain.CartaNaoEncontrada
import ufrn.br.poketlin.domain.ItemColecaoNaoEncontrado
import ufrn.br.poketlin.domain.carta.CartaId
import ufrn.br.poketlin.domain.carta.CatalogoCartas
import ufrn.br.poketlin.domain.colecao.*
import ufrn.br.poketlin.domain.comum.Pagina
import ufrn.br.poketlin.domain.comum.Paginacao
import java.time.Clock

class AdicionarItemColecao(
    private val catalogo: CatalogoCartas,
    private val repositorio: ItemColecaoRepository,
    private val relogio: Clock = Clock.systemUTC(),
) {
    suspend operator fun invoke(cartaId: CartaId, estado: EstadoConservacao, quantidade: Int): ItemColecao {
        val carta = catalogo.buscarPorId(cartaId) ?: throw CartaNaoEncontrada(cartaId)
        if (repositorio.buscarPorCartaEEstado(cartaId, estado) != null) {
            throw CartaJaNaColecao(cartaId, estado)
        }
        val item = ItemColecao(ItemColecaoId.novo(), carta, estado, quantidade, relogio.instant())
        return repositorio.salvar(item)
    }
}

class ObterItemColecao(private val repositorio: ItemColecaoRepository) {
    suspend operator fun invoke(id: ItemColecaoId): ItemColecao =
        repositorio.buscarPorId(id) ?: throw ItemColecaoNaoEncontrado(id)
}

class ListarItensColecao(private val repositorio: ItemColecaoRepository) {
    suspend operator fun invoke(filtro: FiltroItemColecao, paginacao: Paginacao): Pagina<ItemColecao> =
        repositorio.listar(filtro, paginacao)
}

class AtualizarItemColecao(private val repositorio: ItemColecaoRepository) {
    suspend operator fun invoke(id: ItemColecaoId, estado: EstadoConservacao?, quantidade: Int?): ItemColecao {
        val atual = repositorio.buscarPorId(id) ?: throw ItemColecaoNaoEncontrado(id)
        var novo = atual
        if (estado != null && estado != atual.estado) {
            if (repositorio.buscarPorCartaEEstado(atual.carta.id, estado) != null) {
                throw CartaJaNaColecao(atual.carta.id, estado)
            }
            novo = novo.comEstado(estado)
        }
        if (quantidade != null) novo = novo.comQuantidade(quantidade)
        return repositorio.salvar(novo)
    }
}

class RemoverItemColecao(private val repositorio: ItemColecaoRepository) {
    suspend operator fun invoke(id: ItemColecaoId) {
        if (!repositorio.remover(id)) throw ItemColecaoNaoEncontrado(id)
    }
}