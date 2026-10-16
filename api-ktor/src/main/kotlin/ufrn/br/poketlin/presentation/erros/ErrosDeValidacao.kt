package ufrn.br.poketlin.presentation.erros

import kotlinx.serialization.Serializable

@Serializable
data class ErroCampo(val campo: String, val mensagem: String)

class ErroDeValidacao(val erros: List<ErroCampo>) : RuntimeException("Dados inválidos")

fun lancarSeHouver(erros: List<ErroCampo>) {
    if (erros.isNotEmpty()) throw ErroDeValidacao(erros)
}