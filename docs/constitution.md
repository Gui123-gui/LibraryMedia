# Constituição do projeto

Princípios aplicáveis a todas as funcionalidades do Personal Media Library, derivados de `requisitos.md`, `modelo-dominio-e-mer.md`, `api-endpoints-e-contratos.md` e `plano-checklist.md`.

1. **Camadas e módulos.** Organizar o monólito em Controller → Service → Repository → Database e nos módulos `auth`, `user`, `media`, `list`, `library`, `ranking`, `sharing` e `common` (RNF08, RNF12).
2. **Isolamento.** Toda operação privada filtra pelo proprietário; o `userId` vem do token, nunca da URL ou do corpo. Recurso de outro usuário responde 404 (RF03, RNF02).
3. **Confirmação destrutiva.** A API exige `?confirm=true` nas operações destrutivas especificadas; sem confirmação retorna 409 e não apaga dados (contratos da API; RF23, RF24, RN08, RN24).
4. **Erros.** Responder erros com `ProblemDetail` conforme RFC 9457 e um código do catálogo da API (RNF07; contrato da API).
5. **Provedores externos.** Acessar TMDB e Open Library somente através da abstração `MediaProvider` (RNF10; modelo e plano).
6. **Senhas.** Nunca armazenar senha em texto puro; usar BCrypt conforme a decisão técnica do plano (RNF03).
7. **Testes.** Cobrir regras de negócio importantes com testes automatizados; os testes de banco usam Testcontainers com MySQL real (RNF09; modelo e plano).
8. **Legibilidade.** Preferir código simples, nomes claros e métodos curtos à otimização prematura. Comentários devem explicar o porquê de regras de negócio. O autor deve conseguir entender e revisar o código gerado (RNF08, RNF12; princípio de trabalho do projeto).
9. **Spec antes do comportamento.** Mudanças de comportamento começam na especificação. Se código e spec divergirem, corrigir o código ou atualizar a spec antes de tratar o comportamento como definitivo (regra de evolução em `requisitos.md`).
