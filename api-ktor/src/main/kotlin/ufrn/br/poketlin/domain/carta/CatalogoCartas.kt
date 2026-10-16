package ufrn.br.poketlin.domain.carta

import ufrn.br.poketlin.domain.comum.Pagina
import ufrn.br.poketlin.domain.comum.Paginacao

data class FiltroCarta(
    val nome: String? = null,
    val id: CartaId? = null,
    val edicao: String? = null,
) {
    init {
        require(nome != null || id != null || edicao != null) {
            "Informe pelo menos um filtro: nome, id ou edição"
        }
    }
}

interface CatalogoCartas {
    suspend fun buscarPorId(id: CartaId): Carta?
    suspend fun buscar(filtro: FiltroCarta, paginacao: Paginacao): Pagina<Carta>
}