# Poketlin

Fichário web para registrar a sua coleção de cartas Pokémon TCG. A API busca cartas num catálogo externo (TCGdex) e guarda a coleção do usuário, com o estado de conservação de cada carta.

Projeto acadêmico, sprint 1.

## Equipe

| Nome | Matrícula |
| ---- | --------- |
| `<NOME 1>` | `<MATRÍCULA 1>` |
| `<NOME 2>` | `<MATRÍCULA 2>` |

**Coorte:** `<COORTE>`

## O que a sprint 1 entrega

| História | Como está implementada |
| -------- | ---------------------- |
| Buscar cartas por nome, id ou edição | `GET /cartas` |
| Navegar pelos resultados com scroll infinito | Paginação por cursor (`limite` e `cursor`, resposta com `proximoCursor`) |
| Adicionar cartas à minha coleção | `POST /colecao/itens` |
| Definir o estado de conservação ao adicionar (ex: M, NM, MP) | Campo `estado` do `POST /colecao/itens`, por nome ou sigla; `GET /colecao/estados` lista as opções |
| Complemento da coleção | Listar com filtros e paginação, consultar, alterar estado/quantidade e remover |

## Arquitetura

```
Cliente (web/mobile)
        │  HTTPS · REST · OpenAPI
        ▼
api-ktor (Kotlin + Ktor) ───── JDBC ─────► PostgreSQL
        │  gRPC
        ▼
worker (Go) ───── HTTPS ─────► TCGdex API
```

- **api-ktor**: API REST. Cliente gRPC do worker; não recebe chamadas gRPC.
- **worker**: serviço Go que consulta a TCGdex (`GetPokemonCard` e `SearchCards`).
- **PostgreSQL**: guarda a coleção. O esquema é criado só por migrações Flyway.

### Camadas do `api-ktor` (Clean Architecture)

```
api-ktor/src/main/kotlin/ufrn/br/poketlin/
├── domain/           entidades, regras e portas (Kotlin puro)
├── application/      casos de uso
├── infrastructure/   JDBC/Flyway e cliente gRPC do worker
├── presentation/     rotas, DTOs, validação, erros e OpenAPI
└── Application.kt    monta as camadas (único ponto que conhece todas)
```

Regra de dependência:

- `domain` não depende de nenhuma outra camada.
- `application` depende só de `domain`.
- `infrastructure` depende de `domain` e `application`.
- `presentation` depende de `domain` e `application`, nunca de `infrastructure`.

Um teste de arquitetura (`ArchitectureTest`, com Konsist) roda junto com os demais testes e falha se alguma dessas regras for quebrada, ou se `domain`/`application` importarem framework ou infraestrutura (Ktor, JDBC, Flyway, gRPC, serialização).

## Estrutura do repositório

```
.
├── api-ktor/            API REST (Kotlin + Ktor)
├── services/            worker em Go (gRPC → TCGdex)
├── protos/              contrato gRPC (tcg_data.proto)
├── docs/                proposta e especificação OpenAPI
├── docker-compose.yml   PostgreSQL para desenvolvimento local
├── mise.toml            tasks de build e teste
└── .github/workflows/   integração contínua
```

## Pré-requisitos

- Docker (no Windows, com a integração com o WSL ligada)
- JDK 21
- Go (versão indicada no `services/go.mod`)
- [mise](https://mise.jdx.dev/) para as tasks `build` e `test`
- [buf](https://buf.build/docs/installation), só se for regenerar o código gRPC do worker

## Como rodar

Use três terminais, a partir da raiz do repositório.

**1. Banco de dados**

```bash
docker compose up -d postgres
```

**2. Worker (porta 50051)**

```bash
cd services
go run .
```

**3. API (porta 8080)**

```bash
cd api-ktor
./gradlew run
```

Na subida, a API aplica as migrações Flyway automaticamente. Para conferir:

```bash
curl -i localhost:8080/health
curl -s "localhost:8080/cartas?nome=pikachu&limite=3"
```

A documentação interativa fica em <http://localhost:8080/swagger>, e a especificação em <http://localhost:8080/api.json> (cópia versionada em [`docs/openapi.json`](docs/openapi.json)).

### Variáveis de ambiente (API)

| Variável | Padrão | Uso |
| -------- | ------ | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/poketlin` | Conexão com o PostgreSQL |
| `DATABASE_USER` | `poketlin` | Usuário do banco |
| `DATABASE_PASSWORD` | `poketlin` | Senha do banco |
| `WORKER_ADDR` | `localhost:50051` | Endereço do worker gRPC |

## Endpoints

| Método e rota | O que faz | Respostas |
| ------------- | --------- | --------- |
| `GET /health` | Verifica se a API está de pé | 200 |
| `GET /cartas` | Busca cartas no catálogo. Parâmetros: `nome`, `id`, `edicao`, `limite` (1 a 100, padrão 20) e `cursor`. Exige ao menos um entre `nome`, `id` e `edicao` | 200, 400, 503 |
| `POST /colecao/itens` | Adiciona uma carta à coleção, com estado de conservação (nome ou sigla) e quantidade | 201 (com `Location`), 400, 404, 409 |
| `GET /colecao/itens` | Lista a coleção. Filtros: `nome`, `edicao`, `estado`; paginação: `limite` e `cursor` | 200, 400 |
| `GET /colecao/itens/{id}` | Consulta um item | 200, 400, 404 |
| `PATCH /colecao/itens/{id}` | Altera `estado` e/ou `quantidade` | 200, 400, 404, 409 |
| `DELETE /colecao/itens/{id}` | Remove um item | 204, 400, 404 |
| `GET /colecao/estados` | Lista os estados de conservação aceitos (código, sigla e descrição) | 200 |

Estados de conservação: use o nome ou a sigla, sem diferenciar maiúsculas de minúsculas. A lista também sai em `GET /colecao/estados`.

| Sigla | Nome | Descrição |
| ----- | ---- | --------- |
| `M` | `MINT` | Mint |
| `NM` | `NEAR_MINT` | Near Mint |
| `LP` | `LIGHTLY_PLAYED` | Lightly Played |
| `MP` | `MODERATELY_PLAYED` | Moderately Played |
| `HP` | `HEAVILY_PLAYED` | Heavily Played |
| `DMG` | `DAMAGED` | Damaged |

A mesma carta não pode estar na coleção duas vezes no mesmo estado (409).

### Scroll infinito

As listas devolvem `itens` e `proximoCursor`. Para carregar a página seguinte, envie o `proximoCursor` recebido no parâmetro `cursor`. Quando ele vier `null`, acabaram os resultados. O cursor é opaco para o cliente.

```bash
curl -s "localhost:8080/cartas?nome=pikachu&limite=3"
curl -s "localhost:8080/cartas?nome=pikachu&limite=3&cursor=2"
```

### Adicionar uma carta à coleção

```bash
curl -i -X POST localhost:8080/colecao/itens \
  -H 'Content-Type: application/json' \
  -d '{"cartaId":"base1-4","estado":"NM","quantidade":1}'
```

O estado aceita o nome (`NEAR_MINT`) ou a sigla (`NM`), em qualquer caixa. A resposta devolve os dois:

```json
{
  "id": "5a3e6bff-244b-4409-8d6f-25c637bb63e2",
  "carta": {
    "id": "base1-4",
    "nome": "Charizard",
    "edicao": "Base Set",
    "imagemUrl": "https://assets.tcgdex.net/en/base/base1/4/high.png"
  },
  "estado": "NEAR_MINT",
  "estadoSigla": "NM",
  "quantidade": 1,
  "adicionadoEm": "2026-10-16T21:51:34.019069Z"
}
```

### Erros (RFC 9457)

Todos os erros saem como `application/problem+json`. Erros de validação listam cada campo com problema:

```json
{
  "type": "about:blank",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Corrija os campos indicados em 'errors'",
  "instance": "/colecao/itens",
  "errors": [
    { "campo": "cartaId", "mensagem": "campo obrigatório" },
    { "campo": "quantidade", "mensagem": "deve ser pelo menos 1" }
  ]
}
```

Se o worker ou a TCGdex estiverem fora do ar, a busca responde 503.

## Banco de dados e migrações

- As migrações ficam em `api-ktor/src/main/resources/db/migration` (`V1__...`, `V2__...`) e são aplicadas pelo Flyway quando a API sobe.
- Não há DDL automático: o esquema só muda por migração nova. Nunca edite uma migração já aplicada.
- Tabelas: `carta` e `item_colecao` (com chave estrangeira para `carta`). A restrição `UNIQUE (carta_id, estado)` sustenta o erro 409, e `CHECK (quantidade >= 1)` espelha a regra do domínio.

## Testes

Suba o banco antes, porque o teste do repositório JDBC usa o PostgreSQL do compose:

```bash
docker compose up -d postgres
cd api-ktor
./gradlew test
```

O que é coberto:

- regras do domínio (unitários);
- teste de arquitetura das camadas (`ArchitectureTest`);
- rotas e contrato HTTP, com repositório e catálogo em memória (inclui validação por campo e a especificação OpenAPI);
- repositório JDBC contra o PostgreSQL real, com as migrações.

Worker Go:

```bash
cd services
go test ./...
```

Com o mise, as tasks `build` e `test` rodam o equivalente (`mise run build` e `mise run test`).

## Contrato gRPC

O contrato fica em `protos/tcg_data.proto`.

- **Kotlin:** o Gradle gera os stubs automaticamente a partir de `../protos` (não há código gerado versionado).
- **Go:** o código gerado fica em `services/pb` e é versionado. Para regenerar depois de mudar o `.proto`:

```bash
cd services
buf generate ../protos
```

## Integração contínua

O workflow em `.github/workflows` roda em push e em pull request: compila e testa a API Kotlin (com um PostgreSQL como service container) e compila o worker Go. A `main` deve ficar verde.

## Documentação

- [`docs/proposta.md`](docs/proposta.md): proposta do projeto
- [`docs/openapi.json`](docs/openapi.json): especificação OpenAPI gerada pela API