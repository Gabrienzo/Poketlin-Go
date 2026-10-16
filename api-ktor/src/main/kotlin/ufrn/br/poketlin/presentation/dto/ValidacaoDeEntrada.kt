package ufrn.br.poketlin.presentation.dto

import io.ktor.http.Parameters
import ufrn.br.poketlin.domain.carta.CartaId
import ufrn.br.poketlin.domain.colecao.EstadoConservacao
import ufrn.br.poketlin.domain.comum.Paginacao
import ufrn.br.poketlin.presentation.erros.ErroCampo
import ufrn.br.poketlin.presentation.erros.ErroDeValidacao

fun estadoOuNull(valor: String): EstadoConservacao? = EstadoConservacao.deTexto(valor)

fun erroEstado() = ErroCampo(
    "estado",
    "valor inválido; aceitos: " + EstadoConservacao.entries.joinToString { "${it.sigla} (${it.name})" },
)

class NovoItemValidado(val cartaId: CartaId, val estado: EstadoConservacao, val quantidade: Int)

class AtualizacaoValidada(val estado: EstadoConservacao?, val quantidade: Int?)

fun NovoItemColecaoRequest.validar(): NovoItemValidado {
    val erros = mutableListOf<ErroCampo>()

    val id = cartaId?.takeIf { it.isNotBlank() }
    if (id == null) {
        erros += ErroCampo("cartaId", "campo obrigatório")
    }

    val estadoValido = estado?.let { estadoOuNull(it) }
    if (estado == null) {
        erros += ErroCampo("estado", "campo obrigatório")
    } else if (estadoValido == null) {
        erros += erroEstado()
    }

    if (quantidade < 1) {
        erros += ErroCampo("quantidade", "deve ser pelo menos 1")
    }

    if (id == null || estadoValido == null || erros.isNotEmpty()) throw ErroDeValidacao(erros)
    return NovoItemValidado(CartaId(id), estadoValido, quantidade)
}

fun AtualizarItemColecaoRequest.validar(): AtualizacaoValidada {
    val erros = mutableListOf<ErroCampo>()

    if (estado == null && quantidade == null) {
        erros += ErroCampo("corpo", "informe 'estado' e/ou 'quantidade'")
    }

    val estadoValido = estado?.let { estadoOuNull(it) }
    if (estado != null && estadoValido == null) {
        erros += erroEstado()
    }

    if (quantidade != null && quantidade < 1) {
        erros += ErroCampo("quantidade", "deve ser pelo menos 1")
    }

    if (erros.isNotEmpty()) throw ErroDeValidacao(erros)
    return AtualizacaoValidada(estadoValido, quantidade)
}

// Lê limite e cursor da URL. Se o limite for inválido, registra o erro e segue com o padrão;
// quem chama deve usar lancarSeHouver(erros) antes de usar o resultado.
fun Parameters.paginacao(erros: MutableList<ErroCampo>): Paginacao {
    var limite = Paginacao.LIMITE_PADRAO
    val bruto = this["limite"]
    if (bruto != null) {
        val numero = bruto.toIntOrNull()
        if (numero == null) {
            erros += ErroCampo("limite", "deve ser um número inteiro")
        } else if (numero !in 1..Paginacao.LIMITE_MAXIMO) {
            erros += ErroCampo("limite", "deve estar entre 1 e ${Paginacao.LIMITE_MAXIMO}")
        } else {
            limite = numero
        }
    }
    return Paginacao(this["cursor"]?.takeIf { it.isNotBlank() }, limite)
}