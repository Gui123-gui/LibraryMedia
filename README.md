# Personal Media Library

Biblioteca pessoal de livros, filmes e séries. O projeto será desenvolvido como um monólito modular: uma aplicação Spring Boot com módulos de domínio separados e um frontend web React + TypeScript.

## Tecnologias

- Backend: Java 21, Spring Boot, Spring Data JPA, Spring Security, MySQL e Flyway.
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

Defina `DB_PASSWORD` e `MYSQL_ROOT_PASSWORD` no ambiente do PowerShell e execute:

```powershell
$env:DB_PASSWORD = "uma-senha-local"
$env:MYSQL_ROOT_PASSWORD = "outra-senha-local"
docker compose up --build
```

- Frontend: `http://localhost:3000`
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

O MySQL usa o volume `mysql_data`. Para preservar os dados locais, não remova esse volume.

## Executar localmente

Inicie um MySQL 8.4, configure `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` e rode o backend:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/librarymedia?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "librarymedia"
$env:DB_PASSWORD = "uma-senha-local"
.\mvnw.cmd spring-boot:run
```

Flyway cria o schema no início da aplicação. H2 está disponível somente para testes.

Para o frontend, instale uma versão LTS do Node.js e execute:

```powershell
Set-Location frontend
npm ci
npm run dev
```

O servidor Vite fica em `http://localhost:5173` e encaminha `/api` para `http://localhost:8080`.

## Testes do backend

Na raiz do projeto:

```powershell
.\mvnw.cmd test
```
