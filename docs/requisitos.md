# 📚 Personal Media Library — Requisitos

> **Fonte de verdade funcional do projeto.**
> Este documento consolida a versão formal (RF/RN/RNF/UC) e a versão detalhada (decisões de negócio, fluxos e princípios). Mudanças de comportamento devem ser registradas aqui **antes** de serem tratadas como definitivas.

---

## 1. Visão geral

O **Personal Media Library** é um sistema de biblioteca pessoal para organizar **livros, filmes e séries**. Cada usuário mantém sua própria biblioteca, cria listas personalizadas, acompanha o status de consumo, avalia mídias e pode compartilhar listas por link público somente leitura.

A ideia central é uma **biblioteca pessoal baseada em listas**, semelhante ao conceito de playlists.

O sistema é pensado inicialmente para uso pessoal, mas é **multiusuário**, com os dados de cada usuário isolados.

---

## 2. Objetivos

O sistema deve permitir que o usuário:

- Pesquise livros, filmes e séries;
- Adicione mídias à sua biblioteca;
- Organize mídias em listas personalizadas;
- Tenha uma lista automática de Favoritos;
- Coloque uma mesma mídia em várias listas;
- Acompanhe se já leu ou assistiu cada mídia;
- Registre uma avaliação pessoal de 1 a 10;
- Altere depois seu status e sua avaliação;
- Consulte um ranking pessoal;
- Compartilhe listas por links públicos somente para visualização.

---

## 3. Escopo

### 3.1 Tipos de mídia

- 📖 Livro
- 🎬 Filme
- 📺 Série

As listas são **independentes do tipo de mídia**: uma mesma lista pode conter livros, filmes e séries. Não há lista exclusiva por tipo.

### 3.2 Fora do escopo da primeira versão

- Painel administrativo;
- Administrador para bloquear usuários ou remover conteúdos;
- Sistema complexo de permissões administrativas;
- Avaliação global obrigatória entre usuários;
- Limite de quantidade de links compartilhados;
- Expiração automática dos links;
- Separação obrigatória das listas por tipo;
- Cópia independente (estática) de lista compartilhada.

---

## 4. Atores

### 4.1 Usuário
Ator principal. Cria conta, autentica-se, pesquisa mídias, gerencia biblioteca, listas, favoritos, status e avaliações, consulta rankings e compartilha listas.

### 4.2 Visitante
Pessoa que acessa uma lista por link compartilhado. Pode visualizar a lista e suas mídias, mas **não pode modificar nada**.

### 4.3 Serviço externo de mídias
Sistema externo que fornece, conforme disponibilidade: título, ano, gênero, avaliação, tipo, imagem, sinopse, identificador externo e outras informações.

---

## 5. Princípios fundamentais do domínio

1. 👤 **Dados pertencem ao usuário** — biblioteca, listas, status e avaliações são individuais.
2. 📚 **Biblioteca é organizada por listas** — nenhuma mídia fica na biblioteca sem pelo menos uma lista.
3. ⭐ **Favoritos é uma lista especial** — existe automaticamente, não pode ser excluída.
4. 🔁 **Uma mídia pode estar em várias listas** — mas nunca duas vezes na mesma lista.
5. 📊 **Status é pessoal** — depende do usuário, não da mídia.
6. ⭐ **Avaliação é pessoal** — cada usuário tem sua própria nota.
7. 🏆 **Ranking depende da conclusão** — só mídias concluídas participam.
8. 💾 **Mudar o status não apaga a avaliação.**
9. 🗑️ **Última remoção exige confirmação** — remover da última lista remove da biblioteca.
10. 🔎 **Pesquisa é unificada** — livros, filmes e séries na mesma experiência.
11. 🔗 **Compartilhamento é somente leitura.**
12. 🔄 **Lista compartilhada é dinâmica** — reflete o estado atual.

---

## 6. Requisitos funcionais

> A numeração abaixo é nova e agrupa os requisitos por área.

### 6.1 Conta e acesso

**RF01 — Cadastro de usuário.** O sistema deve permitir criar conta com nome, e-mail, senha e confirmação de senha.
- O e-mail deve ser único;
- A confirmação deve corresponder à senha;
- A senha deve ter de 8 a 72 caracteres, sem exigência de símbolos;
- Os dados devem ser validados antes da criação.

**RF02 — Login.** O sistema deve autenticar o usuário por e-mail e senha e, após autenticação válida, liberar o acesso aos recursos privados.

**RF03 — Isolamento dos dados.** Cada usuário só acessa seus próprios dados. Um usuário não pode consultar a biblioteca de outro, alterar listas, status ou avaliações de outro, nem remover recursos de outro.
- *Exceção:* listas compartilhadas por link público (somente leitura).

### 6.2 Listas

**RF04 — Criar lista.** O usuário pode criar listas personalizadas com, no mínimo, nome e usuário proprietário. Uma lista pode ser criada **vazia**.
- O nome da lista deve ser **único por usuário**, sem diferenciar maiúsculas de minúsculas (RN26).

**RF05 — Listar listas.** O usuário pode consultar todas as suas listas.

**RF06 — Renomear/alterar lista.** O proprietário pode alterar os dados permitidos da lista, como o nome, respeitando a unicidade do nome (RN26). A lista **Favoritos não pode ser renomeada** (RN25).

**RF07 — Excluir lista.** O usuário pode excluir listas personalizadas. A lista **Favoritos não pode ser excluída**.
- Se a lista contiver mídias que **não pertencem a nenhuma outra lista**, o sistema informa quantas mídias sairão da biblioteca e **solicita confirmação** antes de excluir (RN24);
- Mídias que também estão em outras listas permanecem na biblioteca;
- Se houver compartilhamento ativo, ele deixa de existir junto com a lista, e o link passa a responder como não encontrado.

**RF08 — Lista Favoritos.** O sistema cria automaticamente uma lista Favoritos para cada usuário no momento do cadastro.
- Existe exatamente uma por usuário;
- Pode receber qualquer tipo de mídia;
- Mídias podem ser removidas dela;
- Comporta-se como lista normal, exceto pela impossibilidade de exclusão e de renomeação;
- **Não é recriada** automaticamente se uma mídia for removida da biblioteca.

**RF09 — Ação em lista vazia.** Uma lista vazia deve oferecer uma ação que leve o usuário à pesquisa de mídias (ex.: "🔎 Pesquisar uma mídia"), seguindo a ideia de navegação orientada por ações (HATEOAS).

**RF10 — Capa da lista.** As listas devem ter representação visual. Quando houver várias mídias, a capa pode ser uma composição das imagens das mídias, no estilo de capas de playlists.

### 6.3 Pesquisa

**RF11 — Pesquisa unificada.** Uma única pesquisa por nome, retornando livros, filmes e séries, consultando fonte externa. Não haverá pesquisas separadas por tipo.

**RF12 — Autocomplete.** O sistema deve sugerir mídias enquanto o usuário digita.

**RF13 — Paginação.** Os resultados devem ser paginados, permitindo navegar entre páginas.

**RF14 — Filtros.** O sistema deve permitir filtrar os resultados por **ano, gênero, avaliação e tipo de mídia** (livro, filme ou série).

**RF15 — Nenhum resultado.** Sem resultados, o sistema deve exibir uma tela/mensagem específica, com representação no estilo página 404/cartoon, em vez de tela vazia.

**RF16 — Detalhes da mídia.** O usuário pode ver detalhes, conforme disponibilidade: título, tipo, ano, gênero, avaliação externa, capa e sinopse — de modo que consiga identificar a mídia antes de adicioná-la.

### 6.4 Biblioteca

**RF17 — Adicionar mídia à biblioteca.** Uma mídia pesquisada **não** pertence automaticamente à biblioteca; passa a pertencer quando o usuário a adiciona, escolhendo **uma ou mais listas** (com opção de criar nova lista no ato).

**RF18 — Mídia em múltiplas listas.** Uma mídia pode pertencer simultaneamente a várias listas do mesmo usuário, sem duplicar o registro da mídia na biblioteca.

**RF19 — Sem duplicidade na lista.** A mesma mídia não pode aparecer duas vezes na mesma lista. Se o usuário tentar adicionar novamente, o sistema apenas informa que ela já está presente e **não cria** segunda associação.

**RF20 — Mídia sempre em pelo menos uma lista.** Nenhuma mídia pode permanecer na biblioteca sem pertencer a pelo menos uma lista.

**RF21 — Organização posterior.** O usuário pode adicionar uma mídia já existente na biblioteca a outras listas. Não é obrigatório definir toda a organização no momento da adição.

**RF22 — Remover mídia de uma lista.** O usuário pode remover uma mídia de uma lista específica. Se ela ainda pertencer a outras listas, continua na biblioteca.

**RF23 — Remover da última lista.** Se a lista for a última da mídia, o sistema deve **solicitar confirmação** antes de prosseguir, avisando que a mídia será removida da biblioteca. O usuário pode cancelar ou confirmar.

**RF24 — Remover da biblioteca.** O usuário pode remover uma mídia da biblioteca, o que a remove de todas as suas listas, com **confirmação prévia**.

**RF25 — Readicionar mídia.** Uma mídia removida pode ser pesquisada e adicionada novamente, como uma nova associação.

**RF26 — Tela "Todas as mídias".** Área que lista todas as mídias da biblioteca, com ações: alterar status, alterar avaliação, adicionar a outra lista, remover de uma lista, remover da biblioteca, ver detalhes e sinopse.

### 6.5 Status

**RF27 — Status de livro.** `QUERO_LER`, `LENDO`, `LIDO`.

**RF28 — Status de filme e série.** `QUERO_ASSISTIR`, `ASSISTINDO`, `ASSISTIDO`.

**RF29 — Pergunta ao adicionar.** Ao adicionar uma mídia, o sistema pergunta se o usuário já consumiu ("Você já leu [mídia]?" / "Você já assistiu [mídia]?").
- **Sim:** marca como concluída e solicita avaliação;
- **Não:** status inicial `QUERO_LER` (livro) ou `QUERO_ASSISTIR` (filme/série), sem exigir avaliação.

**RF30 — Alterar status.** O usuário pode alterar o status a qualquer momento; o fluxo não precisa ser linear.

### 6.6 Avaliação

**RF31 — Avaliar mídia.** O usuário pode atribuir nota de **1 a 10**, associada ao usuário e à mídia.

**RF32 — Avaliação só para concluídas.** Só é permitido avaliar quando o livro estiver `LIDO` ou o filme/série estiver `ASSISTIDO`.

**RF33 — Avaliação obrigatória ao concluir na adição.** Se o usuário informa que já consumiu a mídia durante a adição, a nota é solicitada imediatamente.

**RF34 — Alterar avaliação.** A nota pode ser alterada depois; a nova substitui a anterior.

**RF35 — Avaliação preservada.** Se a mídia sair do estado concluído, a nota **não é apagada**. Se voltar a ser concluída, a nota anterior volta a ser considerada, sem precisar ser cadastrada de novo.

### 6.7 Ranking

**RF36 — Ranking pessoal.** Rankings baseados nas avaliações do próprio usuário, **separados por tipo**: livros, filmes e séries. Não representam ranking global nem avaliação geral de usuários.

**RF37 — Critério de participação.** Uma mídia participa do ranking quando: pertence à biblioteca, está concluída e possui avaliação. Ao deixar de ser concluída, sai do ranking (a nota permanece armazenada).
- Ordenação: nota decrescente; em caso de empate, ordem alfabética do título (RN27).

### 6.8 Compartilhamento

**RF38 — Compartilhar lista.** O usuário pode gerar um link público para uma lista. Cada lista tem **no máximo um** link de compartilhamento (RN28).

**RF39 — Visualizar lista compartilhada.** Qualquer pessoa com o link visualiza a lista, somente leitura. O visitante não pode adicionar/remover mídias, alterar status ou avaliações, renomear nem excluir a lista.
- O visitante vê o **nome da lista** e os **dados das mídias** (título, tipo, ano, gênero, capa e sinopse);
- O visitante **não vê** status, nota nem dados pessoais do proprietário (como e-mail).

**RF40 — Lista compartilhada dinâmica.** O link aponta para a lista atual, não para uma cópia. Alterações do proprietário aparecem para quem acessar depois.

**RF41 — Sem expiração.** Na primeira versão, o link não tem prazo de expiração.

**RF42 — Desativar compartilhamento.** O proprietário pode ativar e desativar o compartilhamento de uma lista (interruptor simples).
- Com o compartilhamento desativado, o link deixa de exibir a lista;
- O proprietário pode reativar o compartilhamento depois;
- Regras adicionais de gerenciamento (ex.: revogar e gerar novo token) ficam para versões futuras.

### 6.9 Navegação e interface

**RF43 — Navegação principal.** Após o login, o usuário encontra: ⭐ Favoritos, 📋 Minhas listas, 🔎 Pesquisa, ➕ Criação de listas, 🎞️ Todas as mídias e 🏆 Ranking. A disposição visual será definida depois.

**RF44 — Emojis na interface.** A interface deve usar emojis em pontos apropriados (⭐ Favoritos, 📚 Livros, 🎬 Filmes, 📺 Séries, 🏆 Ranking, 🔎 Pesquisa). É decisão de UX, **não** regra de persistência do backend.

---

## 7. Regras de negócio

| Código | Regra |
|---|---|
| **RN01** | Não pode existir mais de um usuário com o mesmo e-mail. |
| **RN02** | Listas, avaliações, status e relações de biblioteca pertencem ao usuário proprietário. |
| **RN03** | Todo usuário possui exatamente uma lista Favoritos. |
| **RN04** | A lista Favoritos não pode ser excluída. |
| **RN05** | Toda mídia na biblioteca pertence a pelo menos uma lista do usuário. |
| **RN06** | Uma mídia pode estar em várias listas do mesmo usuário. |
| **RN07** | Para o mesmo usuário, a combinação mídia + lista tem no máximo uma associação. |
| **RN08** | Remover da última lista exige confirmação e remove a mídia da biblioteca. |
| **RN09** | Remover da biblioteca remove a mídia de todas as listas do usuário, tira-a do ranking e **apaga a avaliação associada ao registro da biblioteca**. |
| **RN10** | A remoção não impede que a mídia seja adicionada de novo no futuro (como nova associação). |
| **RN11** | Livros usam estados de leitura; filmes e séries usam estados de visualização. |
| **RN12** | O status é individual: representa a relação usuário + mídia, nunca uma propriedade global da mídia. |
| **RN13** | A avaliação é individual, de 1 a 10, e só é válida com a mídia concluída. |
| **RN14** | O usuário pode alterar status e avaliação depois (a avaliação só enquanto a mídia estiver concluída). |
| **RN15** | Mudar o status para não concluído **não** apaga a avaliação; ela só deixa de contar no ranking. |
| **RN16** | Só mídias concluídas e avaliadas participam do ranking. |
| **RN17** | O acesso por link compartilhado é somente leitura. |
| **RN18** | O conteúdo do link compartilhado reflete o estado atual da lista. |
| **RN19** | O link compartilhado não expira na primeira versão. |
| **RN20** | Ao criar a conta, a lista Favoritos é criada automaticamente. |
| **RN21** | A visualização pública de uma lista compartilhada expõe apenas o nome da lista e os dados das mídias; status, nota e dados do proprietário não são expostos. |
| **RN22** | Toda mídia é identificada de forma única pelo par (provedor externo, identificador externo). Edições ou versões diferentes de uma obra são mídias distintas. |
| **RN23** | Séries são tratadas como um todo: um único status e uma única nota por série, sem temporadas ou episódios na v1. |
| **RN24** | Excluir uma lista cujas mídias pertençam somente a ela exige confirmação; ao confirmar, essas mídias são removidas da biblioteca (com a nota, conforme RN09). Mídias presentes em outras listas permanecem. |
| **RN25** | A lista Favoritos não pode ser renomeada. |
| **RN26** | O nome de uma lista é único por usuário, sem diferenciar maiúsculas de minúsculas. Isso também impede outra lista com o nome "Favoritos". |
| **RN27** | O ranking é ordenado por nota decrescente e, em empate, pelo título em ordem alfabética. |
| **RN28** | Cada lista possui no máximo um compartilhamento (link), que pode ser ativado ou desativado. |

> **Nota sobre RN09 × RN15:** são situações distintas. Mudar de status preserva a nota; **remover a mídia da biblioteca** apaga a nota junto com o registro.

---

## 8. Requisitos não funcionais

| Código | Requisito |
|---|---|
| **RNF01 — Segurança** | Recursos privados protegidos por autenticação e autorização. |
| **RNF02 — Isolamento** | Toda operação valida o proprietário do recurso antes de leitura ou alteração. |
| **RNF03 — Senhas** | Nunca armazenadas em texto puro; usar hash apropriado. |
| **RNF04 — API REST** | Comunicação cliente-servidor baseada em princípios REST e HTTP. |
| **RNF05 — Documentação** | API documentada com OpenAPI/Swagger. |
| **RNF06 — Validação** | Dados recebidos validados antes de processados. |
| **RNF07 — Tratamento de erros** | Respostas HTTP coerentes para validação, autenticação, autorização, recurso inexistente e erro interno. |
| **RNF08 — Manutenibilidade** | Código em camadas: Controller → Service → Repository → Database. |
| **RNF09 — Testabilidade** | Principais regras de negócio cobertas por testes automatizados. |
| **RNF10 — Integração externa** | Integração com a fonte de mídias isolada, para que trocar o provedor cause o menor impacto possível. |
| **RNF11 — Containerização** | Suporte a execução com Docker. |
| **RNF12 — Simplicidade** | API simples (poucos usuários inicialmente), mas com separação entre autenticação, usuários, mídias, listas, biblioteca, status, avaliações, ranking e compartilhamento. |
| **RNF13 — Uso das APIs externas** | Atribuição ao TMDB na aplicação ("This product uses the TMDB API but is not endorsed or certified by TMDB"); `User-Agent` identificado com contato nas chamadas à Open Library; cache das respostas; uso não comercial enquanto o TMDB for a fonte de filmes e séries. |

### Tecnologias previstas
Java, Spring Boot, Spring Security, Spring Data JPA, Hibernate, MySQL, Maven, Swagger/OpenAPI e Docker. Podem ser refinadas na fase de arquitetura.

---

## 9. Modelo conceitual

O sistema separa a **mídia externa** do **relacionamento dela com o usuário**.

```text
User
 ├── Lists (inclui Favoritos)
 │     └── associação com UserMedia
 └── UserMedia
       ├── Media
       ├── Status
       └── Rating
```

**Media** (informações da obra), representada por **uma única entidade** com um campo de tipo (`BOOK`, `MOVIE`, `SERIES`):

```text
id, type, title, year, genre, synopsis, cover, provider, externalId
```

- Chave única: `(provider, externalId)`;
- Os metadados são persistidos localmente no momento em que o usuário adiciona a mídia à biblioteca; a pesquisa em si não grava nada.

**UserMedia** (relação do usuário com a mídia): `status` e `rating` pertencem aqui, nunca à Media.

```text
Duna
 ├── Usuário A → Lido → 9
 ├── Usuário B → Quero ler
 └── Usuário C → Lendo
```

**Relacionamentos:**

```text
User 1 ──── N List
User 1 ──── N UserMedia
Media 1 ─── N UserMedia
UserMedia N ──── N List   (uma mídia em várias listas, sem duplicar)
List 1 ──── N Share
```

**Integridade:**
- `UserMedia` sem nenhuma `List` é um estado inválido;
- Mesma mídia + mesma lista para o mesmo usuário: no máximo uma associação.

A modelagem física do banco será definida depois.

---

## 10. Estados das mídias

```text
Livro:           QUERO_LER      → LENDO      → LIDO
Filme / Série:   QUERO_ASSISTIR → ASSISTINDO → ASSISTIDO
```

O usuário pode alterar o estado depois; o fluxo **não** precisa ser linear.

> **Nota de implementação:** internamente o sistema usa três estados neutros (`WANT`, `IN_PROGRESS`, `DONE`); o nome exibido depende do tipo da mídia (`QUERO_LER`/`LENDO`/`LIDO` para livros e `QUERO_ASSISTIR`/`ASSISTINDO`/`ASSISTIDO` para filmes e séries). Assim é impossível gravar um estado inválido para o tipo.

---

## 11. Casos de uso e fluxos

### UC01 — Criar conta
**Ator:** Usuário
1. Acessa o cadastro e informa nome, e-mail, senha e confirmação;
2. Sistema valida os dados e a disponibilidade do e-mail;
3. Sistema cria o usuário;
4. Sistema cria automaticamente a lista Favoritos.

### UC02 — Realizar login
**Ator:** Usuário
1. Informa e-mail e senha;
2. Sistema valida as credenciais e autentica;
3. Sistema libera os recursos privados.

### UC03 — Pesquisar mídia
**Ator:** Usuário
1. Informa o nome (com autocomplete);
2. Sistema consulta a fonte externa e apresenta os resultados paginados;
3. Usuário aplica filtros e/ou consulta detalhes;
4. Se não houver resultados, sistema exibe a tela específica de ausência de resultado.

### UC04 — Adicionar mídia à biblioteca
**Ator:** Usuário

```text
Pesquisar → Resultados → Selecionar → Detalhes
   → Escolher lista(s) → "Já leu/assistiu?"
        ├── SIM → Concluído → Solicita avaliação (1–10) → Adiciona
        └── NÃO → Quero ler/assistir → Adiciona
```

Se a mídia já estiver na lista escolhida, o sistema apenas informa, sem duplicar.

### UC05 — Alterar status
**Ator:** Usuário
1. Acessa a mídia e escolhe alterar status;
2. Sistema apresenta os estados válidos para o tipo da mídia;
3. Usuário seleciona o novo status;
4. Sistema atualiza. Se o novo estado for concluído, pode solicitar avaliação;
5. Se a mídia deixar de ser concluída: sai do ranking e **mantém** a avaliação.

### UC06 — Avaliar mídia
**Ator:** Usuário — **Pré-condição:** mídia concluída
1. Acessa a mídia concluída e seleciona avaliação;
2. Informa valor de 1 a 10;
3. Sistema valida e salva ou atualiza a avaliação.

### UC07 — Criar lista
**Ator:** Usuário
1. Seleciona criar lista e informa o nome;
2. Sistema valida e cria (a lista pode ficar vazia);
3. Lista aparece entre as listas do usuário.

### UC08 — Remover mídia

```text
Usuário seleciona remover
   → Sistema verifica em quantas listas a mídia está
        ├── Em outras listas → remove apenas da lista
        └── Era a última lista → solicita confirmação
               ├── Confirmou → remove da biblioteca
               └── Cancelou → mantém
```

### UC09 — Compartilhar lista
**Ator:** Usuário
1. Abre a lista e solicita compartilhamento;
2. Sistema gera (ou ativa) o identificador de compartilhamento;
3. Sistema disponibiliza o link;
4. Usuário compartilha.

### UC10 — Visualizar lista compartilhada
**Ator:** Visitante
1. Acessa o link;
2. Sistema valida o compartilhamento e identifica a lista;
3. Sistema apresenta o conteúdo atual;
4. Visitante consulta as mídias, sem poder modificar dados.

### UC11 — Excluir lista
**Ator:** Usuário
1. Seleciona excluir uma lista personalizada (Favoritos não pode ser excluída);
2. Sistema identifica as mídias que pertencem **somente** a essa lista;
3. Se houver, informa quantas sairão da biblioteca e solicita confirmação;
4. Se o usuário confirmar, sistema remove essas mídias da biblioteca e exclui a lista (junto com o compartilhamento, se existir);
5. Se cancelar, nada é alterado.

---

## 12. Decisões pendentes

### 12.1 Decisões já fechadas

| Ponto | Decisão |
|---|---|
| **Filtro por tipo de mídia** | ✅ Incluído na v1 (RF14). |
| **Desativar link compartilhado** | ✅ Desativação simples incluída na v1 (RF42). |
| **Nota na remoção da biblioteca** | ✅ A nota é apagada junto com o registro da biblioteca (RN09). |
| **Visitante do link compartilhado** | ✅ Vê o nome da lista e os dados das mídias; não vê status, nota nem dados do proprietário (RF39, RN21). |
| **Modelo de Media** | ✅ Entidade única com campo de tipo; chave única `(provider, externalId)` (seção 9, RN22). |
| **Séries** | ✅ Tratadas como um todo, com um status e uma nota, sem temporadas/episódios na v1 (RN23). |
| **Excluir lista com mídias exclusivas** | ✅ Avisa quantas mídias sairão da biblioteca e exige confirmação (RF07, RN24, UC11). |
| **Status interno** | ✅ Estados neutros (`WANT`, `IN_PROGRESS`, `DONE`) com rótulo por tipo (seção 10). |
| **Renomear Favoritos** | ✅ Não permitido (RN25). |
| **Nome de lista repetido** | ✅ Não permitido por usuário, sem diferenciar maiúsculas (RN26). |
| **Desempate no ranking** | ✅ Nota decrescente e depois título (RN27). |
| **Compartilhamento por lista** | ✅ No máximo um link por lista, ativável e desativável (RN28). |
| **Autenticação** | ✅ JWT de 1 hora, sem refresh na v1. |
| **Senha** | ✅ De 8 a 72 caracteres, sem exigir símbolos (RF01). |
| **Paginação** | ✅ `page` a partir de 0; `size` padrão 20 e máximo 50 (20 na pesquisa). |
| **API (endpoints e contratos)** | ✅ Definidos em `api-endpoints-e-contratos.md`. |
| **Fonte externa de mídias** | ✅ **TMDB** (filmes e séries) + **Open Library** (livros), ambas gratuitas, atrás da interface `MediaProvider`. Sem filtro de tipo, a busca consulta as duas em paralelo e intercala os resultados. |
| **Filtros da pesquisa** | ✅ Tipo e ano aplicados pelo provedor; gênero e avaliação mínima como melhor esforço, filtrados sobre a página (que pode vir com menos itens que `size`). |

### 12.2 Decisões técnicas e funcionais

- **Identificação de mídia:** como tratar títulos semelhantes e livros de uma mesma franquia na pesquisa e na exibição (a identidade técnica já está definida na RN22);
- **Compartilhamento:** formato e política de segurança do token, comportamento após excluir a lista, estrutura do compartilhamento público;
- **Autorização:** modelo definitivo (a estratégia de token já está definida);
- **Cache** da API externa, incluindo o prazo de renovação dos metadados guardados, conforme os termos do TMDB;
- **Banco de dados:** modelagem definitiva;
- **Tratamento detalhado de erros**, arquitetura de backend, estratégia de testes, Docker e deploy.

> Essas decisões devem ser tomadas **depois** da consolidação dos requisitos, para que a tecnologia não determine as regras de negócio.

---

## 13. Critérios gerais de aceitação (v1)

- [ ] Criar conta e fazer login;
- [ ] Cada usuário tem sua própria biblioteca, isolada das demais;
- [ ] Cada usuário tem uma lista Favoritos automática, não excluível;
- [ ] Criar, renomear e excluir listas personalizadas (inclusive vazias), com nome único por usuário e confirmação ao excluir lista com mídias exclusivas;
- [ ] Favoritos não pode ser excluída nem renomeada;
- [ ] Pesquisar mídias com busca unificada, autocomplete, paginação e filtros (ano, gênero, avaliação e tipo);
- [ ] Tela específica quando não houver resultados;
- [ ] Adicionar mídias a uma ou mais listas;
- [ ] Mídia em múltiplas listas, sem duplicar na mesma lista;
- [ ] Mídia nunca permanece sem lista;
- [ ] Pergunta "já leu/assistiu?" ao adicionar, com avaliação obrigatória se concluída;
- [ ] Alterar status;
- [ ] Avaliar e alterar avaliação de mídias concluídas;
- [ ] Nota preservada ao sair de "concluído";
- [ ] Ranking por tipo considerando apenas mídias concluídas e avaliadas;
- [ ] Remover de uma lista e da biblioteca, com confirmação na última lista;
- [ ] Compartilhar listas por link, somente leitura e sempre atualizado;
- [ ] Ativar e desativar o compartilhamento de uma lista;
- [ ] API documentada com Swagger/OpenAPI;
- [ ] Principais regras de negócio com testes automatizados.

---

## 14. Ordem sugerida de desenvolvimento

1. Levantamento e validação de requisitos
2. Resolução das decisões pendentes
3. Modelagem do domínio
4. Regras de negócio
5. Modelagem do banco (MER)
6. Arquitetura da API, endpoints e contratos
7. Autenticação e usuários
8. Biblioteca e listas
9. Integração com API externa
10. Status e avaliações
11. Ranking
12. Compartilhamento
13. Validações e tratamento de erros
14. Testes
15. Documentação da API (Swagger/OpenAPI)
16. Docker e deploy

---

## 15. Regra para evolução do projeto

Novas funcionalidades não devem ser implementadas só porque parecem interessantes. Antes de adicionar, avaliar:

1. Qual problema ela resolve?
2. É uma regra de negócio ou apenas uma decisão de interface?
3. Altera alguma regra existente?
4. Afeta o modelo de dados?
5. Afeta outros fluxos?
6. Precisa entrar nos requisitos antes da implementação?

---

## Status

🚧 **Levantamento de requisitos / especificação** — documento a ser atualizado conforme novas decisões forem tomadas.
