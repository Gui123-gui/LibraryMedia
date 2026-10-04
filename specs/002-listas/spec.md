# 002 — Listas

## Objetivo

Permitir ao usuário criar e organizar listas próprias, incluindo a lista especial Favoritos.

## Requisitos relacionados

- RF04 — Criar lista; RF05 — Listar listas; RF06 — Alterar lista; RF07 — Excluir lista; RF08 — Lista Favoritos automática; RF09 — Ação em lista vazia; RF10 — Capa da lista.
- RN03 — Favoritos única por usuário; RN04 — Favoritos não excluível; RN24 — Confirmação ao excluir lista com mídias exclusivas; RN25 — Favoritos não renomeável; RN26 — Nome único por usuário sem diferenciar maiúsculas.
- UC07 — Criar lista; UC11 — Excluir lista.

## Cenários de aceitação

### Cenário 1 — Listar somente as listas do usuário

**Dado** que há listas de dois usuários  
**Quando** um usuário autenticado chama `GET /api/v1/lists`  
**Então** a resposta 200 contém somente suas listas, com Favoritos identificada.

### Cenário 2 — Criar lista vazia

**Dado** que o nome tem de 1 a 100 caracteres e ainda não existe para o usuário  
**Quando** o usuário chama `POST /api/v1/lists`  
**Então** a resposta é 201 e a nova lista pertence ao usuário e pode estar vazia.

### Cenário 3 — Nome repetido ignora maiúsculas

**Dado** que o usuário possui uma lista com determinado nome  
**Quando** cria ou renomeia outra lista para o mesmo nome com caixa diferente  
**Então** a resposta é 409 `LIST_NAME_ALREADY_EXISTS` e o estado não muda.

### Cenário 4 — Obter ou alterar lista alheia

**Dado** que a lista pertence a outro usuário  
**Quando** o usuário tenta consultar, renomear ou excluir essa lista  
**Então** a resposta é 404 `RESOURCE_NOT_FOUND`.

### Cenário 5 — Favoritos protegida

**Dado** que o usuário possui sua lista Favoritos  
**Quando** tenta renomeá-la ou excluí-la  
**Então** a resposta é 409 `FAVORITES_LIST_PROTECTED` e a lista permanece.

### Cenário 6 — Excluir lista sem mídia exclusiva

**Dado** que a lista não tem mídias ou todas também pertencem a outras listas do usuário  
**Quando** o usuário a exclui  
**Então** a exclusão é concluída sem remover mídias que permanecem associadas.

### Cenário 7 — Excluir lista com mídias exclusivas exige confirmação

**Dado** que uma ou mais mídias pertencem somente à lista  
**Quando** o usuário chama `DELETE /api/v1/lists/{listId}` sem `confirm=true`  
**Então** a resposta é 409 `LIST_HAS_EXCLUSIVE_MEDIA`, informa `affectedMediaCount` e nada é apagado.

### Cenário 8 — Confirmar exclusão da lista

**Dado** que a lista tem mídias exclusivas  
**Quando** o usuário repete a exclusão com `confirm=true`  
**Então** a lista e seu compartilhamento são removidos, as mídias exclusivas saem da biblioteca e as demais permanecem.

### Cenário 9 — Lista vazia oferece ação de pesquisa

**Dado** que uma lista própria não contém itens  
**Quando** o usuário consulta seus itens  
**Então** a resposta paginada inclui `links.search` apontando para `/api/v1/media/search`.

## Rastreabilidade

| Cenário | Requisitos | Teste (a criar/ajustar) |
|---|---|---|
| 1 | RF05, RN02 | `cenario1_listaSomenteDoProprietario` |
| 2 | RF04, UC07 | `cenario2_criarListaVazia` |
| 3 | RF04, RF06, RN26 | `cenario3_nomeRepetidoSemDiferenciarCaixa` |
| 4 | RF03, RNF02 | `cenario4_listaAlheiaRetorna404` |
| 5 | RF06, RF07, RN04, RN25 | `cenario5_favoritosProtegida` |
| 6 | RF07, RN24 | `cenario6_excluirListaSemMidiaExclusiva` |
| 7 | RF07, RN24, UC11 | `cenario7_exclusaoExigeConfirmacao` |
| 8 | RF07, RN09, RN24, UC11 | `cenario8_confirmarExclusaoRemoveSomenteMidiasExclusivas` |
| 9 | RF09 | `cenario9_listaVaziaOfereceBusca` |

## Decisões pendentes

- `[A DEFINIR]` Arquitetura detalhada de backend e estratégia de testes (seção 12.2 de `docs/requisitos.md`); o código atual contém regra de aplicação diretamente em `ListController`, enquanto a constituição prevê camada Service.

## Fora do escopo

- Gerenciamento de status, avaliações e compartilhamento de mídias, especificados em outras funcionalidades.
- Decisões de apresentação visual além dos dados contratuais definidos.
