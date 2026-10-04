# Tarefas — 004 Biblioteca, status e avaliação

O backend já implementa operações de biblioteca. Confirmar primeiro a conformidade dos payloads e status HTTP com o contrato aprovado; não refatorar comportamento sem testes de cenário.

- [ ] Escrever ou adequar `cenario1_adicionarNaoConsumida`.
- [ ] Escrever ou adequar `cenario2_consumidaExigeNota`.
- [ ] Escrever ou adequar `cenario3_adicionarConcluidaComNota`.
- [ ] Escrever ou adequar `cenario4_readicionarPreservaStatusENota`.
- [ ] Escrever ou adequar `cenario5_listaAlheiaRetorna404`.
- [ ] Escrever ou adequar `cenario6_associacaoNaoDuplica`.
- [ ] Escrever ou adequar `cenario7_consultarBibliotecaComFiltros`.
- [ ] Escrever ou adequar `cenario8_mudarStatusPreservaNota`.
- [ ] Escrever ou adequar `cenario9_avaliarExigeConclusao`.
- [ ] Escrever ou adequar `cenario10_removerUmaListaPreservaBiblioteca`.
- [ ] Escrever ou adequar `cenario11_ultimaListaExigeConfirmacao`.
- [ ] Escrever ou adequar `cenario12_confirmarUltimaListaApagaEntradaENota`.
- [ ] Escrever ou adequar `cenario13_remocaoDaBibliotecaExigeConfirmacao`.
- [ ] Escrever ou adequar `cenario14_remocoesConcorrentesNaoOrfanamEntrada`.
- [ ] Decidir e testar o retorno de `POST /library`: o contrato pede a entrada completa com `addedToLists`/`alreadyInLists`; a implementação devolve um conjunto menor de campos.
- [ ] Decidir e testar o status HTTP de exclusão: o contrato pede 204, enquanto a implementação devolve corpo.
- [ ] Validar migration e concorrência em MySQL real via Testcontainers; a execução atual foi ignorada sem Docker.

