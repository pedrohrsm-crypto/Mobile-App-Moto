# Diário de Bordo (Mobile-App-Moto) — instruções para sessões de IA


Framework: v2.0 (https://github.com/pedrohrsm-crypto/framework-aprendido)

## Idioma
- Português do Brasil em código, commits, documentos e interface. Nunca variantes europeias ("registrado", não "registado"; "arquivo", não "ficheiro"; "salvar", não "guardar").
- Tratar o dono do produto por: "o senhor" (Pedro).

## Stack e versões
- Android nativo em Kotlin, Jetpack Compose + Material 3, MVVM com Hilt, Coroutines/Flow, Room, WorkManager. Plano em `docs/plano-de-produto.md` e `docs/offline-first.md`.
- Versões fixadas em `gradle/libs.versions.toml` (fonte única). AGP 9 com Kotlin embutido: **não** aplicar `org.jetbrains.kotlin.android` nem `kapt`; processadores de anotação vão por KSP. **Leia a doc da versão instalada** antes de usar uma API; a memória e o brief não são fonte.
- minSdk 26 (Android 8), targetSdk 36 (exigência da Play desde 31/08/2026), compileSdk 37. JDK 21 na bancada e no CI.
- Offline-first: o Room é a fonte da verdade. Nada no MVP depende de rede. Sem servidor, sem banco remoto e sem pagamento no MVP (nuvem e cobrança só na fase 6).
- Um módulo `:app`, pacotes por funcionalidade (`garagem`, `diario`, `plano`, `numeros`, `localizacao`, `rota`, `backup`) e `core` para o que é compartilhado.

## Papéis de trabalho (seção 18 do framework; Maestro → PM → agentes e analistas → subagentes)
- **PM do portfólio** (uma sessão para os MVPs do portfólio: este e a `loja-online`; exceção ao 18 §5, regra 1, decidida pelo dono em 26-Out-2026): lidera a equipe do projeto e é o **único interlocutor do dono** (D-PM6). Abre e fecha a sprint semanal e a apresenta ao dono (relatório + board); delega frentes com brief; revisa as entregas pelo `pm-revisor`, que só revisa e devolve (D-PM4); **integra** (D-PM5): merge em `main`, release e publicação na Play. Só o PM toca `main` e as faixas da Play.
- **Agentes de área** (N sessões de frente): cada um só na própria branch `front/NOME-DA-FRENTE`, em worktree própria; entrega PR com os portões rodados. Nunca faz merge nem release. Modelo pelo risco (D-PM9): Opus se a frente toca migração do Room, dado pessoal (localização, exportação, fotos) ou segurança do manifesto; Sonnet nas demais; o brief diz qual.
- **Analistas** (skill `analise`, pelo framework anexado): medem uma dimensão e propõem; não escrevem no produto.
- **Subagentes**: tarefa fechada, somente leitura salvo ordem escrita, devolvem no formato pedido.
- **Maestro** (fora do projeto, um para todos): guarda o padrão dos PMs e divide a cota de IA; as orientações dele chegam ao dono pela seção 9 do relatório do PM ("Orientações do Maestro"), transcritas sem reescrever. A cota deste projeto é conjunta com a da `loja-online`. No fim do projeto, com a autorização do dono, fecha o projeto e atualiza o framework.
- O dono fala só com o PM. Um escritor por branch. Decisão do dono registrada não se reabre.

## Portões (rodar separados, nunca com `| tail` sem `PIPESTATUS`; o CI roda os mesmos, nesta ordem)
1. `bash ferramentas/guardas.sh` → 0 (idioma pt-BR, cor só do tema, nenhum texto fixo)
2. `ktlint "**/*.kt" "**/*.kts"` → 0 **e** saída vazia (configuração em `.editorconfig`)
3. `./gradlew -p buildSrc test` → verde
4. `./gradlew lintDebug` → 0
5. `./gradlew assembleDebug` → 0 (compila Kotlin, KSP de Room e Hilt; é o typecheck do kit)
6. `./gradlew testDebugUnitTest` → verde; teste novo precisa ter sido **visto falhar** (bug plantado → vermelho → revertido)
   - Logo depois: `./gradlew verifyRoborazziDebug` → verde **e** `git status --porcelain -- app/src/test/snapshots` vazio (captura mudou ou é nova: `recordRoborazziDebug` e commitar)
   - Provas negativas: `BASE_SHA=<base> CORPO_PR="$(cat corpo.md)" bash ferramentas/provas-negativas.sh` → 0 (todo `@Test` novo do diff aparece pelo nome no corpo do PR; o CI lê o corpo por `env:`, nunca interpolado no `run:`; editar o corpo reexecuta o CI)
7. `./gradlew verificarManifestoRelease` → 0 (componente exportado entra na lista em `app/build.gradle.kts` com motivo)
8. `./gradlew bundleRelease` → 0, depois `bash ferramentas/teto-aab.sh` → 0 (o AAB não passa de `ferramentas/teto-aab.txt`; subir o teto só no PR, com o motivo na mensagem do commit, e o PM aceita; o script avisa quando o AAB cai, para baixar o teto)
9. Visual: 3 temas (Oficina, Estrada, Cromo) × modo oficina × fonte 140% e 200% × tela de 360 dp × teclado aberto, quando houver interface

## Regras inegociáveis
- Nunca descartar erro: `Result`/exceção tratada ou propagada; um `Flow` do Room que falha nunca vira lista vazia. Escrita confere as linhas afetadas.
- Mudança de entidade do Room sobe a versão do banco, com `Migration` testada e o schema exportado em `app/schemas/` commitado. O app abre um banco da versão anterior sem perder dado.
- Localização só com permissão concedida na hora do uso; o app funciona sem ela. Nada de coordenada, placa ou texto livre em log ou no Crashlytics; EXIF de GPS removido das fotos.
- Uma regra de negócio mora em um lugar só (funções de domínio puras, testadas na JVM).
- Toda tela usa `TelaDiario` (edge-to-edge, insets pelo `PaddingValues`); formulário usa `FormularioDiario` (rola e sobe com o teclado).
- Toda cor vem do `ColorScheme` com par nos 3 temas; texto em `sp`, espaçamento em `dp`, nunca px. As catracas (contagem de cor crua, tamanho do AAB) não sobem.
- **Regra da segunda ocorrência:** um bug que já aconteceu antes só é consertado junto com um primitivo, uma guarda ou um lint que impeça a volta (PFA-01, 2.3).
- Todo número citado vem de um comando rodado, com data e SHA.
- Versão nova de dependência só pelo `libs.versions.toml`.
- Segredo nunca no git: chave de upload da Play nos segredos do GitHub, com Play App Signing; `*.jks`, `*.keystore` e `local.properties` no `.gitignore`.
- Commit no formato `tipo(frente): texto (#issue)`; sem identificador de modelo e sem e-mail pessoal no commit (autor `noreply` do GitHub).
- Nada entra na `main` sem PR, sem os checks verdes e sem o veredito `aprovar` do `pm-revisor` (18 §5, regra 4).
- Antes de construir sobre um fornecedor (Play Console, Crashlytics, Supabase na fase 6): preço, cota, limites e política conferidos.

## Método (seção 18 §8 do framework; skill `metodo`)
- O framework é a base de todos os níveis, no tamanho do contexto de cada um: **G** (Maestro), **M** (PM), **P** (agente de área e analista), **PP** (subagente). Peça o seu nível e não leia capítulo inteiro sem motivo.
- Toda saída termina com "Não medido" e "Para o framework", cada lição com a prova (≤ 3 linhas no subagente; ≤ 5 lições no agente e no analista; ≤ 5 na seção 8 do relatório do PM).
- A skill `/nova-funcionalidade` (`.claude/skills/nova-funcionalidade/`) gera entidade, DAO, repositório, ViewModel, tela e testes no padrão; `verificar-padrao.sh` reprova o que foge dele.

## Planejamento com as áreas (seção 14 do framework; plano em `docs/PLANEJAMENTO.md`)
- O plano de 28-Set cobre 5 áreas (v1.6). Fase nova revisa as nove áreas da v2.0 (14 §2): UX/UI, Segurança, DevOps, Desempenho, Marketing, Suporte, Back-end e dados, Pagamentos e Privacidade, cada uma com fonte ou motivo para não se aplicar.
- Toda frente cita, na seção "Áreas consultadas" do brief, a regra de cada área que ela toca.

## Gestão (Kanban + PMBOK 8, escala S — seção 09 do framework; detalhe em `docs/GESTAO.md`)
- Fluxo: Opções → Com evidência → Pronta (ponto de compromisso) → Execução (WIP 2) → Integração (WIP 2) → Validação com o dono (WIP 10) → Concluída. Puxar só com espaço no limite. Os limites mudam só depois da linha de base (18 §5, regra 6).
- Classes de serviço: `urgente` (fura o WIP, máx. 1), `data-fixa` (ex.: targetSdk anual em agosto), padrão, `estrutural` (~20%).
- Riscos e decisões são issues (`risco`, `decisao`). Decisão só se reabre com nova issue que cite a anterior.
- **Sprint = semana ISO, fechada na segunda.** O PM apresenta ao dono o relatório e o board (skill `gerencia`, `sprint fechar`), com as Orientações do Maestro; depois, `sprint abrir`.
- Os registros do PM (relatório, board, linhas de `dados/`) vão ao framework por PR de registro, na branch `pm/moto-AAAA-Sss`, integrado pelo dono.

## Fim de trabalho
Toda sessão termina com o bloco `=== FIM DE TRABALHO ===` (seção 7 do 05 do framework), ou com `=== NÃO RODE /clear AINDA ===` se algo faltar.
A linha `Sessão: <min> min · tokens <in>/<out> · US$ <custo> · <modelo>` sai do `get_session` e vira uma linha em `dados/sessoes.csv` do framework, com o papel (`pm`, `frente`, `analista`) e o projeto `moto`. No commit e no arquivo do projeto, o modelo não aparece.

## Desvios deste framework (motivo obrigatório)
- Portões `typecheck` e `e2e` do kit viram `assembleDebug` e, quando houver emulador no CI, `connectedDebugAndroidTest`: o compilador Kotlin faz o typecheck; teste instrumentado entra com as telas reais.
- Sem `node`/`npm`: o app é Android; a versão de ferramenta fixada é o JDK 21 e o Gradle Wrapper.
- Regras de RLS e `GRANT` do kit não se aplicam no MVP (sem servidor); voltam na fase 6, com o Supabase.
- Kit instalado com `--sem-analise`: as skills `analise`, `metodo` e `metodo-cartao` e os subagentes `analista-*` e `pm-*` chegam pelo framework anexado à sessão (mesmo desvio do Velix, #565), para o repositório público de portfólio carregar só o que é do produto.
- Branch de frente: `front/NOME` quando o ambiente permitir; se a sessão impuser `claude/...`, o nome da frente vai no título do PR.
