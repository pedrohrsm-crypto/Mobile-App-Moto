## O que muda
<!-- Uma ou duas frases. Se é um fix, afirme o defeito com número: "X dava 2,35:1 no tema claro". -->

## Frente / issue
<!-- front/<nome> · closes #NN -->

## Portões (números medidos nesta árvore)
- [ ] Guardas e ktlint: 0
- [ ] `./gradlew lintDebug`: 0
- [ ] `./gradlew assembleDebug`: 0
- [ ] `./gradlew testDebugUnitTest` e `-p buildSrc test`: N ✓ — cada teste novo foi **visto falhar** (bug plantado → vermelho → revertido)
- [ ] `./gradlew verificarManifestoRelease` e `bundleRelease`: 0

## Definition of Done
- [ ] Nenhum `error` descartado; toda escrita confere as linhas afetadas
- [ ] Nenhuma regra de negócio duplicada
- [ ] Nenhuma cor fora do `ColorScheme` nem `px` novo; catracas não subiram
- [ ] Captura visual nos 3 temas × fonte 140% × 360 dp (se houver interface)
- [ ] Banco: versão do Room subiu, `Migration` escrita e schema em `app/schemas/` commitado (se houver)
- [ ] Texto em pt-BR revisado; o que a interface promete é verdade
- [ ] Release note escrita pelo diff, não pelo plano
- [ ] Guia de uso, board e brief atualizados, com data

## Regra da segunda ocorrência
- [ ] Este bug **não** aconteceu antes, **ou** este PR inclui primitivo, guarda ou lint que impede a volta (label `guarda`)

## Itens desmarcados — motivo
<!-- Obrigatório para cada caixa acima deixada vazia. O Maestro não integra sem isto. -->
