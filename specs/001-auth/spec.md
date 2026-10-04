# 001 — Autenticação e usuários

## Objetivo

Permitir que uma pessoa crie uma conta e entre com e-mail e senha.  
Proteger os recursos privados e identificar o usuário autenticado pelo token.

## Requisitos cobertos

RF01, RF02, RF03, RF08, RN01, RN03, RN20, RNF03, UC01, UC02.

## Cenários de aceitação

### Cenário 1 — Cadastro válido cria Favoritos

**Dado** que o e-mail ainda não está cadastrado e os dados são válidos  
**Quando** o cliente envia `POST /api/v1/auth/register`  
**Então** a API responde 201 com os dados públicos da conta e cria exatamente uma lista Favoritos na mesma transação.

### Cenário 2 — E-mail duplicado não diferencia maiúsculas

**Dado** que já existe uma conta com determinado e-mail  
**Quando** é enviado cadastro com o mesmo e-mail, inclusive com diferenças apenas entre maiúsculas e minúsculas  
**Então** a API responde 409 com `EMAIL_ALREADY_REGISTERED` e não cria outra conta.

### Cenário 3 — Confirmação de senha diferente

**Dado** que `passwordConfirmation` não corresponde a `password`  
**Quando** o cliente envia o cadastro  
**Então** a API responde 400 com `VALIDATION_ERROR` e não cria a conta.

### Cenário 4 — Senha abaixo do mínimo

**Dado** que a senha tem menos de 8 caracteres  
**Quando** o cliente envia o cadastro  
**Então** a API responde 400 com `VALIDATION_ERROR` e não cria a conta.

### Cenário 5 — Senha acima do máximo

**Dado** que a senha tem mais de 72 caracteres  
**Quando** o cliente envia o cadastro  
**Então** a API responde 400 com `VALIDATION_ERROR` e não cria a conta.

### Cenário 6 — Nome inválido

**Dado** que o nome está vazio ou tem mais de 100 caracteres  
**Quando** o cliente envia o cadastro  
**Então** a API responde 400 com `VALIDATION_ERROR` e não cria a conta.

### Cenário 7 — Falha ao criar Favoritos reverte o cadastro

**Dado** que ocorre falha ao persistir a lista Favoritos durante o cadastro  
**Quando** o cliente envia cadastro válido  
**Então** a transação é revertida e nenhum usuário fica gravado.

### Cenário 8 — Login válido

**Dado** que existem credenciais válidas  
**Quando** o cliente envia `POST /api/v1/auth/login`  
**Então** a API responde 200 com `accessToken`, `tokenType` igual a `Bearer` e `expiresIn` igual a `3600`.

### Cenário 9 — Credenciais inválidas têm resposta indistinguível

**Dado** um e-mail inexistente ou uma senha incorreta  
**Quando** o cliente tenta entrar  
**Então** em ambos os casos a API responde 401 com `INVALID_CREDENTIALS`, sem revelar qual dado falhou.

### Cenário 10 — Rota privada sem token

**Dado** que a requisição não possui token  
**Quando** o cliente acessa uma rota privada  
**Então** a API responde 401 com `UNAUTHORIZED`.

### Cenário 11 — Rota privada com token expirado

**Dado** que a requisição possui token expirado  
**Quando** o cliente acessa uma rota privada  
**Então** a API responde 401 com `UNAUTHORIZED`.

### Cenário 12 — Consultar o usuário autenticado

**Dado** que a requisição possui token válido  
**Quando** o cliente envia `GET /api/v1/users/me`  
**Então** a API responde 200 com `id`, `name` e `email` do usuário identificado pelo token.

## Contratos

Não duplicar exemplos de JSON. Consultar as seções **1**, **3.5**, **4** e **10** de `docs/api-endpoints-e-contratos.md`.

## Fora do escopo

- Refresh token.
- Recuperação de senha.
- Confirmação de e-mail.
- Logout.

## Rastreabilidade

| Cenário | Requisitos | Teste (a criar) |
|---|---|---|
| 1 — Cadastro válido e Favoritos atômicos | RF01, RF08, RN03, RN20, UC01 | `cenario1_cadastroValidoCriaFavoritos` |
| 2 — E-mail duplicado sem diferenciar caixa | RF01, RN01 | `cenario2_emailDuplicadoCaseInsensitive` |
| 3 — Confirmação divergente | RF01, UC01 | `cenario3_confirmacaoSenhaDivergente` |
| 4 — Senha menor que 8 | RF01 | `cenario4_senhaAbaixoDoMinimo` |
| 5 — Senha maior que 72 | RF01 | `cenario5_senhaAcimaDoMaximo` |
| 6 — Nome vazio ou acima de 100 | RF01 | `cenario6_nomeInvalido` |
| 7 — Falha ao criar Favoritos reverte conta | RF08, RN20, UC01 | `cenario7_falhaFavoritosReverteCadastro` |
| 8 — Login válido com token de uma hora | RF02, UC02 | `cenario8_loginValidoEmiteToken` |
| 9 — E-mail ausente e senha incorreta indistinguíveis | RF02, UC02 | `cenario9_credenciaisInvalidasRespostaGenerica` |
| 10 — Rota privada sem token | RF02, RF03, RNF03 | `cenario10_rotaPrivadaSemToken` |
| 11 — Rota privada com token expirado | RF02, RF03 | `cenario11_rotaPrivadaTokenExpirado` |
| 12 — Dados do usuário do token | RF03, UC02 | `cenario12_consultaUsuarioDoToken` |
