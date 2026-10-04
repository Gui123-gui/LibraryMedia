# Tarefas — 001 Autenticação e usuários

O fluxo principal já existe no backend. Estas tarefas reconciliam a implementação com SDD; não reimplementar endpoints existentes. Uma regra só é aceita no fluxo SDD quando há teste `cenarioN_descricaoEmCamelCase` e o resultado corresponde à spec.

- [x] Criar `cenario1_cadastroValidoCriaFavoritos`, verificando resposta 201 e nome `Favoritos`.
- [ ] Criar `cenario3_confirmacaoSenhaDivergente`; há teste equivalente, mas ele não tem o nome padronizado.
- [ ] Criar `cenario10_rotaPrivadaSemToken`; há teste equivalente, mas ele não tem o nome padronizado.
- [ ] Criar `cenario12_consultaUsuarioDoToken`; há teste equivalente, mas ele não tem o nome padronizado.
- [ ] Escrever `cenario2_emailDuplicadoCaseInsensitive` cobrindo variação de maiúsculas/minúsculas.
- [ ] Escrever `cenario4_senhaAbaixoDoMinimo` para senha menor que 8 caracteres.
- [ ] Escrever `cenario5_senhaAcimaDoMaximo` para mais de 72 caracteres; incluir caso multibyte após definir a regra.
- [ ] Escrever `cenario6_nomeInvalido` para nome vazio e acima de 100 caracteres.
- [ ] Escrever `cenario7_falhaFavoritosReverteCadastro` verificando que nenhum usuário permanece persistido.
- [ ] Escrever `cenario8_loginValidoEmiteToken` exigindo `expiresIn` exatamente igual a 3600.
- [ ] Escrever `cenario9_credenciaisInvalidasRespostaGenerica` para e-mail inexistente e senha errada, comparando status e código.
- [ ] Escrever `cenario11_rotaPrivadaTokenExpirado` verificando 401 `UNAUTHORIZED`.
- [ ] Renomear ou substituir os testes de autenticação existentes para que todo teste cite o cenário no nome.
- [ ] Executar cenários de persistência/transação em MySQL real via Testcontainers; o teste de schema atual foi ignorado por falta de Docker.
- [ ] Extrair os casos de uso de autenticação do controller para services conforme RNF08, preservando os contratos aprovados.
