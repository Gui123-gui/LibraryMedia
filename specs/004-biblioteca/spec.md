# 004 — Biblioteca, status e avaliação

## Objetivo

Permitir que o usuário adicione mídias à biblioteca, associe-as a listas e acompanhe status e avaliação pessoal.

## Requisitos relacionados

- RF17 — Adicionar mídia; RF18 — Mídia em várias listas; RF19 — Sem duplicidade na lista; RF20 — Mídia sempre em uma lista; RF21 — Organizar depois; RF22 — Remover de uma lista; RF23 — Confirmação ao remover da última lista; RF24 — Remover da biblioteca; RF25 — Readicionar mídia; RF26 — Tela de todas as mídias.
- RF27 — Status de livro; RF28 — Status de filme/série; RF29 — Perguntar se já consumiu; RF30 — Alterar status; RF31 — Avaliar mídia; RF32 — Avaliação apenas para concluídas; RF33 — Avaliação obrigatória ao concluir na adição; RF34 — Alterar avaliação; RF35 — Preservar avaliação.
- RN05 — Mídia da biblioteca em pelo menos uma lista; RN06 — Mídia em várias listas; RN07 — Associação única mídia/lista; RN08 — Confirmação ao remover da última lista; RN09 — Remoção da biblioteca apaga avaliação; RN10 — Readicionar mídia; RN11 — Status conforme tipo; RN12 — Status individual; RN13 — Avaliação individual e condicionada a conclusão; RN14 — Alteração de status e avaliação; RN15 — Preservar nota ao sair de concluído; RN22 — Identidade única da mídia; RN23 — Série sem temporadas ou episódios.
- UC04 — Adicionar mídia; UC05 — Alterar status; UC06 — Avaliar mídia; UC08 — Remover mídia.

## Cenários de aceitação

### Cenário 1 — Adicionar mídia não consumida

**Dado** que a mídia existe no provedor e `listIds` contém ao menos uma lista do usuário  
**Quando** o usuário adiciona a mídia com `consumed=false`  
**Então** a API responde 201, cria a entrada com status `WANT` e associa a todas as listas indicadas.

### Cenário 2 — Adicionar mídia já consumida exige nota

**Dado** que o usuário envia `consumed=true` sem nota  
**Quando** adiciona uma mídia  
**Então** a API responde 400 `RATING_REQUIRED` e não cria a entrada.

### Cenário 3 — Nota válida ao adicionar como concluída

**Dado** que `consumed=true` e a nota está entre 1 e 10  
**Quando** o usuário adiciona a mídia  
**Então** a entrada é criada com status `DONE` e a nota informada.

### Cenário 4 — Mídia existente não perde status nem nota

**Dado** que a mídia já está na biblioteca e algumas listas ainda não a contêm  
**Quando** o usuário a adiciona a essas listas  
**Então** a API responde 200, mantém status/nota existentes e informa `addedToLists` e `alreadyInLists`.

### Cenário 5 — Lista de outro usuário não pode ser usada

**Dado** que uma das listas indicadas pertence a outro usuário  
**Quando** o usuário tenta adicionar mídia à biblioteca nela  
**Então** a API responde 404 `RESOURCE_NOT_FOUND` e não grava a entrada nem as associações.

### Cenário 6 — Mídia não duplica na mesma lista

**Dado** que a entrada já está associada à lista  
**Quando** o usuário tenta adicioná-la novamente  
**Então** nenhuma segunda associação é criada e a resposta informa que já estava na lista.

### Cenário 7 — Listar e filtrar a biblioteca

**Dado** que o usuário tem mídias na biblioteca  
**Quando** chama `GET /api/v1/library` com filtros e ordenação permitidos  
**Então** a resposta paginada contém somente entradas próprias que correspondem aos filtros.

### Cenário 8 — Alterar status preserva nota

**Dado** que uma mídia concluída tem nota  
**Quando** o usuário muda seu status para não concluído  
**Então** a nota permanece armazenada e a mídia deixa de participar do ranking.

### Cenário 9 — Avaliação requer conclusão

**Dado** que a mídia não está em `DONE`  
**Quando** o usuário tenta definir ou alterar sua nota  
**Então** a API responde 409 `RATING_REQUIRES_COMPLETED`.

### Cenário 10 — Remover de lista quando há outra associação

**Dado** que a entrada pertence a mais de uma lista do usuário  
**Quando** o usuário a remove de uma das listas  
**Então** somente essa associação é removida e a entrada permanece na biblioteca.

### Cenário 11 — Remover da última lista exige confirmação

**Dado** que a entrada está somente naquela lista  
**Quando** o usuário remove a associação sem `confirm=true`  
**Então** a API responde 409 `LAST_LIST_CONFIRMATION_REQUIRED` e não remove nada.

### Cenário 12 — Confirmar remoção da última lista

**Dado** que a entrada está somente naquela lista  
**Quando** o usuário repete a remoção com `confirm=true`  
**Então** a associação, entrada da biblioteca e nota são removidas.

### Cenário 13 — Remover diretamente da biblioteca exige confirmação

**Dado** que uma entrada existe na biblioteca  
**Quando** o usuário chama `DELETE /api/v1/library/{entryId}` sem `confirm=true`  
**Então** a API responde 409 `LIBRARY_REMOVAL_CONFIRMATION_REQUIRED` e não apaga dados.

### Cenário 14 — Remoções concorrentes não deixam entrada órfã

**Dado** que a mesma mídia está em duas listas  
**Quando** duas requisições concorrentes tentam remover simultaneamente suas associações finais  
**Então** o resultado não deixa uma entrada da biblioteca sem nenhuma lista.

## Rastreabilidade

| Cenário | Requisitos | Teste (a criar/ajustar) |
|---|---|---|
| 1 | RF17, RF20, RF29, RN05, RN11, UC04 | `cenario1_adicionarNaoConsumida` |
| 2 | RF29, RF33, RN13, UC04 | `cenario2_consumidaExigeNota` |
| 3 | RF29, RF33, RN13, UC04 | `cenario3_adicionarConcluidaComNota` |
| 4 | RF18, RF21, RN06, RN12 | `cenario4_readicionarPreservaStatusENota` |
| 5 | RF03, RF17, RNF02 | `cenario5_listaAlheiaRetorna404` |
| 6 | RF19, RN07 | `cenario6_associacaoNaoDuplica` |
| 7 | RF26, RN02 | `cenario7_consultarBibliotecaComFiltros` |
| 8 | RF30, RF35, RN15, UC05 | `cenario8_mudarStatusPreservaNota` |
| 9 | RF31, RF32, RF34, RN13, UC06 | `cenario9_avaliarExigeConclusao` |
| 10 | RF22, RN06, UC08 | `cenario10_removerUmaListaPreservaBiblioteca` |
| 11 | RF23, RN08, UC08 | `cenario11_ultimaListaExigeConfirmacao` |
| 12 | RF23, RN09, UC08 | `cenario12_confirmarUltimaListaApagaEntradaENota` |
| 13 | RF24, RN09 | `cenario13_remocaoDaBibliotecaExigeConfirmacao` |
| 14 | RF20, RN05, RN08 | `cenario14_remocoesConcorrentesNaoOrfanamEntrada` |

## Decisões pendentes

- `[A DEFINIR]` Modelagem definitiva do banco, caso difira do DDL proposto; a seção 12.2 de `docs/requisitos.md` lista essa decisão, enquanto `docs/modelo-dominio-e-mer.md` apresenta um DDL candidato.
- `[A DEFINIR]` Arquitetura detalhada e estratégia de testes (seção 12.2). O código atual concentra casos de uso no `LibraryController`, em vez de usar um service dedicado.

## Fora do escopo

- Temporadas e episódios de séries na v1.
- Alterar dados de uma mídia externa como se fossem propriedades individuais do usuário.
