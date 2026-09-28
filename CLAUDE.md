# Diário de Bordo (Mobile App Moto) — instruções para sessões de IA

Framework: v1.6 (https://github.com/pedrohrsm-crypto/framework-aprendido)

## Idioma
- Português do Brasil em código, commits, documentos e interface. Nunca variantes europeias ("registrado", não "registado"; "arquivo", não "ficheiro").
- Tratar o dono do produto por: Pedro.

## Stack e versões
- Android nativo em Kotlin, Jetpack Compose + Material 3, MVVM com Hilt, Coroutines/Flow, Room, WorkManager. Plano completo em `docs/plano-de-produto.md`.
- Versões fixadas em `gradle/libs.versions.toml` (fonte única). AGP 9 com Kotlin embutido: **não** aplicar `org.jetbrains.kotlin.android` nem `kapt`; processadores de anotação vão por KSP.
- minSdk 26 (Android 8), targetSdk 36 (exigência da Play desde 31/08/2026), compileSdk 37. JDK 21 na bancada e no CI.
- Offline-first: o Room é a fonte da verdade. Nada no MVP depende de rede.
- Um módulo `:app`, pacotes por funcionalidade (`garagem`, `diario`, `plano`, `numeros`, `localizacao`) e `core` para o que é compartilhado.

## Papéis de trabalho
- **Maestro** (uma sessão): integra, faz merge em `main`, aplica migrations, faz deploy, mantém o board. Só ele toca `main` e produção.
- **Frente** (N sessões): trabalha só na própria branch `front/NOME-DA-FRENTE`, em worktree própria. Nunca faz merge nem deploy.
- Um escritor por branch. Decisão do dono registrada não se reabre.

## Portões (rodar separados, nunca com `| tail`; o CI roda os mesmos, nesta ordem)
1. `bash ferramentas/guardas.sh` → 0 (idioma pt-BR, cor só do tema, nenhum texto fixo)
2. `ktlint "**/*.kt" "**/*.kts"` → 0 (configuração em `.editorconfig`)
3. `./gradlew -p buildSrc test` → verde
4. `./gradlew lintDebug` → 0
5. `./gradlew assembleDebug` → 0 (compila Kotlin, KSP de Room e Hilt)
6. `./gradlew testDebugUnitTest` → verde; teste novo precisa ter sido visto falhar
7. `./gradlew verificarManifestoRelease` → 0 (componente exportado entra na lista em `app/build.gradle.kts` com motivo)
8. `./gradlew bundleRelease` → 0
9. Visual: 3 temas (Oficina, Estrada, Cromo) × fonte 140% × tela de 360 dp quando houver interface

## Regras inegociáveis
- Nunca descartar `error`. Escrita confere as linhas afetadas.
- Migration **antes** do deploy, e o código tolera o schema antigo.
- Mudança de entidade do Room sobe a versão do banco, com `Migration` e o schema exportado em `app/schemas/` commitado.
- Localização só com permissão concedida na hora do uso; o app funciona sem ela.
- Uma regra de negócio mora em um lugar só.
- Toda tela usa `TelaDiario` (edge-to-edge, insets pelo `PaddingValues`); formulário usa `FormularioDiario` (rola e sobe com o teclado).
- Toda cor vem do `ColorScheme` com par nos 3 temas; tamanhos em `dp`/`sp`, nunca px.
- Na **segunda** ocorrência de um bug, criar primitivo, guarda ou lint (procedimento PFA-01, 2.3).
- Todo número citado vem de um comando rodado, com data e SHA.
- Versão nova de dependência só pelo `libs.versions.toml`.
- Commit no formato `tipo(frente): texto (#issue)` — o tipo (`feat`, `fix`, `docs`…) alimenta as métricas; a issue dá rastreabilidade.
- Nada entra na `main` sem PR e sem os checks verdes.

## Gestão (Kanban + PMBOK 8 — seção 09 do framework; detalhe em `docs/GESTAO.md`)
- Fluxo: Opções → Com evidência → Pronta (ponto de compromisso) → Execução (WIP 3) → Integração (WIP 2) → Validação com cliente (WIP 20) → Concluída. Puxar só com espaço no limite.
- Classes de serviço: `urgente` (fura o WIP, máx. 1), `data-fixa`, padrão, `estrutural` (~20% reservados).
- Riscos e decisões são issues (`risco`, `decisao`). Decisão só se reabre com nova issue que cite a anterior.
- Ao iniciar uma sessão: olhar itens envelhecendo e bloqueados. Todo item concluído passa pela validação com cliente, ou registra por que dispensa.

## Fim de trabalho
Toda sessão termina com o bloco `=== FIM DE TRABALHO ===` (seção 7 do 05 do framework), ou com `=== NÃO RODE /clear AINDA ===` se algo faltar.

## Desvios deste framework (motivo obrigatório)
- Portões `typecheck` e `e2e` do kit viram `assembleDebug` (o compilador Kotlin faz o typecheck). Testes instrumentados entram quando houver telas reais; até lá, sem emulador no CI.
- Branch de trabalho segue o nome dado pela sessão (`claude/...`) em vez de `front/NOME`, porque o ambiente das sessões impõe o nome.
