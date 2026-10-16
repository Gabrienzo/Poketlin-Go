package ufrn.br.poketlin.infrastructure

import ufrn.br.poketlin.domain.carta.*
import ufrn.br.poketlin.domain.comum.Pagina
import ufrn.br.poketlin.domain.comum.Paginacao

/** Provisório: será substituído pelo cliente gRPC do worker (TCGDex). */
class CatalogoCartasProvisorio : CatalogoCartas {
    private val cartas = listOf(
        Carta(CartaId("base1-4"), "Charizard", "Base Set"),
        Carta(CartaId("base1-58"), "Pikachu", "Base Set"),
        Carta(CartaId("base1-2"), "Blastoise", "Base Set"),
    ).associateBy { it.id }

    override suspend fun buscarPorId(id: CartaId): Carta? = cartas[id]

    override suspend fun buscar(filtro: FiltroCarta, paginacao: Paginacao): Pagina<Carta> {
        val achadas = cartas.values.filter {
            (filtro.id == null || it.id == filtro.id) &&
                (filtro.nome == null || it.nome.contains(filtro.nome, ignoreCase = true)) &&
                (filtro.edicao == null || it.edicao.contains(filtro.edicao, ignoreCase = true))
        }
        return Pagina(achadas, proximoCursor = null)
    }
}