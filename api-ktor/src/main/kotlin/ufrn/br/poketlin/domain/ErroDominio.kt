package ufrn.br.poketlin.domain

import ufrn.br.poketlin.domain.carta.CartaId
import ufrn.br.poketlin.domain.colecao.EstadoConservacao
import ufrn.br.poketlin.domain.colecao.ItemColecaoId

sealed class ErroDeDominio(mensagem: String) : RuntimeException(mensagem)

class CartaNaoEncontrada(val id: CartaId) :
    ErroDeDominio("Carta '$id' não encontrada no catálogo")

class ItemColecaoNaoEncontrado(val id: ItemColecaoId) :
    ErroDeDominio("Item '${id.valor}' não encontrado na coleção")

class CartaJaNaColecao(val cartaId: CartaId, val estado: EstadoConservacao) :
    ErroDeDominio("A carta '$cartaId' já está na coleção no estado $estado")