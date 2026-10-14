package ufrn.br.poketlin.presentation.dto

import kotlinx.serialization.Serializable
import ufrn.br.poketlin.domain.carta.Carta
import ufrn.br.poketlin.domain.colecao.EstadoConservacao
import ufrn.br.poketlin.domain.colecao.ItemColecao
import ufrn.br.poketlin.domain.comum.Pagina

@Serializable
data class CartaDto(val id: String, val nome: String, val edicao: String, val imagemUrl: String? = null)

@Serializable
data class ItemColecaoDto(
    val id: String,
    val carta: CartaDto,
    val estado: String,
    val quantidade: Int,
    val adicionadoEm: String,
)

@Serializable
data class NovoItemColecaoRequest(val cartaId: String, val estado: String, val quantidade: Int = 1)

@Serializable
data class AtualizarItemColecaoRequest(val estado: String? = null, val quantidade: Int? = null)

@Serializable
data class PaginaDto<T>(val itens: List<T>, val proximoCursor: String?)

fun Carta.toDto() = CartaDto(id.valor, nome, edicao, imagemUrl)

fun ItemColecao.toDto() =
    ItemColecaoDto(id.valor.toString(), carta.toDto(), estado.name, quantidade, adicionadoEm.toString())

fun Pagina<ItemColecao>.toDto() = PaginaDto(itens.map { it.toDto() }, proximoCursor)

fun parseEstado(valor: String): EstadoConservacao =
    EstadoConservacao.entries.firstOrNull { it.name.equals(valor, ignoreCase = true) }
        ?: throw IllegalArgumentException(
            "Estado inválido: '$valor'. Valores aceitos: ${EstadoConservacao.entries.joinToString()}"
        )