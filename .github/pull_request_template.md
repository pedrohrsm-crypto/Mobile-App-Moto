## O que muda
<!-- Uma ou duas frases. Se é um fix, afirme o defeito com número: "X dava 2,35:1 no tema claro". -->

## Frente / issue
<!-- front/<nome> · closes #NN -->

**Classe:** <!-- urgente | data-fixa | padrão | estrutural (09) -->
**Sessão:** <!-- <min> min · tokens <in>/<out> · US$ <custo> · <modelo>, do get_session; vira linha em dados/sessoes.csv -->

## Portões (números medidos nesta árvore)
- [ ] `lint`: exit 0 **e** saída vazia
- [ ] `typecheck`: 0
- [ ] testes unitários: N ✓ — cada teste novo foi **visto falhar** (bug plantado → vermelho → revertido)
- [ ] capturas: `verifyRoborazziDebug` verde e `app/src/test/snapshots` sem mudança não commitada (se houver interface)
- [ ] teto do AAB: `ferramentas/teto-aab.sh` → 0; subir o teto é decisão do dono (issue `decisao`) e vai com o motivo na mensagem do commit
- [ ] e2e: suíte inteira (sem arquivo fixo)
- [ ] tabela de rotas do build igual à baseline (ou a mudança explicada abaixo)

## Provas negativas
<!-- Uma linha por @Test novo, citado pelo NOME da função (o CI reprova nome ausente). Também cada passo novo do CI, visto vermelho no próprio CI, com o link do run. -->
| Teste ou passo | Bug plantado ou causa do vermelho | Visto vermelho (link do run ou saída) |
|---|---|---|

## Definition of Done
- [ ] Nenhum `error` descartado; toda escrita confere as linhas afetadas
- [ ] Nenhuma regra de negócio duplicada
- [ ] Nenhum hex literal ou `px` novo; catracas não subiram
- [ ] Captura visual nos temas × 140% × 360 px (se houver interface)
- [ ] Migration: aplicada **antes** do deploy e código tolerante ao schema antigo (se houver)
- [ ] Texto em pt-BR revisado; o que a interface promete é verdade
- [ ] Release note escrita pelo diff, não pelo plano
- [ ] Guia de uso, board e brief atualizados, com data

## Regra da segunda ocorrência
- [ ] Este bug **não** aconteceu antes, **ou** este PR inclui primitivo, guarda ou lint que impede a volta (label `guarda`)
- [ ] A guarda reprova também uma variante nova da mesma classe, não só o caso real (04, padrão 11)

## Itens desmarcados — motivo
<!-- Obrigatório para cada caixa acima deixada vazia. O PM não integra sem isto (nem sem o veredito `aprovar` do `pm-revisor`). -->
