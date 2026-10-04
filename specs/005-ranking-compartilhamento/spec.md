# 005 — Ranking e compartilhamento

## Objetivo

Exibir rankings pessoais de mídias concluídas e permitir compartilhar listas por links públicos somente para leitura.

## Requisitos relacionados

- RF36 — Ranking pessoal por tipo; RF37 — Critério de participação no ranking; RF38 — Compartilhar lista; RF39 — Visualização pública somente leitura; RF40 — Lista compartilhada dinâmica; RF41 — Link sem expiração; RF42 — Ativar e desativar compartilhamento.
- RN16 — Apenas concluídas e avaliadas no ranking; RN17 — Compartilhamento somente leitura; RN18 — Lista compartilhada reflete estado atual; RN19 — Link sem expiração; RN21 — Conteúdo público sem dados privados; RN27 — Ordenação do ranking; RN28 — No máximo um compartilhamento por lista.
- UC09 — Compartilhar lista; UC10 — Visualizar lista compartilhada.

## Cenários de aceitação

### Cenário 1 — Ranking inclui somente mídia concluída e avaliada

**Dado** que o usuário possui mídias de tipos diferentes com estados e notas variados  
**Quando** consulta um ranking por tipo  
**Então** a resposta inclui somente mídias próprias desse tipo com status `DONE` e nota.

### Cenário 2 — Ordenação e posição paginada

**Dado** que há itens concluídos com nota no ranking  
**Quando** o usuário consulta páginas consecutivas  
**Então** os itens ordenam por nota decrescente e título crescente nos empates, e a posição considera o índice global na paginação.

### Cenário 3 — Ativar compartilhamento

**Dado** que uma lista própria ainda não tem compartilhamento  
**Quando** o proprietário ativa o compartilhamento  
**Então** a API cria um token público e devolve o link.

### Cenário 4 — Reativar mantém o mesmo token

**Dado** que a lista tem compartilhamento previamente desativado  
**Quando** o proprietário o ativa novamente  
**Então** o mesmo token é reutilizado.

### Cenário 5 — Consultar link público

**Dado** que o compartilhamento está ativo  
**Quando** qualquer visitante consulta `GET /api/v1/public/lists/{token}`  
**Então** recebe o nome e os dados paginados das mídias atuais, sem dados do proprietário, status ou nota pessoal.

### Cenário 6 — Conteúdo compartilhado é dinâmico

**Dado** que o proprietário altera os itens da lista compartilhada  
**Quando** um visitante abre novamente o mesmo link  
**Então** vê o conteúdo atual da lista.

### Cenário 7 — Token ausente ou compartilhamento desativado

**Dado** um token inexistente ou de um compartilhamento inativo  
**Quando** alguém consulta o link  
**Então** a resposta é o mesmo 404 genérico `RESOURCE_NOT_FOUND` nos dois casos.

### Cenário 8 — Recursos privados da lista compartilhada

**Dado** que um visitante acessa uma lista pública  
**Quando** consulta seu conteúdo  
**Então** o DTO não expõe proprietário, status nem avaliação pessoal.

## Rastreabilidade

| Cenário | Requisitos | Teste (a criar/ajustar) |
|---|---|---|
| 1 | RF36, RF37, RN16 | `cenario1_rankingIncluiConcluidasAvaliadasDoTipo` |
| 2 | RF37, RN27 | `cenario2_rankingOrdenaECalculaPosicaoGlobal` |
| 3 | RF38, RN28, UC09 | `cenario3_ativarCompartilhamentoGeraLink` |
| 4 | RF42, RN28, UC09 | `cenario4_reativarCompartilhamentoMantemToken` |
| 5 | RF39, RF41, RN17, RN19, RN21, UC10 | `cenario5_linkPublicoExibeSomenteListaAtual` |
| 6 | RF40, RN18 | `cenario6_linkRefleteAlteracoesDaLista` |
| 7 | RF42, UC10 | `cenario7_tokenInvalidoEInativoRetornamMesmo404` |
| 8 | RF39, RN21 | `cenario8_dtoPublicoNaoExpoeDadosPrivados` |

## Decisões pendentes

- `[A DEFINIR]` Formato e política de segurança do token; comportamento ao excluir a lista e estrutura da representação pública (seção 12.2 de `docs/requisitos.md`).
- A regra de identidade autenticada a partir do token está definida em RF03/RNF02; o modelo definitivo de autorização listado como pendente em 12.2 precisa ser delimitado sem contradizer essa regra.
- `[A DEFINIR]` Política e prazo de cache de metadados externos, conforme os termos do TMDB (seção 12.2).
- O checklist sugere token de 32 bytes aleatórios com Base64 URL-safe; o formato foi uma sugestão técnica, não uma decisão funcional confirmada.

## Fora do escopo

- Expiração automática do link na v1.
- Cópia estática da lista compartilhada.
- Avaliação global ou ranking entre usuários.
- Limite de quantidade de links compartilhados.
