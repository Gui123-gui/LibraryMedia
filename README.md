# Personal Media Library

Biblioteca pessoal de livros, filmes e séries. O projeto será desenvolvido como um monólito modular: uma aplicação Spring Boot com módulos de domínio separados e um frontend web React + TypeScript.

## Tecnologias

- Backend: Java 21, Spring Boot 3.5.x, Spring Data JPA, Spring Security, MySQL e Flyway.
- Frontend: React, TypeScript e Vite.
- API: REST em `/api/v1`, documentada com OpenAPI/Swagger.
- Desenvolvimento: Docker Compose.

## Organização do backend

O código fica sob `src/main/java/com/guilhermedev/librarymedia`. Cada módulo agrupa uma área do domínio:

| Pacote | Responsabilidade |
|---|---|
| `auth` | Cadastro, login e emissão de tokens. |
| `user` | Identidade e dados do usuário autenticado. |
| `media` | Metadados de obras e integração com provedores externos. |
| `list` | Listas do usuário e associação de itens às listas. |
| `library` | Relação entre usuário e mídia, status e avaliação. |
| `ranking` | Consultas dos rankings pessoais. |
| `sharing` | Ativação e visualização pública de listas. |
| `common` | Configurações e componentes compartilhados entre módulos. |

Dentro de um módulo, a regra geral é separar responsabilidades em `api` (controllers e DTOs HTTP), `application` (casos de uso/serviços), `domain` (regras do domínio) e `infrastructure` (repositórios e integrações técnicas). Essas pastas serão criadas conforme cada funcionalidade for implementada, evitando classes vazias sem responsabilidade.

O fluxo típico de uma requisição será:

```text
HTTP request -> Controller -> Service -> Repository -> Database
```

O controller trata a entrada e a resposta HTTP; o service coordena o caso de uso; o repository é a porta de acesso aos dados. As regras importantes ficam no domínio ou no service, não escondidas no controller.

## Organização do frontend

O diretório `frontend` contém a aplicação React + TypeScript. A configuração inicial do Vite permite executar o frontend separado durante o desenvolvimento e encaminhar chamadas `/api` para o backend. No Compose, o Nginx serve a aplicação e encaminha essas chamadas para o serviço `api`.

## Executar com Docker Compose

O Spring Initializr atualmente lista versões 4.x do Spring Boot. Este projeto segue a decisão de aprendizado de usar a linha 3.5.x; as versões publicadas podem ser conferidas no Maven Central. O parent do Spring Boot gerencia versões compatíveis das dependências Spring.

Copie `.env.example` para `.env`, substitua todos os valores de exemplo e execute:

```powershell
Copy-Item .env.example .env
# Edite o arquivo .env e substitua os valores de exemplo antes de continuar.
docker compose up --build
```

- Frontend: `http://localhost:3000`
- API: `http://localhost:8080`
- MySQL: `localhost:${MYSQL_PORT}` (default `3306`)
- Verificação de saúde: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

Se a porta local `3306` já estiver ocupada por outro MySQL, altere `MYSQL_PORT` no `.env` (por exemplo, para `3307`); a API continua usando o MySQL interno do Compose.
O MySQL usa o volume `mysql_data`. Para preservar os dados locais, não remova esse volume.
O arquivo `.env` contém configurações locais e é ignorado pelo Git. Nunca coloque credenciais reais em `.env.example`.
`TMDB_API_KEY` deve conter a chave de API **v3** do TMDB, não o Read Access Token v4.

## Executar localmente

Inicie um MySQL 8.4, copie `.env.example` para `.env` e configure as variáveis no ambiente do PowerShell:

```powershell
Copy-Item .env.example .env
# Edite .env e substitua os valores de exemplo.
$env:DB_URL = "jdbc:mysql://localhost:3306/librarymedia?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "librarymedia"
$env:DB_PASSWORD = "a-mesma-senha-local-do-arquivo-env"
$env:JWT_SECRET = "a-mesma-chave-local-do-arquivo-env"
$env:TMDB_API_KEY = "sua-chave-tmdb"
.\mvnw.cmd spring-boot:run
```

O perfil `dev` é ativado por padrão. Flyway cria o schema no início da aplicação. O endpoint `/actuator/health` é público para verificações; as outras rotas exigem autenticação. H2 está disponível somente para testes.

Para o frontend, instale uma versão LTS do Node.js e execute:

```powershell
Set-Location frontend
npm ci
npm run dev
```

O servidor Vite fica em `http://localhost:5173` e encaminha `/api` para `http://localhost:8080`.
No Compose, `APP_FRONTEND_BASE_URL` usa `http://localhost:3000` por padrão para que links
compartilhados apontem para o Nginx; ajuste a variável quando publicar o frontend em outra origem.
Com a API em execução, atualize os tipos estáticos do contrato com `npm run generate:api-types`
no diretório `frontend`; o arquivo gerado fica em `frontend/src/generated/api-schema.d.ts`.

## Testes do backend

Na raiz do projeto:

```powershell
.\mvnw.cmd test
```

Os testes padrão usam H2 e provedores simulados; um teste WireMock cobre a integração HTTP com o Open Library.
Há também um teste opcional de validação das migrations e mapeamentos Hibernate no MySQL 8.4 via Testcontainers.
Ele é automaticamente ignorado quando Docker não está disponível, então a suíte padrão não depende de Docker.

## API implementada

A API usa JWT. Cadastre-se em `POST /api/v1/auth/register` (incluindo `passwordConfirmation`) e entre
em `POST /api/v1/auth/login`; o cadastro retorna apenas `id`, `name` e `email`, enquanto o login retorna
`accessToken`, `tokenType` e `expiresIn`. Envie esse token no cabeçalho HTTP Authorization, usando o esquema de autenticação JWT.
O cadastro cria automaticamente a lista imutável `Favoritos`. `GET /api/v1/users/me` retorna somente id, nome e e-mail.
Rotas disponíveis:

| Área | Rotas |
|---|---|
| Mídia | `GET /api/v1/media/search?q=...&page=0&size=20&type=BOOK&year=...&genre=...&minRating=...`, `GET /api/v1/media/suggestions?q=...`, `GET /api/v1/media/{provider}/{externalId}?type=...` |
| Listas | `GET/POST /api/v1/lists`, `GET/PATCH/DELETE /api/v1/lists/{listId}`, `GET/POST /api/v1/lists/{listId}/items`, `DELETE /api/v1/lists/{listId}/items/{entryId}` |
| Biblioteca | `GET/POST /api/v1/library`, `GET/DELETE /api/v1/library/{entryId}`, `PUT /api/v1/library/{entryId}/status`, `PUT /api/v1/library/{entryId}/rating` |
| Rankings | `GET /api/v1/rankings/books`, `/movies` ou `/series` |
| Compartilhamento | `GET/PUT /api/v1/lists/{listId}/share`, `GET /api/v1/public/lists/{token}` |

Paginação começa em zero e retorna `content`, `page`, `size`, `totalElements` e `totalPages`.
Tamanho padrão: 20 (máximo 50; buscas externas têm máximo 20). A biblioteca aceita filtros `type`,
`status`, `listId`, `q` e `sort`; adições exigem ao menos um `listId` explícito. Erros seguem Problem Details
e incluem `code`; conflitos que exigem confirmação usam HTTP 409 e query parameter `confirm=true`.
Listas e entradas são sempre filtradas pelo usuário autenticado; recursos de outra pessoa retornam
404. Remover a última associação de uma obra ou apagar uma lista com obras exclusivas pede
confirmação. Uma avaliação de 1 a 10 só pode ser criada/alterada enquanto o status é `DONE`, mas é
preservada quando o status muda.

Busca consulta TMDB (filmes e séries) e Open Library (livros), não grava resultados até serem
adicionados à biblioteca, usa cache Caffeine em memória limitado e com expiração para respostas externas e continua com o provedor
disponível se o outro falhar. Busca unificada interleave os resultados; filtro de gênero e nota é
aplicado localmente na página retornada. Open Library recebe o User-Agent configurável
`OPEN_LIBRARY_USER_AGENT`. TMDB requer `TMDB_API_KEY`.

O endpoint `GET /api/v1/media/attribution` fornece o texto e link de atribuição do TMDB. Configure
`APP_FRONTEND_BASE_URL` para definir a origem usada nas URLs de compartilhamento. A interface
que consumir a API deve exibir: “This product uses the TMDB API but is not endorsed or certified by
TMDB.” Consulte também [os requisitos de atribuição do TMDB](https://developer.themoviedb.org/docs/faq).

Swagger UI: `http://localhost:8080/swagger-ui.html`; OpenAPI JSON: `/v3/api-docs`.

## Chaves de serviços externos

Copie `.env.example` para `.env` e substitua os valores fictícios. A chave JWT deve ter pelo menos
32 bytes aleatórios. Não compartilhe nem versiona o `.env`. Os testes usam H2 e provedores simulados,
sem chaves reais ou chamadas de rede.
