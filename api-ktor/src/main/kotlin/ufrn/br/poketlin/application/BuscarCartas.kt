package ufrn.br.poketlin.application

import ufrn.br.poketlin.domain.carta.Carta
import ufrn.br.poketlin.domain.carta.CatalogoCartas
import ufrn.br.poketlin.domain.carta.FiltroCarta
import ufrn.br.poketlin.domain.comum.Pagina
import ufrn.br.poketlin.domain.comum.Paginacao

class BuscarCartas(private val catalogo: CatalogoCartas) {
    suspend operator fun invoke(filtro: FiltroCarta, paginacao: Paginacao): Pagina<Carta> =
        catalogo.buscar(filtro, paginacao)
}