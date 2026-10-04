# 🔌 Personal Media Library — Endpoints e Contratos da API

> Baseado em `requisitos.md` e `modelo-dominio-e-mer.md`.
> Os exemplos de JSON são **ilustrativos** (o provedor externo ainda não foi escolhido).
> As propostas de autenticação, senha e paginação foram aceitas (seção 10). Permanece em aberto apenas o que depende da escolha da API externa de mídias.

---

## 1. Convenções gerais

| Tema | Convenção |
|---|---|
| **Base URL** | `/api/v1` |
| **Formato** | JSON (`application/json`), UTF-8 |
| **Autenticação** | `Authorization: Bearer <token>` (JWT) |
| **Identidade do usuário** | Vem **sempre do token**, nunca da URL nem do corpo (RF03, RNF02) |
| **Isolamento** | Recurso de outro usuário responde **404**, nunca 403, para não revelar que existe |
| **Datas** | ISO 8601 em UTC (`2026-09-29T14:30:00Z`) |
| **IDs** | Numéricos, expostos como `id` |
| **Nomes de campos** | `camelCase` |
| **Status HTTP** | 200/201/204 sucesso · 400 validação · 401 não autenticado · 404 não encontrado · 409 conflito de regra de negócio · 429 excesso de requisições · 502/503 provedor externo · 500 erro interno |
| **Documentação** | OpenAPI/Swagger gerado (springdoc), agrupado por tags |

### Endpoints públicos (sem token)

- `POST /auth/register`
- `POST /auth/login`
- `GET /public/lists/{token}`

Todos os demais exigem autenticação.

---

## 2. Visão geral dos endpoints

| Área | Método e caminho | Descrição | Requisitos |
|---|---|---|---|
| **Auth** | `POST /auth/register` | Criar conta (cria Favoritos) | RF01, RF08, UC01 |
| | `POST /auth/login` | Autenticar | RF02, UC02 |
| | `GET /users/me` | Dados do usuário autenticado | RF03 |
| **Pesquisa** | `GET /media/search` | Busca unificada, paginada e filtrada | RF11, RF13, RF14, UC03 |
| | `GET /media/suggestions` | Autocomplete | RF12 |
| | `GET /media/{provider}/{externalId}` | Detalhes de uma mídia | RF16 |
| **Listas** | `GET /lists` | Listar minhas listas | RF05 |
| | `POST /lists` | Criar lista | RF04, UC07 |
| | `GET /lists/{listId}` | Detalhes de uma lista | RF05 |
| | `PATCH /lists/{listId}` | Renomear | RF06 |
| | `DELETE /lists/{listId}` | Excluir (com confirmação) | RF07, UC11 |
| **Itens da lista** | `GET /lists/{listId}/items` | Mídias da lista (paginado) | RF09, RF26 |
| | `POST /lists/{listId}/items` | Adicionar mídia da biblioteca à lista | RF21 |
| | `DELETE /lists/{listId}/items/{entryId}` | Remover da lista (confirmação se for a última) | RF22, RF23, UC08 |
| **Biblioteca** | `GET /library` | Todas as mídias (paginado, filtros) | RF26 |
| | `POST /library` | Adicionar mídia à biblioteca | RF17 a RF19, RF29, RF33, UC04 |
| | `GET /library/{entryId}` | Detalhe de uma mídia da biblioteca | RF26 |
| | `PUT /library/{entryId}/status` | Alterar status | RF30, UC05 |
| | `PUT /library/{entryId}/rating` | Avaliar / alterar nota | RF31 a RF35, UC06 |
| | `DELETE /library/{entryId}` | Remover da biblioteca (com confirmação) | RF24, RF25 |
| **Ranking** | `GET /rankings/{type}` | Ranking pessoal por tipo | RF36, RF37 |
| **Compartilhamento** | `GET /lists/{listId}/share` | Estado do compartilhamento | RF38, RF42 |
| | `PUT /lists/{listId}/share` | Ativar ou desativar | RF38, RF41, RF42, UC09 |
| | `GET /public/lists/{token}` | Visualização pública | RF39, RF40, UC10 |

> `entryId` é o id da `UserMedia` (mídia **na biblioteca do usuário**). Não confundir com o id da `Media`.

---

## 3. Formatos comuns

### 3.1 Paginação

Parâmetros: `page` (começa em **0**) e `size`.

| Parâmetro | Padrão | Máximo |
|---|---|---|
| `size` (listas, biblioteca, ranking, público) | 20 | 50 |
| `size` (pesquisa) | 20 | 20 |

Resposta:

```json
{
  "content": [ ],
  "page": 0,
  "size": 20,
  "totalElements": 134,
  "totalPages": 7
}
```

### 3.2 Mídia resumida (`MediaSummary`)

```json
{
  "type": "BOOK",
  "provider": "openlibrary",
  "externalId": "OL893415W",
  "title": "Duna",
  "year": 1965,
  "genre": "Ficção científica",
  "coverUrl": "https://exemplo.com/capas/duna.jpg",
  "externalRating": 8.4
}
```

`type` é `BOOK`, `MOVIE` ou `SERIES`. Campos como `year`, `genre`, `coverUrl` e `externalRating` podem vir `null`, conforme a disponibilidade da fonte.

### 3.3 Item da biblioteca (`LibraryEntry`)

```json
{
  "id": 15,
  "media": {
    "type": "BOOK",
    "provider": "openlibrary",
    "externalId": "OL893415W",
    "title": "Duna",
    "year": 1965,
    "genre": "Ficção científica",
    "coverUrl": "https://exemplo.com/capas/duna.jpg",
    "synopsis": "…",
    "externalRating": 8.4
  },
  "status": "DONE",
  "statusLabel": "LIDO",
  "rating": 9,
  "lists": [
    { "id": 1, "name": "Favoritos" },
    { "id": 4, "name": "Ficção Científica" }
  ],
  "addedAt": "2026-09-29T14:30:00Z"
}
```

- `status`: `WANT`, `IN_PROGRESS` ou `DONE`;
- `statusLabel`: rótulo conforme o tipo (`QUERO_LER`, `LENDO`, `LIDO`, `QUERO_ASSISTIR`, `ASSISTINDO`, `ASSISTIDO`);
- `rating`: inteiro de 1 a 10 ou `null`. Pode existir mesmo com `status` diferente de `DONE` (a nota é preservada, RN15).

### 3.4 Lista (`ListSummary`)

```json
{
  "id": 4,
  "name": "Ficção Científica",
  "favorites": false,
  "mediaCount": 12,
  "covers": [
    "https://exemplo.com/capas/1.jpg",
    "https://exemplo.com/capas/2.jpg",
    "https://exemplo.com/capas/3.jpg",
    "https://exemplo.com/capas/4.jpg"
  ],
  "shared": true
}
```

`covers` traz até 4 capas para o front montar o mosaico (RF10). `shared` indica se o compartilhamento está ativo.

### 3.5 Erros (RFC 9457 / `ProblemDetail`)

Todo erro segue o mesmo formato (RNF07):

```json
{
  "type": "/errors/last-list-confirmation-required",
  "title": "Confirmação necessária",
  "status": 409,
  "detail": "Esta é a última lista desta mídia. Se continuar, a mídia será removida da sua biblioteca.",
  "instance": "/api/v1/lists/4/items/15",
  "code": "LAST_LIST_CONFIRMATION_REQUIRED",
  "timestamp": "2026-09-29T14:30:00Z"
}
```

Erros de validação acrescentam `errors`:

```json
{
  "type": "/errors/validation-error",
  "title": "Dados inválidos",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "errors": [
    { "field": "email", "message": "deve ser um e-mail válido" },
    { "field": "passwordConfirmation", "message": "deve ser igual à senha" }
  ]
}
```

**Catálogo de códigos:**

| Código | HTTP | Quando |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Entrada inválida (inclui nota fora de 1 a 10, lista de `listIds` vazia) |
| `RATING_REQUIRED` | 400 | Adicionou como já consumida (`consumed = true`) sem informar nota |
| `INVALID_CREDENTIALS` | 401 | E-mail ou senha incorretos |
| `UNAUTHORIZED` | 401 | Token ausente, inválido ou expirado |
| `RESOURCE_NOT_FOUND` | 404 | Recurso inexistente **ou de outro usuário** |
| `MEDIA_NOT_FOUND` | 404 | Mídia não encontrada no provedor externo |
| `EMAIL_ALREADY_REGISTERED` | 409 | E-mail já cadastrado (RN01) |
| `LIST_NAME_ALREADY_EXISTS` | 409 | Nome de lista repetido para o usuário (RN26) |
| `FAVORITES_LIST_PROTECTED` | 409 | Tentativa de excluir ou renomear Favoritos (RN04, RN25) |
| `LAST_LIST_CONFIRMATION_REQUIRED` | 409 | Remover da última lista sem `confirm=true` (RN08) |
| `LIST_HAS_EXCLUSIVE_MEDIA` | 409 | Excluir lista com mídias exclusivas sem `confirm=true` (RN24); inclui `affectedMediaCount` |
| `LIBRARY_REMOVAL_CONFIRMATION_REQUIRED` | 409 | Remover da biblioteca sem `confirm=true` (RF24) |
| `RATING_REQUIRES_COMPLETED` | 409 | Avaliar mídia que não está concluída (RN13) |
| `TOO_MANY_REQUESTS` | 429 | Limite de requisições excedido |
| `EXTERNAL_PROVIDER_UNAVAILABLE` | 503 | Provedor externo fora do ar ou com timeout |
| `INTERNAL_ERROR` | 500 | Erro inesperado (sem detalhes internos na resposta) |

### 3.6 Confirmação em operações destrutivas

Três operações exigem confirmação (RF23, RF24, RN24). O padrão é o mesmo:

1. O cliente chama `DELETE` **sem** `confirm`;
2. A API responde **409** com o código específico e uma mensagem pronta para exibir;
3. Se o usuário confirmar, o cliente repete a chamada com `?confirm=true`.

A confirmação é **imposta pela API**, não só pelo front: sem `confirm=true`, nada é apagado.

---

## 4. Auth e usuário

### `POST /auth/register`

```json
{
  "name": "Maria Silva",
  "email": "maria@exemplo.com",
  "password": "senhaSegura123",
  "passwordConfirmation": "senhaSegura123"
}
```

**201 Created**

```json
{ "id": 1, "name": "Maria Silva", "email": "maria@exemplo.com" }
```

- Cria o usuário e a lista **Favoritos** na mesma transação (UC01);
- Validações: nome de 1 a 100 caracteres, e-mail válido e normalizado, senha de 8 a 72 caracteres, confirmação igual;
- Erros: `VALIDATION_ERROR` (400), `EMAIL_ALREADY_REGISTERED` (409).

### `POST /auth/login`

```json
{ "email": "maria@exemplo.com", "password": "senhaSegura123" }
```

**200 OK**

```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

- Erro: `INVALID_CREDENTIALS` (401), com mensagem genérica (não revela se o e-mail existe);
- Tempo de vida do token e refresh: (seção 10).

### `GET /users/me`

**200 OK** → `{ "id": 1, "name": "Maria Silva", "email": "maria@exemplo.com" }`

---

## 5. Pesquisa de mídias

A pesquisa consulta o provedor externo e **não grava nada** localmente. A mídia só é persistida quando o usuário a adiciona à biblioteca.

**Provedores** (identificados no campo `provider`):

| `provider` | Fonte | Tipos |
|---|---|---|
| `tmdb` | TMDB | `MOVIE`, `SERIES` |
| `openlibrary` | Open Library | `BOOK` |

- Com `type` informado, consulta **só** o provedor correspondente (`BOOK` → Open Library; `MOVIE` e `SERIES` → TMDB);
- Sem `type`, consulta os dois **em paralelo** e intercala os resultados;
- Se um dos provedores falhar numa busca sem `type`, a API devolve os resultados do outro (o que evita que uma indisponibilidade derrube a pesquisa inteira);
- `type` e `year` são aplicados pelo próprio provedor; `genre` e `minRating` são aplicados sobre os resultados da página (melhor esforço), então a página pode vir com menos itens que `size`. `totalElements` e `totalPages` refletem a busca do provedor sem esses dois filtros.

### `GET /media/search`

| Parâmetro | Obrigatório | Descrição |
|---|---|---|
| `q` | sim | Texto da busca (2 a 100 caracteres) |
| `page`, `size` | não | Paginação (seção 3.1) |
| `type` | não | `BOOK`, `MOVIE` ou `SERIES` |
| `year` | não | Ano de lançamento |
| `genre` | não | Gênero |
| `minRating` | não | Avaliação externa mínima |

**200 OK**

```json
{
  "content": [
    {
      "type": "BOOK",
      "provider": "openlibrary",
      "externalId": "OL893415W",
      "title": "Duna",
      "year": 1965,
      "genre": "Ficção científica",
      "coverUrl": "https://exemplo.com/capas/duna.jpg",
      "externalRating": 8.4,
      "inLibrary": true,
      "entryId": 15
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

- `inLibrary` e `entryId` indicam se a mídia já está na biblioteca do usuário, para o front evitar adição duplicada;
- **Sem resultados = 200 com `content: []` e `totalElements: 0`.** A tela 404/cartoon (RF15) é responsabilidade do front, porque para a API "nenhum resultado" é uma resposta válida, não um erro;
- Erros: `VALIDATION_ERROR`, `EXTERNAL_PROVIDER_UNAVAILABLE`.

### `GET /media/suggestions`

Autocomplete leve, com no máximo 8 sugestões.

| Parâmetro | Descrição |
|---|---|
| `q` | Texto parcial (mínimo 2 caracteres) |

**200 OK**

```json
[
  { "type": "BOOK", "provider": "openlibrary", "externalId": "OL893415W", "title": "Duna", "year": 1965 },
  { "type": "MOVIE", "provider": "tmdb", "externalId": "693134", "title": "Duna: Parte Dois", "year": 2024 }
]
```

O front deve aplicar **debounce** (cerca de 300 ms) antes de chamar.

### `GET /media/{provider}/{externalId}`

Detalhes completos (RF16), incluindo `synopsis`. Mesmo formato de `MediaSummary` mais `synopsis`, `inLibrary` e `entryId`.

Erros: `MEDIA_NOT_FOUND` (404), `EXTERNAL_PROVIDER_UNAVAILABLE` (503).

---

## 6. Listas e itens da lista

### `GET /lists`

**200 OK** → array de `ListSummary`. Favoritos vem sempre incluída (`"favorites": true`). Sem paginação, porque o número de listas por usuário tende a ser pequeno.

### `POST /lists`

```json
{ "name": "Livros para 2027" }
```

**201 Created**, com `Location: /api/v1/lists/9` → `ListSummary`.

- Nome de 1 a 100 caracteres, único por usuário sem diferenciar maiúsculas;
- Erros: `VALIDATION_ERROR`, `LIST_NAME_ALREADY_EXISTS`.

### `GET /lists/{listId}`

**200 OK** → `ListSummary`. Erro: `RESOURCE_NOT_FOUND`.

### `PATCH /lists/{listId}`

```json
{ "name": "Livros para 2028" }
```

**200 OK** → `ListSummary`.

Erros: `LIST_NAME_ALREADY_EXISTS`, `FAVORITES_LIST_PROTECTED` (renomear Favoritos), `VALIDATION_ERROR`.

### `DELETE /lists/{listId}`

Parâmetro opcional: `confirm=true`.

- **204 No Content** quando a lista foi excluída;
- Se a lista tiver mídias **exclusivas** (que não estão em nenhuma outra lista) e faltar `confirm=true`, responde **409**:

```json
{
  "type": "/errors/list-has-exclusive-media",
  "title": "Confirmação necessária",
  "status": 409,
  "detail": "3 mídias pertencem somente a esta lista e serão removidas da sua biblioteca.",
  "code": "LIST_HAS_EXCLUSIVE_MEDIA",
  "affectedMediaCount": 3
}
```

- Com `confirm=true`, as mídias exclusivas saem da biblioteca (com a nota) e o compartilhamento da lista deixa de existir;
- Erros: `FAVORITES_LIST_PROTECTED`, `RESOURCE_NOT_FOUND`.

### `GET /lists/{listId}/items`

Mídias da lista, paginadas.

| Parâmetro | Descrição |
|---|---|
| `page`, `size` | Paginação |
| `type` | Filtro por tipo |
| `status` | Filtro por status (`WANT`, `IN_PROGRESS`, `DONE`) |

**200 OK** → página de `LibraryEntry`.

Se a lista estiver vazia, a resposta inclui um atalho para o próximo passo (RF09), no espírito de HATEOAS:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "links": { "search": "/api/v1/media/search" }
}
```

### `POST /lists/{listId}/items`

Adiciona à lista uma mídia **que já está na biblioteca** (RF21).

```json
{ "entryId": 15 }
```

- **201 Created** quando a associação foi criada;
- **200 OK** com `{ "alreadyInList": true }` quando a mídia já estava na lista (RF19: apenas informa, não duplica);
- Erros: `RESOURCE_NOT_FOUND` (lista ou item inexistente ou de outro usuário).

### `DELETE /lists/{listId}/items/{entryId}`

Parâmetro opcional: `confirm=true`.

- **204** quando removeu apenas da lista (a mídia continua em outras listas);
- Se for a **última lista** da mídia e faltar `confirm=true`, responde **409** com `LAST_LIST_CONFIRMATION_REQUIRED`;
- Com `confirm=true`, remove a mídia da biblioteca (e a nota junto, RN09);
- Erros: `RESOURCE_NOT_FOUND`.

---

## 7. Biblioteca

### `POST /library`

Fluxo principal de adição (UC04).

```json
{
  "provider": "openlibrary",
  "externalId": "OL893415W",
  "listIds": [1, 4],
  "consumed": true,
  "rating": 9
}
```

| Campo | Regra |
|---|---|
| `provider`, `externalId` | Identificam a mídia. O tipo **não** vem do cliente: é obtido do provedor |
| `listIds` | Pelo menos uma lista, todas do usuário (RN05) |
| `consumed` | `true` = "já leu/assistiu" (RF29) |
| `rating` | **Obrigatório** se `consumed = true` (RF33); ignorado/proibido se `false` |

**Regras aplicadas:**
- `consumed = true` → status `DONE` com a nota informada;
- `consumed = false` → status `WANT`, sem nota;
- Se a mídia **já estiver** na biblioteca, status e nota **não são alterados**; a chamada só adiciona às novas listas (RF21).

**Resposta:**
- **201 Created** (com `Location`) se a mídia entrou na biblioteca agora;
- **200 OK** se já estava e apenas foram adicionadas listas.

Corpo: `LibraryEntry`, mais `addedToLists` e `alreadyInLists` (ids das listas), para o front informar o usuário (RF19).

Erros: `VALIDATION_ERROR` (sem listas, nota inválida), `RATING_REQUIRED`, `RESOURCE_NOT_FOUND` (lista inexistente ou de outro usuário), `MEDIA_NOT_FOUND`, `EXTERNAL_PROVIDER_UNAVAILABLE`.

### `GET /library`

Todas as mídias do usuário (RF26).

| Parâmetro | Descrição |
|---|---|
| `page`, `size` | Paginação |
| `type` | Filtro por tipo |
| `status` | Filtro por status |
| `listId` | Somente mídias de uma lista |
| `q` | Busca por título dentro da biblioteca (opcional) |
| `sort` | `addedAt` (padrão, decrescente), `title`, `rating` |

**200 OK** → página de `LibraryEntry`.

### `GET /library/{entryId}`

**200 OK** → `LibraryEntry` completo (com sinopse e listas). Erro: `RESOURCE_NOT_FOUND`.

### `PUT /library/{entryId}/status`

```json
{ "status": "IN_PROGRESS" }
```

**200 OK** → `LibraryEntry`.

- Aceita qualquer transição (RF30);
- **Não altera a nota**: se sair de `DONE`, a nota fica guardada e a mídia sai do ranking (RN15);
- Se voltar a `DONE` e já houver nota, ela volta a contar no ranking;
- Erros: `VALIDATION_ERROR` (status desconhecido), `RESOURCE_NOT_FOUND`.

### `PUT /library/{entryId}/rating`

```json
{ "rating": 9 }
```

**200 OK** → `LibraryEntry`.

- Só permitido com `status = DONE` (RF32);
- Substitui a nota anterior (RF34);
- Erros: `VALIDATION_ERROR` (fora de 1 a 10), `RATING_REQUIRES_COMPLETED` (409), `RESOURCE_NOT_FOUND`.

### `DELETE /library/{entryId}`

Parâmetro: `confirm=true`.

- Sem `confirm=true`: **409** `LIBRARY_REMOVAL_CONFIRMATION_REQUIRED`;
- Com `confirm=true`: **204**. Remove a mídia de todas as listas, do ranking e apaga a nota (RN09);
- A mídia pode ser adicionada de novo depois (RF25).

---

## 8. Ranking

### `GET /rankings/{type}`

`type` ∈ `books`, `movies`, `series`.

| Parâmetro | Descrição |
|---|---|
| `page`, `size` | Paginação |

**200 OK**

```json
{
  "content": [
    { "position": 1, "entryId": 15, "media": { "type": "BOOK", "title": "Duna", "year": 1965, "coverUrl": "…" }, "rating": 10 },
    { "position": 2, "entryId": 22, "media": { "type": "BOOK", "title": "O Hobbit", "year": 1937, "coverUrl": "…" }, "rating": 9 }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

- Só entram mídias `DONE` com nota (RN12/RF37);
- Ordenação: nota decrescente e, em empate, título (RN27);
- `position` é calculada considerando a página (por exemplo, a primeira da página 1 com `size = 20` tem posição 21);
- `type` inválido → 400.

---

## 9. Compartilhamento

### `GET /lists/{listId}/share`

**200 OK**

```json
{ "shared": true, "url": "https://app.exemplo.com/shared/Xa9...", "token": "Xa9..." }
```

Se nunca foi compartilhada ou está desativada: `{ "shared": false }`.

### `PUT /lists/{listId}/share`

```json
{ "active": true }
```

**200 OK** → mesmo formato do `GET`.

- `active: true` cria o compartilhamento (com token novo) se não existir, ou reativa o existente **mantendo o mesmo token**;
- `active: false` desativa: o link deixa de funcionar (RF42);
- Operação idempotente; no máximo um link por lista (RN28);
- Sem expiração na v1 (RF41).

### `GET /public/lists/{token}` (sem autenticação)

| Parâmetro | Descrição |
|---|---|
| `page`, `size` | Paginação dos itens |

**200 OK**

```json
{
  "name": "Ficção Científica",
  "mediaCount": 12,
  "items": {
    "content": [
      {
        "type": "BOOK",
        "title": "Duna",
        "year": 1965,
        "genre": "Ficção científica",
        "coverUrl": "https://exemplo.com/capas/duna.jpg",
        "synopsis": "…"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 12,
    "totalPages": 1
  }
}
```

- **DTO público reduzido** (RN21): não expõe `status`, `rating`, ids internos nem dados do proprietário;
- Sempre reflete a lista atual (RF40);
- Token inexistente **ou** compartilhamento desativado → **404** genérico, sem distinguir os dois casos;
- Recomenda-se **limite de requisições** por IP neste endpoint (429).

---

## 10. Decisões fechadas e ponto em aberto

### Decisões fechadas

| # | Ponto | Decisão |
|---|---|---|
| 1 | **Tempo de vida do token** | Access token JWT de **1 hora**, sem refresh na v1 |
| 2 | **Política de senha** | 8 a 72 caracteres (72 é o limite prático do BCrypt), sem exigir símbolos |
| 3 | **Paginação** | `page` a partir de 0, `size` padrão 20 e máximo 50 (20 na pesquisa), formato da seção 3.1 |
| 4 | **`inLibrary` na pesquisa** | Resultados trazem `inLibrary` e `entryId` |
| 5 | **Busca na biblioteca (`q`)** | Filtro opcional em `GET /library` |
| 6 | **Listas sem paginação** | `GET /lists` devolve todas as listas |

### API externa de mídias

Decidido: **TMDB** (filmes e séries) e **Open Library** (livros), ambas gratuitas, atrás da interface `MediaProvider`. Comportamento da busca e dos filtros na seção 5. Requisitos de uso (atribuição ao TMDB, `User-Agent` identificado na Open Library, cache) estão no RNF13 de `requisitos.md`.

---

## 11. Próximo passo

Passar para o **esqueleto do projeto**: estrutura de pacotes por módulo, migrations com o DDL do modelo e o fluxo mínimo de ponta a ponta (cadastro, login e criar lista, com Favoritos automática), já com testes e Docker.
