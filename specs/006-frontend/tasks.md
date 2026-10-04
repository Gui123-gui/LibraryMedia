# Tarefas — 006 Interface web

- [x] Resolver decisões de token, cliente HTTP, runner de testes e navegação visual para a primeira fatia.
- [x] Gerar e versionar tipos TypeScript a partir do OpenAPI da API.
- [x] Escrever teste `cenario1_cadastroComFavoritos` para o fluxo de cadastro e resposta da API (cenário 1).
- [x] Implementar tela e fluxo de cadastro conforme contrato aprovado (cenário 1).
- [x] Escrever teste `cenario2_loginAbreAreaPrivada` (cenário 2).
- [x] Implementar login e cliente autenticado com o mecanismo de token aprovado (cenário 2).
- [x] Escrever teste `cenario3_sessaoAusenteOuExpirada` (cenário 3).
- [x] Implementar guarda da área privada, expiração da sessão e tratamento 401 (cenário 3).
- [x] Escrever teste `cenario4_navegacaoPrincipal` (cenário 4).
- [x] Implementar navegação inicial para as áreas autenticadas (cenário 4; as telas de cada área seguem pendentes).
- [x] Escrever teste `cenario5_gerenciarListasComConfirmacao` (cenário 5).
- [x] Implementar telas de listas e fluxo de confirmação baseado nos erros 409 da API (cenário 5).
- [x] Escrever teste `cenario6_buscaFiltrosPaginacaoESemResultados` (cenário 6).
- [x] Implementar busca, sugestões, filtros, paginação e estados vazio/carregamento/erro (cenário 6).
- [x] Escrever `cenario6_exibirDetalhesDaMidia` e implementar a consulta de detalhes completos (RF16).
- [x] Escrever teste `cenario7_adicionarMidiaALista` (cenário 7).
- [x] Implementar fluxo de seleção de listas, pergunta de consumo e nota quando concluída (cenário 7).
- [x] Escrever teste `cenario8_atualizarBibliotecaEConfirmarRemocao` (cenário 8).
- [x] Implementar tela da biblioteca e operações de status, nota e remoção (cenário 8).
- [x] Escrever teste `cenario9_consultarRankingPessoal` (cenário 9).
- [x] Implementar consulta paginada dos rankings por tipo (cenário 9).
- [x] Escrever teste `cenario10_gerenciarLinkCompartilhado` (cenário 10).
- [x] Implementar ativação/desativação e apresentação do link (cenário 10).
- [x] Escrever teste `cenario11_visualizarListaPublicaSomenteLeitura` (cenário 11).
- [x] Implementar página pública de lista com campos estritamente permitidos (cenário 11).
- [x] Escrever teste `cenario12_exibirEstadosEAtribuicao` (cenário 12).
- [x] Implementar estados comuns de erro/carregamento e atribuição TMDB em toda a aplicação (cenário 12).
- [x] Integrar frontend e API via Docker Compose depois que os fluxos e contratos aprovados estiverem concluídos.

> MySQL 8.4, API e Nginx foram iniciados via Compose. Cadastro/login, lista Favoritos, pesquisa/detalhes TMDB e compartilhamento (ativar, leitura pública e desativar) foram exercitados. MySQL publica em `3307` no host porque `3306` já estava ocupada.
