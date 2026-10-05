package ufrn.br.poketlin.infrastructure.persistencia

import java.sql.ResultSet
import java.util.UUID
import javax.sql.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ufrn.br.poketlin.domain.carta.Carta
import ufrn.br.poketlin.domain.carta.CartaId
import ufrn.br.poketlin.domain.colecao.EstadoConservacao
import ufrn.br.poketlin.domain.colecao.FiltroItemColecao
import ufrn.br.poketlin.domain.colecao.ItemColecao
import ufrn.br.poketlin.domain.colecao.ItemColecaoId
import ufrn.br.poketlin.domain.colecao.ItemColecaoRepository
import ufrn.br.poketlin.domain.comum.Pagina
import ufrn.br.poketlin.domain.comum.Paginacao

class ItemColecaoRepositoryJdbc(private val dataSource: DataSource) : ItemColecaoRepository {

    override suspend fun salvar(item: ItemColecao): ItemColecao =
            withContext(Dispatchers.IO) {
                dataSource.connection.use { conexao ->
                    conexao.prepareStatement(
                                    """
                INSERT INTO item_colecao (id, carta_id, estado, quantidade, adicionado_em)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE
                SET estado = EXCLUDED.estado, quantidade = EXCLUDED.quantidade
                """.trimIndent()
                            )
                            .use { stmt ->
                                stmt.setObject(1, item.id.valor)
                                stmt.setString(2, item.carta.id.valor)
                                stmt.setString(3, item.estado.name)
                                stmt.setInt(4, item.quantidade)
                                stmt.setObject(
                                        5,
                                        item.adicionadoEm.atOffset(java.time.ZoneOffset.UTC)
                                )
                                stmt.executeUpdate()
                            }
                }
                item
            }

    override suspend fun buscarPorId(id: ItemColecaoId): ItemColecao? =
            withContext(Dispatchers.IO) {
                dataSource.connection.use { conexao ->
                    conexao.prepareStatement(SELECT_COM_CARTA + " WHERE ic.id = ?").use { stmt ->
                        stmt.setObject(1, id.valor)
                        stmt.executeQuery().use { rs -> if (rs.next()) mapear(rs) else null }
                    }
                }
            }

    override suspend fun buscarPorCartaEEstado(
            cartaId: CartaId,
            estado: EstadoConservacao,
    ): ItemColecao? =
            withContext(Dispatchers.IO) {
                dataSource.connection.use { conexao ->
                    conexao.prepareStatement(
                                    SELECT_COM_CARTA + " WHERE ic.carta_id = ? AND ic.estado = ?"
                            )
                            .use { stmt ->
                                stmt.setString(1, cartaId.valor)
                                stmt.setString(2, estado.name)
                                stmt.executeQuery().use { rs ->
                                    if (rs.next()) mapear(rs) else null
                                }
                            }
                }
            }

    override suspend fun listar(
            filtro: FiltroItemColecao,
            paginacao: Paginacao,
    ): Pagina<ItemColecao> =
            withContext(Dispatchers.IO) {
                val condicoes = mutableListOf<String>()
                val parametros = mutableListOf<Any>()

                filtro.nome?.let {
                    condicoes += "c.nome ILIKE ?"
                    parametros += "%$it%"
                }
                filtro.edicao?.let {
                    condicoes += "c.edicao ILIKE ?"
                    parametros += "%$it%"
                }
                filtro.estado?.let {
                    condicoes += "ic.estado = ?"
                    parametros += it.name
                }

                val offset = paginacao.cursor?.toIntOrNull() ?: 0
                val whereSql =
                        if (condicoes.isEmpty()) "" else "WHERE " + condicoes.joinToString(" AND ")
                val sql =
                        "$SELECT_COM_CARTA $whereSql ORDER BY ic.adicionado_em DESC LIMIT ? OFFSET ?"

                dataSource.connection.use { conexao ->
                    conexao.prepareStatement(sql).use { stmt ->
                        var indice = 1
                        parametros.forEach { stmt.setObject(indice++, it) }
                        stmt.setInt(
                                indice++,
                                paginacao.limite + 1
                        ) // +1 para saber se há próxima página
                        stmt.setInt(indice, offset)

                        val itens =
                                stmt.executeQuery().use { rs ->
                                    generateSequence { if (rs.next()) mapear(rs) else null }
                                            .toList()
                                }

                        val temProximaPagina = itens.size > paginacao.limite
                        val itensDaPagina = itens.take(paginacao.limite)
                        val proximoCursor =
                                if (temProximaPagina) (offset + paginacao.limite).toString()
                                else null

                        Pagina(itensDaPagina, proximoCursor)
                    }
                }
            }

    override suspend fun remover(id: ItemColecaoId): Boolean =
            withContext(Dispatchers.IO) {
                dataSource.connection.use { conexao ->
                    conexao.prepareStatement("DELETE FROM item_colecao WHERE id = ?").use { stmt ->
                        stmt.setObject(1, id.valor)
                        stmt.executeUpdate() > 0
                    }
                }
            }

    private fun mapear(rs: ResultSet): ItemColecao =
            ItemColecao(
                    id = ItemColecaoId(rs.getObject("id", UUID::class.java)),
                    carta =
                            Carta(
                                    id = CartaId(rs.getString("carta_id")),
                                    nome = rs.getString("nome"),
                                    edicao = rs.getString("edicao"),
                                    imagemUrl = rs.getString("imagem_url"),
                            ),
                    estado = EstadoConservacao.valueOf(rs.getString("estado")),
                    quantidade = rs.getInt("quantidade"),
                    adicionadoEm =
                            rs.getObject("adicionado_em", java.time.OffsetDateTime::class.java)
                                    .toInstant(),
            )

    companion object {
        private const val SELECT_COM_CARTA =
                """
            SELECT ic.id, ic.carta_id, ic.estado, ic.quantidade, ic.adicionado_em,
                   c.nome, c.edicao, c.imagem_url
            FROM item_colecao ic
            JOIN carta c ON c.id = ic.carta_id
        """
    }
}
