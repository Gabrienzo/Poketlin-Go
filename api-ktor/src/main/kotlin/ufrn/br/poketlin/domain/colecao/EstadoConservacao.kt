package ufrn.br.poketlin.domain.colecao

enum class EstadoConservacao(val sigla: String, val descricao: String) {
    MINT("M", "Mint"),
    NEAR_MINT("NM", "Near Mint"),
    LIGHTLY_PLAYED("LP", "Lightly Played"),
    MODERATELY_PLAYED("MP", "Moderately Played"),
    HEAVILY_PLAYED("HP", "Heavily Played"),
    DAMAGED("DMG", "Damaged");

    companion object {
        /** Aceita o nome (NEAR_MINT) ou a sigla (NM), sem diferenciar maiúsculas de minúsculas. */
        fun deTexto(valor: String): EstadoConservacao? {
            val texto = valor.trim()
            return entries.firstOrNull {
                it.name.equals(texto, ignoreCase = true) || it.sigla.equals(texto, ignoreCase = true)
            }
        }
    }
}