# 1. Visão Geral do Projeto
    Para jogadores e colecionadores de Pokemon Trading Card Game (TCG).
    Que auxilia a gerir suas coleções e montar baralhos (decks) com base em suas cartas.
    O Poketlin-Go é um catálogo digital de referência para cartas de Pokémon TCG.
    Que disponibiliza, registra, organiza as cartas do usuario.
    Diferente de "TCG Collector", o Poketlin permite a criação de decks dentro da plataforma, alem de ser open source.
    Nosso produto carrega os cards de forma dinamica, alem de permitir importar e exportar decks no formato do TCG-live (jogo oficial virtual) e no formato de busca da LigaPokemon (Marketplace online de cartas)

# 2. Definição do MVP
## No MVP
* Busca de cards por ID ou Nome
* Armazenar coleção na conta do usuário
* Criar decks a partir da coleção
* Exportar lista do Deck (formato padrão)
* Definir o estado da carta na coleção (M, MN, MP... etc.)
## Fora do MVP 
* Carregar as cartas de forma dinamica com scroll infinito
* Separação por Edições
* Organizar Coleção por Pastas
* Completar deck automaticamente
* Importar deck
* Login com autenticação
* Exportar lista do Deck em formato de busca LigaPokemon

# 3. Backlog Inicial
- **backlog no projects:** *https://github.com/Gabrienzo/Poketlin-Go*

# 4. Entidades Principais do Domínio
- **Card (Carta):** Representa uma carta física do jogo.
  *Atributos principais:* ID único, nome, supertipo (Pokémon, Treinador, Energia), tipo (Fogo, Água, etc.), HP, ataques, regras, custo de recuo, raridade e URL da imagem.

- **Set (Edição/Coleção):** 
  Representa as expansões lançadas oficialmente (ex: *Base Set*, *Scarlet & Violet*).
  *Atributos principais:* ID, nome da edição, símbolo, quantidade de cartas na coleção e data de lançamento.

- **Deck (Baralho):** 
  Representa a lista de cartas montada pelo usuário para jogar.
  *Atributos principais:* ID, nome do deck, data de criação e a lista das cartas contidas nele (respeitando o limite do jogo de 60 cartas no total e máximo de 4 cópias da mesma carta, exceto energias básicas).

- **DeckCard (Carta do Baralho):** 
  Uma entidade de ligação necessária para o banco de dados e lógica do sistema. Em vez de salvar a carta inteira várias vezes dentro do deck, essa entidade salva apenas qual é a carta e a quantidade dela naquele deck específico.
  *Atributos principais:* ID da Carta e Quantidade (ex: 3x Pikachu).

# 5. Decisão de Stack: Kotlin com Spring boot
Decidir seguir o projeto com Kotlin, pois ja fiz projetos anteriormente com java e queria ter uma experiência nova para aumentar meu repertório, alem de ser uma linguagem mais moderna e que possui interoperabilidade com o próprio Java, somado a semelhanças que facilitam o aprendizado e melhorias que facilitam o uso e escrita.

# 6. Divisão de Responsabilidades (Serviço Principal vs Go)
| Vai para o serviço principal (Ktor) | Vai para um microsserviço Go |
| :--- | :--- |
| Receber as requisições HTTP do usuário (API REST) | Conectar com APIs externas (ex: Pokémon TCG API) para buscar as cartas |
| Gerenciar a criação, edição e exclusão de Decks | Processar e filtrar grandes volumes de dados brutos das cartas rapidamente |
| Salvar e organizar a coleção de cartas | Executar buscas pesadas de cartas por nome, ID ou edição |
| Validar regras do sistema (ex: limite de cartas no deck) | Formatar e estruturar os dados para exportação de listas de decks |
| Atuar como o "Gateway" que conversa com o front-end | Retornar apenas os dados limpos e necessários para o Kotlin via gRPC |


# 7. Equipe
- Gabriel Victor de Lima Pimentel
  - Mat.: 20260001820

# 8. Vídeo de Apresentação

- Link:
