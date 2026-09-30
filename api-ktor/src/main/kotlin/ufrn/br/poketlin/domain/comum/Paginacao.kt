package ufrn.br.poketlin.domain.comum

data class Paginacao(val cursor: String? = null, val limite: Int = LIMITE_PADRAO) {
    init { require(limite in 1..LIMITE_MAXIMO) { "O limite deve estar entre 1 e $LIMITE_MAXIMO" } }

    companion object {
        const val LIMITE_PADRAO = 20
        const val LIMITE_MAXIMO = 100
    }
}

data class Pagina<T>(val itens: List<T>, val proximoCursor: String?)