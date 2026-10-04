# Plano técnico — 001 Autenticação e usuários

Este plano descreve trabalho futuro. Não criar Java ou migration durante a preparação documental.

## Componentes a criar

- `auth`: controller para cadastro e login; service para coordenar os casos de uso; DTOs de entrada e saída para os contratos da API.
- `user`: entidade `User` e repository para buscar usuário por e-mail e persistir seus dados.
- `list`: criação da lista Favoritos durante o cadastro, na mesma transação.
- `common`: serviço JWT, filtro de autenticação, configuração de segurança e tratamento global de erros baseado em `ProblemDetail`.
- Validação dos DTOs de entrada para nome, e-mail, senha e confirmação.
- Testes de unidade e integração que referenciem os cenários pelo padrão definido na spec.

## Banco de dados

A implementação futura cria `V1__create_tables.sql` usando o DDL da seção 4 de `docs/modelo-dominio-e-mer.md`. Essa migration não faz parte desta tarefa documental. A fonte do modelo fornecida na conversa ainda não está no checkout.

## Decisões técnicas

| Decisão | Justificativa |
|---|---|
| Usar BCrypt para armazenar senhas. | RNF03 proíbe texto puro e o checklist da fase 2 escolhe BCrypt. |
| Emitir access token JWT com validade de 1 hora e sem refresh na v1. | A seção 10 do contrato fecha essa duração e exclui refresh. |
| Obter a identidade privada exclusivamente da autenticação do token. | RF03 e RNF02 exigem isolamento do proprietário. |
| Criar usuário e Favoritos em uma única transação. | UC01 e RN20 exigem que todo usuário tenha Favoritos automaticamente. |
| Normalizar e-mail antes de persistir e comparar. | O modelo determina que variações de maiúsculas não criem contas diferentes. |
| Responder erros com `ProblemDetail` e código do catálogo. | RNF07 e contrato da API definem um formato uniforme e códigos verificáveis. |
| Manter controllers, services e repositories em camadas. | RNF08 separa entrada HTTP, caso de uso e acesso a dados. |
| Usar testes de integração com MySQL via Testcontainers para comportamento dependente do banco. | O checklist do projeto prevê validar o mapeamento contra MySQL real. |

## Pontos ainda não definidos e divergências observadas

- `[A DEFINIR]` A spec exige senha de 8–72 caracteres, mas BCrypt limita a entrada a 72 bytes e o código atual também valida o número de bytes UTF-8. Decidir/documentar o comportamento para senhas multibyte.
- A lista automática usa o nome `Favoritos`, conforme RF08/RN20 e contrato; o backend foi alinhado a esse nome.
- `[A DEFINIR]` A seção 12.2 de requisitos deixa arquitetura detalhada, estratégia de testes e tratamento de erros como decisões técnicas pendentes. A presente spec fixa os resultados já descritos no contrato.
- `[A DEFINIR]` Detalhes de configuração operacional e rotação do segredo JWT não constam dos cenários de autenticação.

## Estado do código existente

Cadastro, login, JWT, BCrypt, filtro, configuração de segurança e `/users/me` já existem. A implementação está concentrada em `AuthController`, sem service de autenticação; isso diverge do princípio em camadas. O teste de cadastro, login e rota privada passa, mas não verifica todos os cenários desta spec.
