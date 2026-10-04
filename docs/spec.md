# 📚 Personal Media Library — Spec

> Resumo do que entendi do projeto, em um único documento.
> Os detalhes completos estão em `requisitos.md`, `modelo-dominio-e-mer.md` e `api-endpoints-e-contratos.md`.

---

## 1. A ideia em uma frase

Uma **biblioteca pessoal de livros, filmes e séries organizada em listas** (como playlists): o usuário pesquisa uma obra, guarda em uma ou mais listas, marca se já consumiu, dá uma nota de 1 a 10 e pode compartilhar listas por link público somente leitura.

O projeto nasce para uso pessoal, mas é **multiusuário com dados isolados**. Deve ser **simples e gratuito**.

## 2. Para quem é

| Ator | O que faz |
|---|---|
| **Usuário** | Cria conta, pesquisa, monta listas, marca status, avalia, vê ranking, compartilha |
| **Visitante** | Abre um link compartilhado e vê a lista (só leitura, sem status nem notas) |
| **Serviço externo** | TMDB (filmes e séries) e Open Library (livros) fornecem os dados das obras |

## 3. Escopo da v1

**Dentro:** cadastro e login; listas personalizadas e Favoritos automática; pesquisa unificada (autocomplete, paginação, filtros); biblioteca; status; nota; ranking pessoal por tipo; compartilhamento por link com ativar/desativar; API documentada, testes e Docker.

**Fora:** painel administrativo, permissões complexas, avaliação global entre usuários, temporadas e episódios, expiração de links, cópia estática de lista compartilhada, refresh token.

## 4. Como eu entendo o domínio

Três conceitos, e a separação entre eles é o coração do projeto:

```text
Media       → a obra em si (título, tipo, ano, capa, sinopse). Compartilhada, não muda por usuário.
UserMedia   → a obra NA biblioteca de um usuário, com seu status e sua nota.
List        → só organização; liga UserMedia às listas (uma obra pode estar em várias).
```

Exemplo: "Duna" existe uma vez em `Media`. O usuário A tem `UserMedia` com status **Lido** e nota 9; o usuário B tem **Quero ler**. A mesma `UserMedia` do A pode estar em Favoritos e em "Ficção Científica" sem duplicar nada.

**Status** (guardado como `WANT`, `IN_PROGRESS`, `DONE`, exibido conforme o tipo):

| | Livro | Filme / Série |
|---|---|---|
| Ainda não | Quero ler | Quero assistir |
| Em andamento | Lendo | Assistindo |
| Concluído | Lido | Assistido |

## 5. Regras que não podem quebrar

1. **Toda mídia na biblioteca está em pelo menos uma lista.**
2. A mesma mídia **não aparece duas vezes na mesma lista**.
3. Cada usuário tem **exatamente uma lista Favoritos**, criada no cadastro, que **não pode ser excluída nem renomeada**.
4. Nome de lista é **único por usuário** (sem diferenciar maiúsculas).
5. **Nota de 1 a 10, só para mídia concluída.** Se a mídia sai de "concluído", a nota **fica guardada** e a mídia sai do ranking; ao voltar, a nota volta a contar.
6. **Ranking** = mídias concluídas e avaliadas, por tipo, nota decrescente e título como desempate.
7. Remover da **última lista**, excluir uma lista com **mídias exclusivas** e **remover da biblioteca** exigem **confirmação**. O resultado é a mídia sair da biblioteca, **com a nota**.
8. **Isolamento total:** o usuário nunca vê nem altera dados de outro. Recurso alheio responde 404.
9. **Compartilhamento:** um link por lista, público, somente leitura, sempre mostra a lista atual, sem expiração. Pode ser desativado. O visitante vê nome da lista e dados das mídias, **nunca status, nota ou dados do dono**.

## 6. Fluxos principais

**Adicionar mídia:** pesquisar → escolher → ver detalhes → escolher uma ou mais listas → "já leu/assistiu?" → se sim, concluída e nota obrigatória; se não, "quero ler/assistir".

**Remover:** se a mídia está em outras listas, sai só da lista. Se era a última, pede confirmação e sai da biblioteca.

**Excluir lista:** avisa quantas mídias só existiam nela, pede confirmação e remove essas mídias da biblioteca.

**Compartilhar:** ativar gera o link; desativar o derruba; reativar mantém o mesmo link.

## 7. API (resumo)

Base `/api/v1`, JSON, JWT de 1 hora, erros no formato `ProblemDetail` com códigos próprios.

| Área | Endpoints |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `GET /users/me` |
| Pesquisa | `GET /media/search`, `GET /media/suggestions`, `GET /media/{provider}/{externalId}` |
| Listas | `GET/POST /lists`, `GET/PATCH/DELETE /lists/{id}` |
| Itens da lista | `GET/POST /lists/{id}/items`, `DELETE /lists/{id}/items/{entryId}` |
| Biblioteca | `GET/POST /library`, `GET/DELETE /library/{id}`, `PUT .../status`, `PUT .../rating` |
| Ranking | `GET /rankings/{books|movies|series}` |
| Compartilhamento | `GET/PUT /lists/{id}/share`, `GET /public/lists/{token}` (sem login) |

Confirmações são impostas pela API: sem `?confirm=true`, a resposta é **409** e nada é apagado.

## 8. Arquitetura e stack

- **Monolito em camadas, organizado por módulos**: `auth`, `user`, `media`, `list`, `library`, `ranking`, `sharing`, `common`.
- **Java 21, Spring Boot, Spring Security (JWT), Spring Data JPA, MySQL, Flyway, Maven, springdoc (Swagger), Docker.**
- **Integração externa isolada** atrás de `MediaProvider`, com duas implementações: TMDB e Open Library. Sem filtro de tipo, a busca consulta as duas em paralelo e intercala.
- **Integridade em duas camadas:** o banco garante o que consegue (unicidades, `CHECK` da nota, cascatas) e o serviço garante o resto (mídia sempre em uma lista, em transação com lock).

## 9. Decisões já tomadas

| Tema | Decisão |
|---|---|
| Modelo de mídia | Entidade única com campo de tipo; chave única `(provider, externalId)` |
| Séries | Tratadas como um todo, sem temporadas e episódios |
| Nota ao remover da biblioteca | Apagada junto |
| Token | JWT de 1 hora, sem refresh |
| Senha | 8 a 72 caracteres |
| Paginação | `page` de 0, tamanho 20 (máximo 50; 20 na pesquisa) |
| Filtros da pesquisa | Tipo e ano pelo provedor; gênero e nota mínima como melhor esforço |
| APIs externas | TMDB + Open Library (gratuitas; TMDB só para uso não comercial) |

## 10. Riscos e pontos de atenção

- **TMDB é gratuito só para uso não comercial** e exige atribuição. Se o projeto for monetizado, é preciso licença.
- **Filtros de gênero e nota** filtram sobre a página, que pode vir com menos itens. Vale confirmar na documentação oficial o que o TMDB filtra de forma nativa e o prazo de cache dos metadados.
- **Mesclar duas fontes na busca** é a parte mais delicada da paginação; a v1 aceita intercalação simples, sem ordenar por relevância global.
- **Regra "mídia sempre em uma lista"** não é expressável no banco; depende de transação com lock e de testes de concorrência.

## 11. Estado atual

| Etapa | Situação |
|---|---|
| Requisitos | ✅ Fechados |
| Modelo de domínio e MER | ✅ Fechados. DDL executado e testado em MySQL 8.0 |
| Endpoints e contratos | ✅ Fechados |
| Escolha das APIs externas | ✅ Decidida |
| **Esqueleto do projeto** | ⏳ Não entregue ainda: estrutura de pacotes, `pom.xml`, migration, Docker e fluxo mínimo (cadastro, login, criar lista) |
| Implementação dos demais módulos | Pendente |

## 12. O que eu gostaria que você confirmasse

1. A separação **`Media` × `UserMedia` × `List`** representa o que você imagina?
2. A regra de que **sair da última lista significa sair da biblioteca (com a nota)** é o comportamento desejado, mesmo sendo destrutivo?
3. O visitante enxergar **só as obras, sem status e sem notas**, está certo para você?
4. O objetivo continua sendo **portfólio ou uso pessoal, não comercial**?
