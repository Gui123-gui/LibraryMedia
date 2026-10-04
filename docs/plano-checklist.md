# ✅ Personal Media Library — Plano e Checklist

> Marque cada tarefa trocando `[ ]` por `[x]`.
> Siga a ordem das fases: cada uma termina com algo que roda e pode ser testado.

## Auditoria do estado atual (2026-10-04)

**Estado de execução: Fase 8 — concluída e validada via Docker Compose.** Os cenários SDD 1–12, a tela de detalhes e os tipos gerados do OpenAPI estão implementados, e a integração real com MySQL, API, frontend e TMDB foi exercitada. As fases anteriores ainda têm pendências SDD próprias.

| Fase | Estado | Evidência e pendências principais |
|---|---|---|
| 0 — Preparação | Parcial | Repositório, `.gitignore`, Java 21/Spring Boot 3.5, Compose/MySQL 8.4, pacotes modulares e health endpoint existem. Criação/validação de chave TMDB e conferência no Initializr não são verificáveis pelo checkout. |
| 1 — Banco e domínio | Parcial | Migration `V1__create_initial_schema.sql`, segunda migration, entidades, enums, repositórios e testes existem. O Compose aplicou ambas as migrations em MySQL 8.4; permanece a divergência entre o nome da V1 e o nome planejado. O teste Testcontainers não conseguiu estabelecer conexão com seu container MySQL durante o startup. |
| 2 — Autenticação | Implementação presente; aceitação SDD pendente | Cadastro/login, BCrypt, JWT, segurança, criação de `Favoritos` e `/users/me` existem. O cenário de cadastro agora tem teste rastreável; ainda faltam testes para senha mínima, validade exata de 3600 s, e-mail inexistente vs. senha errada, token expirado e rollback do cadastro. |
| 3 — Listas | Implementação presente; aceitação SDD pendente | CRUD, proteção de Favoritos, confirmação, isolamento e itens de lista existem e há testes. Resumo de lista e exclusão da lista estão alinhados ao contrato; faltam testes rastreáveis por cenário e divergências de outros endpoints. |
| 4 — Provedores externos | Implementação presente; aceitação SDD pendente | `MediaProvider`, TMDB, Open Library, busca, cache, atribuição e testes com provedores simulados/WireMock existem; testes passaram. Falta rastrear cada cenário por nome e decidir política de cache. |
| 5 — Biblioteca, status e nota | Implementação presente; aceitação SDD pendente | Adição, filtros, status, avaliação, confirmações e concorrência existem; testes passaram. Falta teste por cenário e adequação do payload/status HTTP ao contrato. |
| 6 — Ranking e compartilhamento | Implementação presente; aceitação SDD pendente | Ranking, token, visualização pública, ativação/desativação e testes existem. Falta teste por cenário e decisão sobre formato da resposta do ranking/campos públicos. |
| 7 — Fechamento do backend | Parcial | Swagger, CORS, teste de isolamento/concorrência, Dockerfile e Compose existem. A inicialização completa de API + MySQL por Compose não foi verificada nesta auditoria. |
| 8 — Frontend | Concluída | Cenários 1–12, detalhes de mídia (RF16), geração de tipos OpenAPI e integração Compose estão validados. MySQL saudável com migrations aplicadas; API e frontend respondem; cadastro/login, Favoritos, busca/detalhes TMDB e ativação/desativação de compartilhamento foram exercitados. |
| 9 — Entrega | Parcial / não verificável | Dockerfile, Compose e README existem; stack integrada executou. Configuração de produção, deploy e publicação externa continuam pendentes/não verificáveis pelo checkout. |

**Verificação desta continuação:** `npm test` passou (18 testes frontend), `npm run build` concluiu e a integração Compose passou pelos fluxos de autenticação, Favoritos, busca/detalhes TMDB e compartilhamento. MySQL 8.4 está saudável e aplicou as migrations V1–V2; API `/actuator/health` retorna `UP`. `.\mvnw.cmd test` executou 25 testes: 24 passaram e `MySqlSchemaMappingTests` falhou ao iniciar/conectar ao MySQL temporário do Testcontainers (sem falhas de asserção nos testes executados).

**Divergências já identificadas para resolver antes de tratar contratos como conformes:**

- O catálogo descreve `errors` de validação como lista de campos/mensagens; o handler atual devolve um mapa.
- O requisito descreve senha em 8–72 caracteres, mas o código também limita a 72 bytes UTF-8 para BCrypt. A expectativa para senhas multibyte precisa ser decidida.
- A introdução do contrato diz que o provedor ainda não foi escolhido, embora os documentos definam TMDB e Open Library.

---

## Fase 0 — Preparação

- [ ] Criar o repositório no GitHub
- [ ] Criar o `.gitignore` (Java, Maven, IntelliJ, `.env`, `node_modules`)
- [ ] Gerar o projeto base com o `pom.xml` (Java 21, Spring Boot 3.5.x)
- [ ] Conferir as versões das dependências em start.spring.io
- [ ] Criar o `docker-compose.yml` com MySQL 8
- [ ] Criar o `application.yml` (perfil `dev`)
- [ ] Definir as variáveis de ambiente (senha do banco, chave JWT, chave do TMDB)
- [ ] Criar conta no TMDB e gerar a chave da API
- [ ] Criar a estrutura de pacotes por módulo: `auth`, `user`, `media`, `list`, `library`, `ranking`, `sharing`, `common`
- [ ] Subir a aplicação vazia e abrir `/actuator/health`

## Fase 1 — Banco e domínio

- [ ] Criar a migration `V1__create_tables.sql` com o DDL do MER
- [ ] Subir o MySQL e validar que o Flyway aplicou a migration
- [ ] Criar os enums `MediaType` e `ConsumptionStatus`
- [ ] Criar a entidade `User`
- [ ] Criar a entidade `Media` (chave única `provider + externalId`)
- [ ] Criar a entidade `MediaList`
- [ ] Criar a entidade `UserMedia` com `changeStatus()` e `rate()`
- [ ] Criar a entidade `ListItem` com chave composta
- [ ] Criar a entidade `Share`
- [ ] Criar os repositórios com `findByIdAndUserId` e `findAllByUserId`
- [ ] Testar o mapeamento com Testcontainers (MySQL real)

## Fase 2 — Autenticação e usuários

- [ ] Configurar o Spring Security (rotas públicas e privadas)
- [ ] Configurar o `BCryptPasswordEncoder`
- [ ] Criar o serviço de geração e validação do JWT (1 hora)
- [ ] Criar o filtro de autenticação JWT
- [ ] Implementar `POST /auth/register` (valida dados e e-mail único)
- [ ] Criar a lista Favoritos na mesma transação do cadastro
- [ ] Implementar `POST /auth/login`
- [ ] Implementar `GET /users/me`
- [ ] Criar o tratamento global de erros (`ProblemDetail` + catálogo de códigos)
- [ ] Testes: cadastro, e-mail duplicado, login válido e inválido, rota sem token

## Fase 3 — Listas

- [ ] `GET /lists` (com `mediaCount`, `covers` e `shared`)
- [ ] `POST /lists` (nome único por usuário, sem diferenciar maiúsculas)
- [ ] `GET /lists/{listId}`
- [ ] `PATCH /lists/{listId}` (Favoritos não pode ser renomeada)
- [ ] `DELETE /lists/{listId}` sem mídias exclusivas
- [ ] `DELETE /lists/{listId}` com mídias exclusivas: 409 `LIST_HAS_EXCLUSIVE_MEDIA` e depois `confirm=true`
- [ ] Bloquear a exclusão do Favoritos
- [ ] Testes: isolamento (lista de outro usuário responde 404), nome repetido, Favoritos protegida

## Fase 4 — Provedores externos de mídia

- [ ] Criar a interface `MediaProvider`
- [ ] Implementar o provedor TMDB (filmes e séries)
- [ ] Implementar o provedor Open Library (livros), com `User-Agent` identificado
- [ ] Mapear as respostas externas para `MediaSummary`
- [ ] Implementar `GET /media/search` com paginação e filtros (tipo e ano no provedor; gênero e nota mínima na página)
- [ ] Buscar nos dois provedores em paralelo e intercalar quando não houver `type`
- [ ] Tratar a falha de um provedor devolvendo o resultado do outro
- [ ] Implementar `GET /media/suggestions` (máximo 8)
- [ ] Implementar `GET /media/{provider}/{externalId}` com sinopse
- [ ] Preencher `inLibrary` e `entryId` nos resultados
- [ ] Configurar o cache (Caffeine) das respostas
- [ ] Testes com WireMock (sem chamar a internet)

## Fase 5 — Biblioteca, status e nota

- [ ] `POST /library` com `consumed = false` (status `WANT`)
- [ ] `POST /library` com `consumed = true` e nota obrigatória (`RATING_REQUIRED`)
- [ ] `POST /library` com mídia já existente: só adiciona às novas listas
- [ ] Informar `addedToLists` e `alreadyInLists` na resposta
- [ ] `GET /library` com filtros (`type`, `status`, `listId`, `q`, `sort`)
- [ ] `GET /library/{entryId}`
- [ ] `PUT /library/{entryId}/status` (qualquer transição, sem apagar a nota)
- [ ] `PUT /library/{entryId}/rating` (só com `DONE`; `RATING_REQUIRES_COMPLETED`)
- [ ] `GET /lists/{listId}/items` com atalho `links.search` quando vazia
- [ ] `POST /lists/{listId}/items` (200 com `alreadyInList` se já estiver)
- [ ] `DELETE /lists/{listId}/items/{entryId}` com lock (`SELECT ... FOR UPDATE`)
- [ ] Remover da última lista: 409 e depois `confirm=true`, apagando a nota
- [ ] `DELETE /library/{entryId}` com confirmação
- [ ] Testes: mídia nunca fica sem lista, nota preservada ao sair de `DONE`, remoção em concorrência

## Fase 6 — Ranking e compartilhamento

- [ ] `GET /rankings/{books|movies|series}` (só `DONE` com nota)
- [ ] Ordenar por nota decrescente e título como desempate
- [ ] Calcular `position` considerando a página
- [ ] `GET /lists/{listId}/share`
- [ ] `PUT /lists/{listId}/share` (ativar cria o token; reativar mantém o mesmo)
- [ ] Gerar o token com `SecureRandom` (32 bytes, Base64 URL-safe)
- [ ] `GET /public/lists/{token}` com DTO reduzido (sem status, nota e dados do dono)
- [ ] Responder 404 igual para token inexistente e compartilhamento desativado
- [ ] (Opcional) Limite de requisições por IP no endpoint público (429)
- [ ] Testes: ranking, desempate, visitante não vê nota, link desativado

## Fase 7 — Fechamento do backend

- [ ] Revisar o Swagger (tags, exemplos e códigos de erro)
- [ ] Revisar se toda consulta privada filtra pelo dono
- [ ] Escrever o teste de isolamento entre dois usuários
- [ ] Escrever o teste de concorrência da regra "mídia sempre em uma lista"
- [ ] Configurar o CORS para o frontend
- [ ] Criar o `Dockerfile` da API
- [ ] Subir API + MySQL com `docker-compose`

## Fase 8 — Frontend (JavaScript / TypeScript)

**Base**
- [x] Criar o projeto com Vite + TypeScript
- [x] Definir o framework (React é a sugestão)
- [x] Gerar os tipos a partir do OpenAPI (`openapi-typescript`)
- [x] Criar o cliente HTTP único (`fetch` ou axios) com `Bearer` token
- [x] Tratar erros `ProblemDetail`, incluindo os 409 de confirmação
- [x] Guardar o token e tratar a expiração (1 hora) redirecionando para o login
- [x] Configurar as rotas (públicas e privadas)

**Telas**
- [x] Cadastro e login
- [x] Navegação principal (Favoritos, Minhas listas, Pesquisa, Todas as mídias, Ranking)
- [x] Minhas listas com capa em mosaico
- [x] Criar, renomear e excluir lista (com diálogo de confirmação)
- [x] Pesquisa com autocomplete (debounce de 300 ms), filtros e paginação
- [x] Tela de "nenhum resultado" (estilo 404/cartoon)
- [x] Detalhes da mídia
- [x] Fluxo de adicionar: escolher listas, "já leu/assistiu?" e nota
- [x] Tela "Todas as mídias" com ações (status, nota, listas, remover)
- [x] Ranking por tipo
- [x] Compartilhar lista (ativar, desativar e copiar o link)
- [x] Página pública da lista compartilhada (somente leitura)
- [x] Atribuição ao TMDB visível na aplicação

**Qualidade**
- [x] Estados de carregamento e de erro em todas as telas
- [x] Testes dos fluxos principais

## Fase 9 — Entrega

- [x] Containerizar o frontend e subir tudo no `docker-compose`
- [ ] Escrever o README (como rodar, stack, prints, link do Swagger)
- [ ] Configurar as variáveis de ambiente para produção
- [ ] Fazer o deploy
- [ ] Publicar o link no GitHub e no LinkedIn

---

## Progresso

| Fase | Concluída |
|---|---|
| 0 — Preparação | [ ] |
| 1 — Banco e domínio | [ ] |
| 2 — Autenticação | [ ] |
| 3 — Listas | [ ] |
| 4 — Provedores externos | [ ] |
| 5 — Biblioteca, status e nota | [ ] |
| 6 — Ranking e compartilhamento | [ ] |
| 7 — Fechamento do backend | [ ] |
| 8 — Frontend | [x] |
| 9 — Entrega | [ ] |
