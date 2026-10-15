package ufrn.br.poketlin.infrastructure.catalogo

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ufrn.br.poketlin.domain.CatalogoIndisponivel
import ufrn.br.poketlin.domain.carta.Carta
import ufrn.br.poketlin.domain.carta.CartaId
import ufrn.br.poketlin.domain.carta.CatalogoCartas
import ufrn.br.poketlin.domain.carta.FiltroCarta
import ufrn.br.poketlin.domain.comum.Pagina
import ufrn.br.poketlin.domain.comum.Paginacao
import ufrn.br.poketlin.infrastructure.CardRequest
import ufrn.br.poketlin.infrastructure.CardResponse
import ufrn.br.poketlin.infrastructure.SearchCardsRequest
import ufrn.br.poketlin.infrastructure.TcgDataServiceGrpc
import java.util.concurrent.TimeUnit

fun criarCanalWorker(): ManagedChannel =
    ManagedChannelBuilder
        .forTarget(System.getenv("WORKER_ADDR") ?: "localhost:50051")
        .usePlaintext()
        .build()

class CatalogoCartasGrpc(canal: ManagedChannel) : CatalogoCartas {
    private val stub = TcgDataServiceGrpc.newBlockingStub(canal)

    override suspend fun buscarPorId(id: CartaId): Carta? = withContext(Dispatchers.IO) {
        val resposta = chamar { it.getPokemonCard(CardRequest.newBuilder().setCardId(id.valor).build()) }
        if (resposta.success) resposta.toCarta() else null
    }

    override suspend fun buscar(filtro: FiltroCarta, paginacao: Paginacao): Pagina<Carta> {
        val id = filtro.id
        if (id != null) {
            // Busca por ID reaproveita o RPC que já existia; nome e edição, se vierem, só confirmam o resultado.
            val encontrada = buscarPorId(id)?.takeIf { c ->
                (filtro.nome == null || c.nome.contains(filtro.nome, ignoreCase = true)) &&
                    (filtro.edicao == null || c.edicao.contains(filtro.edicao, ignoreCase = true))
            }
            return Pagina(listOfNotNull(encontrada), proximoCursor = null)
        }

        val pagina = paginaDoCursor(paginacao.cursor)
        val requisicao = SearchCardsRequest.newBuilder()
            .setName(filtro.nome.orEmpty())
            .setSet(filtro.edicao.orEmpty())
            .setPage(pagina)
            .setPageSize(paginacao.limite)
            .build()

        val resposta = withContext(Dispatchers.IO) { chamar { it.searchCards(requisicao) } }
        return Pagina(
            itens = resposta.cardsList.map { it.toCarta() },
            proximoCursor = if (resposta.hasMore) (pagina + 1).toString() else null,
        )
    }

    // Aqui o cursor é o número da página. Quem usa a porta não sabe disso: para ela o cursor é opaco.
    private fun paginaDoCursor(cursor: String?): Int {
        if (cursor == null) return 1
        return cursor.toIntOrNull()?.takeIf { it >= 1 }
            ?: throw IllegalArgumentException("Cursor inválido: '$cursor'")
    }

    private fun <T> chamar(bloco: (TcgDataServiceGrpc.TcgDataServiceBlockingStub) -> T): T =
        try {
            bloco(stub.withDeadlineAfter(15, TimeUnit.SECONDS))
        } catch (e: StatusRuntimeException) {
            throw CatalogoIndisponivel(e.status.code.name)
        }

    private fun CardResponse.toCarta() = Carta(
        id = CartaId(id),
        nome = name,
        edicao = setName.ifBlank { "Desconhecida" },
        imagemUrl = imageUrl.ifBlank { null },
    )
}