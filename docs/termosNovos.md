# O que aprendi até agora (Com e sem IA)
Coisas que aprendi tanto pesquisando quanto com o uso de IA (Claude)
## estrutura
No API-Ktor eu comecei fazendo as regras e moldes das classes, mas não as criando em sí, isso será feito depois com o banco de dados.
## Termos
### @JvmInline value class
Basicamente previne que erros de compilação aconteçam em casos de funções que usam mais de um tipo "String", alem de otimizar o codigo pois ele faz o uso sem custo em tempo de execução
### companion object
tipo o Static, porem deixa mais obvio a chamada de construção da classe, no meu caso estou usando como "novo" então em vez de ItemColecaoId(UUID.randomUUID()), eu uso ItemColecaoId.novo() pois novo() = ItemColecaoId(UUID.randomUUID())
### Suspend
serve para dizer uma função que pode pausar sem bugar tudo, tipo se ela está rodando mas depende de outro processor finalizar para ela terminar, ela entra em pausa e espera o processo dependente finalizar. (como uma consulta no banco de dados por exemplo)
### copy(estado = novo)
basicamente ele copia o objeto e troca apenas a veriavel que eu quero, ou seja, se eu quiser alterar algum campo, em vez de criar um objeto do zero repetindo os campos que não mudam e salvando esse objeto novo, esse copy() (que é do kotlin) ele copia o objeto mas muda o campo que eu passei no argumento.