# Plano técnico — 006 Interface web

O frontend atual em `frontend/` é um esqueleto Vite + React + TypeScript, com uma página estática em `src/App.tsx`. Este plano descreve a evolução especificada; não autoriza desviar dos cenários de `spec.md`.

## Componentes previstos

- Organizar o app React em rotas públicas (cadastro, login e lista compartilhada) e privadas (listas, pesquisa, biblioteca e rankings).
- Criar cliente para `/api/v1` que centralize JSON, Bearer token e erros `ProblemDetail`.
- Criar telas e componentes para os fluxos de cadastro/login, listas, busca, detalhes, adição, biblioteca, ranking e compartilhamento.
- Manter a página pública em estado somente leitura e sem campos de dados privados.
- Implementar estados de carregamento, erro, lista vazia e confirmação nos pontos definidos pela spec.
- Exibir a atribuição exigida pelo TMDB.
- Usar os scripts e estrutura de Vite já existentes; o frontend já tem Dockerfile e integração com Compose.

## Decisões técnicas aprovadas para a primeira fatia

- Guardar access token em `sessionStorage`; não persistir senha ou segredo adicional.
- Usar `fetch` nativo para chamadas JSON e Bearer token.
- Adotar Vitest e React Testing Library para testes.
- Usar barra superior responsiva como navegação inicial.
- Cadastro termina com sucesso e instrução para login, pois o contrato de `POST /auth/register` não retorna token.

A implementação deverá lidar com 401 apagando a sessão local e retornando à tela de login. Divergências mais amplas de API listadas em `docs/plano-checklist.md` permanecem fora da primeira fatia se não forem acionadas pelos fluxos auth.
