# 003 — Provedores externos

## Objetivo

Pesquisar livros, filmes e séries por meio de provedores externos isolados por uma interface comum.

## Requisitos relacionados

- RF11 — Pesquisa unificada; RF12 — Autocomplete; RF13 — Paginação; RF14 — Filtros; RF15 — Tela sem resultados; RF16 — Detalhes da mídia.
- RN22 — Identidade única da mídia; RNF10 — Integração externa isolada; RNF13 — Atribuição, User-Agent, cache e uso não comercial.
- UC03 — Pesquisar mídia.

## Cenários de aceitação

### Cenário 1 — Busca unificada

**Dado** que `q` tem de 2 a 100 caracteres e não há filtro `type`  
**Quando** o usuário pesquisa mídias  
**Então** a API consulta TMDB e Open Library por `MediaProvider`, intercala filmes, séries e livros, pagina os resultados e não persiste as obras pesquisadas.

### Cenário 2 — Busca filtrada por tipo e ano

**Dado** que a pesquisa informa `type` e/ou `year`  
**Quando** o usuário chama `GET /api/v1/media/search`  
**Então** os filtros de tipo e ano são enviados ao provedor correspondente e a resposta obedece à paginação documentada.

### Cenário 3 — Filtros locais

**Dado** que a pesquisa informa gênero ou nota mínima  
**Quando** os resultados da página do provedor são retornados  
**Então** a API filtra essa página como melhor esforço; a página pode conter menos que `size`.

### Cenário 4 — Falha parcial de provedor

**Dado** que a busca consulta dois provedores e um deles falha  
**Quando** o outro provedor retorna resultados  
**Então** a busca responde 200 com os resultados disponíveis.

### Cenário 5 — Todos os provedores indisponíveis

**Dado** que todos os provedores selecionados falham ou expiram  
**Quando** o usuário pesquisa  
**Então** a API responde 503 `EXTERNAL_PROVIDER_UNAVAILABLE`.

### Cenário 6 — Sugestões

**Dado** que `q` contém ao menos 2 caracteres  
**Quando** o cliente chama `GET /api/v1/media/suggestions`  
**Então** a resposta contém no máximo 8 sugestões.

### Cenário 7 — Detalhes da mídia

**Dado** um provedor e identificador externo conhecidos  
**Quando** o usuário consulta `GET /api/v1/media/{provider}/{externalId}`  
**Então** a API retorna os metadados disponíveis, incluindo sinopse quando fornecida.

### Cenário 8 — Mídia externa inexistente

**Dado** um identificador não localizado no provedor  
**Quando** o usuário consulta seus detalhes  
**Então** a API responde 404 `MEDIA_NOT_FOUND`.

### Cenário 9 — Resultado já salvo na biblioteca

**Dado** que o usuário autenticado já adicionou uma mídia  
**Quando** a busca ou os detalhes retornam essa mídia  
**Então** a resposta informa `inLibrary=true` e seu `entryId`.

### Cenário 10 — Atribuição e identificação do Open Library

**Dado** que a aplicação usa TMDB e Open Library  
**Quando** consulta a atribuição ou chama Open Library  
**Então** a atribuição TMDB está disponível em endpoint público e a chamada Open Library envia o `User-Agent` configurado.

## Rastreabilidade

| Cenário | Requisitos | Teste (a criar/ajustar) |
|---|---|---|
| 1 | RF11, RF13, UC03, RNF10 | `cenario1_buscaUnificadaIntercalaEnaoPersiste` |
| 2 | RF13, RF14, UC03 | `cenario2_buscaAplicaTipoEAnoNoProvedor` |
| 3 | RF14 | `cenario3_generoENotaFiltramPagina` |
| 4 | RNF10 | `cenario4_buscaContinuaComUmProvedorDisponivel` |
| 5 | RF11, RNF07 | `cenario5_todosProvedoresIndisponiveis` |
| 6 | RF12 | `cenario6_sugestoesLimitadasAOito` |
| 7 | RF16 | `cenario7_detalhesRetornamMetadadosDisponiveis` |
| 8 | RF16 | `cenario8_midiaExternaInexistente` |
| 9 | RF11, RF16 | `cenario9_buscaIndicaMidiaNaBiblioteca` |
| 10 | RNF13 | `cenario10_atribuicaoEUserAgentConfigurados` |

## Decisões pendentes

- `[A DEFINIR]` Identificação de títulos semelhantes e livros da mesma franquia na pesquisa/exibição (seção 12.2 de `docs/requisitos.md`).
- `[A DEFINIR]` Política e prazo de cache/renovação de metadados conforme os termos do TMDB (seção 12.2). A implementação atual usa Caffeine em memória com expiração de 15 minutos.
- A escolha dos provedores está fechada: TMDB para filmes/séries e Open Library para livros. A introdução do contrato que diz “provedor ainda não foi escolhido” está desatualizada e precisa ser corrigida.

## Fora do escopo

- Persistir resultados apenas por pesquisar; a mídia é gravada quando adicionada à biblioteca.
- Trocar os provedores definidos para a v1 sem atualizar a especificação.
