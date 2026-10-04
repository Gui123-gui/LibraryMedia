# Tarefas — 005 Ranking e compartilhamento

Os fluxos principais estão implementados e têm testes de integração. Completar testes rastreáveis e resolver diferenças de contrato antes de ajustar respostas.

- [ ] Escrever ou adequar `cenario1_rankingIncluiConcluidasAvaliadasDoTipo`.
- [ ] Escrever ou adequar `cenario2_rankingOrdenaECalculaPosicaoGlobal`.
- [ ] Escrever ou adequar `cenario3_ativarCompartilhamentoGeraLink`.
- [ ] Escrever ou adequar `cenario4_reativarCompartilhamentoMantemToken`.
- [ ] Escrever ou adequar `cenario5_linkPublicoExibeSomenteListaAtual`.
- [ ] Escrever ou adequar `cenario6_linkRefleteAlteracoesDaLista`.
- [ ] Escrever ou adequar `cenario7_tokenInvalidoEInativoRetornamMesmo404`.
- [ ] Escrever ou adequar `cenario8_dtoPublicoNaoExpoeDadosPrivados`.
- [ ] Decidir e testar o formato da resposta do ranking: o contrato prevê `position` e `media` aninhado; o controller devolve `rank` e campos de mídia no nível superior.
- [ ] Decidir se `externalRating` deve ser exposto no DTO público; o contrato de lista pública não o lista.
- [ ] Manter o limite de requisições por IP como opcional até que seja aprovado como requisito.

