# 006 — Interface web

## Objetivo

Permitir que usuários acessem os fluxos da biblioteca pelo frontend React, consumindo os contratos da API e respeitando autenticação, isolamento e confirmações.

## Requisitos relacionados

- RF01–RF44 — cadastro/acesso, listas, pesquisa, biblioteca, status, avaliações, ranking, compartilhamento e navegação.
- RN01–RN28 — regras do domínio refletidas pela interface sem substituí-las por validação apenas no cliente.
- RNF01, RNF04–RNF07, RNF09, RNF11, RNF13 — segurança, API, documentação, validação, erros, testabilidade, containerização e atribuição a provedores.
- UC01–UC11 — fluxos de cadastro, login, pesquisa, biblioteca, status, avaliação, listas, remoção e compartilhamento.

## Cenários de aceitação

### Cenário 1 — Cadastro e autenticação visual

**Dado** que o visitante está na tela pública de cadastro  
**Quando** informa os dados válidos e envia o formulário  
**Então** o frontend chama o contrato de cadastro, mostra o resultado da operação e permite iniciar sessão sem expor a senha.

### Cenário 2 — Login e acesso privado

**Dado** que o visitante informa credenciais válidas  
**Quando** a API autentica e devolve access token  
**Então** o frontend envia o token nas requisições privadas e permite navegar às áreas autenticadas.

### Cenário 3 — Sessão não autenticada ou expirada

**Dado** que o usuário não possui token válido  
**Quando** tenta abrir uma rota privada ou uma chamada recebe 401 `UNAUTHORIZED`  
**Então** o frontend não exibe dados privados e encaminha o usuário ao login.

### Cenário 4 — Navegação principal

**Dado** que o usuário está autenticado  
**Quando** acessa a navegação principal  
**Então** encontra Favoritos, Minhas listas, Pesquisa, criação de listas, Todas as mídias e Ranking.

### Cenário 5 — Gerenciar listas e confirmar operações

**Dado** que o usuário está autenticado  
**Quando** cria, renomeia ou solicita a exclusão de uma lista  
**Então** a interface chama a API, apresenta erros de regra e só repete uma exclusão destrutiva depois de confirmação explícita.

### Cenário 6 — Pesquisar e reconhecer ausência de resultados

**Dado** que o usuário informa um termo de busca  
**Quando** aplica filtros e navega páginas  
**Então** a interface mostra resultados, estados de carregamento/erro e uma tela de ausência quando `content` está vazio; sugestões usam debounce aproximado de 300 ms.

Ao pedir detalhes de um resultado, o cliente consulta `GET /api/v1/media/{provider}/{externalId}` e apresenta os campos disponíveis, incluindo sinopse.

### Cenário 7 — Adicionar mídia

**Dado** que o usuário escolheu uma mídia pesquisada  
**Quando** seleciona uma ou mais listas e informa se já consumiu a obra  
**Então** a interface envia os dados previstos pela API e exige nota de 1 a 10 quando a mídia é adicionada como concluída.

### Cenário 8 — Atualizar biblioteca, status e nota

**Dado** que o usuário está consultando uma entrada própria  
**Quando** altera status ou nota ou solicita remoção  
**Então** a interface respeita as regras da API, apresenta conflitos e pede confirmação antes de remover da última lista ou da biblioteca.

### Cenário 9 — Consultar rankings

**Dado** que o usuário escolhe livros, filmes ou séries  
**Quando** consulta seu ranking  
**Então** a interface apresenta a página retornada pela API sem alterar a ordem definida pelo servidor.

### Cenário 10 — Compartilhar uma lista

**Dado** que o usuário é proprietário de uma lista  
**Quando** ativa ou desativa o compartilhamento  
**Então** a interface mostra o link quando ativo e informa que a página pública é somente leitura.

### Cenário 11 — Abrir lista pública

**Dado** que um visitante tem um link de compartilhamento ativo  
**Quando** acessa a página pública  
**Então** vê o nome e as mídias da lista, sem dados pessoais do proprietário, status ou nota pessoal.

### Cenário 12 — Erros, carregamento e atribuição

**Dado** que há uma chamada em andamento, falha de API ou conteúdo de provedor externo  
**Quando** a interface renderiza o estado correspondente  
**Então** todas as telas têm estados de carregamento e erro e exibem a atribuição obrigatória ao TMDB.

## Rastreabilidade

| Cenário | Requisitos | Teste (a criar) |
|---|---|---|
| 1 | RF01, RF08, UC01 | `cenario1_cadastroComFavoritos` |
| 2 | RF02, RNF01, UC02 | `cenario2_loginAbreAreaPrivada` |
| 3 | RF02, RF03, RNF01, UC02 | `cenario3_sessaoAusenteOuExpirada` |
| 4 | RF43, RF44 | `cenario4_navegacaoPrincipal` |
| 5 | RF04–RF08, RF24, RN04, RN24–RN26, UC07, UC11 | `cenario5_gerenciarListasComConfirmacao` |
| 6 | RF11–RF16, UC03 | `cenario6_buscaFiltrosPaginacaoESemResultados`, `cenario6_exibirDetalhesDaMidia` |
| 7 | RF17–RF21, RF29, RF33, RN05–RN07, UC04 | `cenario7_adicionarMidiaALista` |
| 8 | RF22–RF35, RN08–RN15, UC05, UC06, UC08 | `cenario8_atualizarBibliotecaEConfirmarRemocao` |
| 9 | RF36, RF37, RN16, RN27 | `cenario9_consultarRankingPessoal` |
| 10 | RF38, RF41, RF42, RN17–RN19, RN28, UC09 | `cenario10_gerenciarLinkCompartilhado` |
| 11 | RF39, RF40, RN21, UC10 | `cenario11_visualizarListaPublicaSomenteLeitura` |
| 12 | RNF05–RNF07, RNF09, RNF13 | `cenario12_exibirEstadosEAtribuicao` |

## Decisões tomadas para a primeira fatia

- Access token em `sessionStorage`, conforme escolha do usuário; a sessão encerra ao fechar a aba.
- `fetch` nativo como cliente HTTP, evitando dependência adicional.
- Vitest e React Testing Library para testes de componentes e fluxos.
- Barra superior com conteúdo responsivo como padrão inicial de navegação.
- O cadastro segue o contrato atual: depois do 201, a pessoa faz login explicitamente; o endpoint de cadastro não emite token.

- O frontend depende da resolução das divergências de contrato listadas em `docs/plano-checklist.md`; não deve implementar payloads em desacordo com a spec aprovada.

## Fora do escopo

- Criar regras de negócio no cliente ou substituir validações da API.
- Expor ao visitante público status, avaliação pessoal ou dados do proprietário.
- Alterar os contratos da API sem atualizar a spec correspondente.
