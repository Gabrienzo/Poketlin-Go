package ufrn.br.poketlin.domain.carta

@JvmInline
value class CartaId(val valor: String) {
    init { require(valor.isNotBlank()) { "O id da carta não pode ser vazio" } }
    override fun toString() = valor
}

data class Carta(
    val id: CartaId,
    val nome: String,
    val edicao: String,
    val imagemUrl: String? = null,
) {
    init {
        require(nome.isNotBlank()) { "O nome da carta não pode ser vazio" }
        require(edicao.isNotBlank()) { "A edição da carta não pode ser vazia" }
    }
}