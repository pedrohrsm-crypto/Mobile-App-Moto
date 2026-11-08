# Diário de Bordo — ficha técnica

Estrutura do app, stack e testes. Visão geral do produto e das decisões no [estudo de caso](../README.md).

Números medidos em 8-Nov-2026 na `main` `ce80744`, exceto onde a linha diz outra fonte (ver "Fontes" no fim).

## Plataforma

| Item | Valor |
|---|---|
| Sistema | Android 8.0 ou mais novo (`minSdk` 26) |
| `targetSdk` / `compileSdk` | 36 / 37 |
| Linguagem | Kotlin 2.4.21, com KSP 2.3.12 para os processadores de anotação |
| Build | Gradle 9.8.2 (Wrapper), Android Gradle Plugin 9.5.0, JDK 21 |
| Versões | todas fixadas num único catálogo, [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) |

## Stack

| Camada | Biblioteca | Versão |
|---|---|---|
| Interface | Jetpack Compose (BOM) + Material 3 | 2026.09.00 |
| Navegação | Navigation Compose | 2.10.2 |
| Estado e ciclo de vida | Lifecycle (ViewModel) + Coroutines/Flow | 2.11.0 / 1.11.0 |
| Injeção de dependência | Hilt | 2.60.1 |
| Banco local (fonte da verdade) | Room, com o schema exportado em [`app/schemas/`](../app/schemas) | 2.8.5 |
| Listas longas | Paging | 3.5.3 |
| Lembretes em segundo plano | WorkManager | 2.12.0 |
| Localização (só com permissão na hora do uso) | Play Services Location | 21.4.0 |
| Fotos (GPS do EXIF removido) | ExifInterface | 1.4.2 |
| Backup em `.zip` | kotlinx-serialization | 1.12.0 |

## Arquitetura

- **Um módulo `:app`**, com pacotes por funcionalidade: `garagem`, `diario`, `plano`, `numeros`, `localizacao`, `rota`, `backup` e `ajustes`, mais `core` para o que é compartilhado (tema, componentes, banco, injeção).
- **MVVM:** tela Compose → ViewModel → repositório → DAO do Room. A tela observa um `Flow`. Um `Flow` que falha mostra erro na tela; nunca vira lista vazia.
- **Regras de negócio em funções puras** (`*/dominio/`), testadas na JVM, sem Android. Exemplo: o km do hodômetro nunca volta, e o vencimento do plano é por km e/ou por dias.
- **Offline-first:** nada depende de rede. Exclusão lógica (`excluidoEm`) e marcas de alteração em cada tabela.
- **Esqueleto único de tela e de formulário** (`TelaDiario` e `FormularioDiario`): edge-to-edge e insets aplicados. O formulário rola e sobe com o teclado.
- **Toda cor vem do tema**, com par nos 3 temas (Oficina, Estrada, Cromo). Texto em `sp`, espaçamento em `dp`.
- **Build de debug separado** (`applicationId` `.debug`, nome "Diário (exemplo)") com a ação **Carregar dados de exemplo**. O release não compila esse código.

## Testes

| Tipo | Quantidade | O que cobre |
|---|---|---|
| Execuções de teste unitário (JVM e Robolectric) | 757, nenhuma falhando nem ignorada | Domínio, repositórios com Room em memória, ViewModels, telas Compose e regras de backup |
| Métodos `@Test` no código | 394 | O número de execuções é maior porque parte dos testes roda uma vez por tema, fonte ou caso |
| Capturas de tela comparadas pixel a pixel (Roborazzi) | 372 | 3 temas × modo oficina × fonte 140% e 200%, em tela de 360 dp, inclusive com o teclado aberto |

Métodos `@Test` por área (código-fonte em `app/src/test` e `app/src/testDebug`):

| Área | `@Test` |
|---|---|
| plano | 98 |
| garagem | 90 |
| backup | 64 |
| core | 49 |
| rota | 32 |
| diario | 31 |
| numeros | 17 |
| exemplo (só debug) | 8 |
| ajustes | 4 |
| navegacao | 1 |

**Teste visto falhar:** todo teste novo entra com a prova de que pega o defeito. O bug é plantado, o teste fica vermelho e o bug é revertido. O CI reprova o PR se o corpo não citar cada teste novo pelo nome.

## Portões do CI

Cada PR passa por estes 13 passos, nesta ordem; o push na `main` passa por todos menos o 4, que confere o corpo do PR (GitHub Actions, [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)):

1. Guardas de idioma (pt-BR), cor fora do tema e texto fixo na interface
2. Formatação (ktlint)
3. Testes dos scripts de ferramentas
4. Provas negativas: todo teste novo citado no PR (só em PR)
5. Testes do buildSrc
6. Lint do Android
7. Build (`assembleDebug`, que compila o Kotlin e gera o código do Room e do Hilt)
8. Testes unitários
9. Capturas do Roborazzi iguais às versionadas
10. Manifesto do release: nenhum componente exportado sem motivo
11. AAB de release
12. Teto de tamanho do AAB
13. Schema do Room igual ao versionado

No push na `main` também saem:
- o **APK de release assinado**, com a assinatura conferida pelo `apksigner`;
- o **APK de teste** com os dados de exemplo.

Último run verde da `main`: [run 42291246030](https://github.com/pedrohrsm-crypto/Mobile-App-Moto/actions/runs/42291246030), head `ce80744`, 6-Nov-2026.

## Tamanho

AAB de release: 5.210.264 B (cerca de 5,0 MiB), com R8 (minificação e remoção de recursos). O teto é de 5.210.245 B + 2.048 B de tolerância, conferido em todo PR.

## Fontes

| Número | Fonte (comando) | Data |
|---|---|---|
| Versões | `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `app/build.gradle.kts` na `ce80744` | 8-Nov-2026 |
| 757 execuções | CI do PR #81, soma dos `tests` dos XML de `testDebugUnitTest` (78 classes); o código de teste não mudou até a `ce80744` | 6-Nov-2026 |
| 394 `@Test` e a tabela por área | `grep -rho '@Test' app/src/test app/src/testDebug \| wc -l` e o mesmo por pasta de pacote, na `ce80744` | 8-Nov-2026 |
| 372 capturas | `find app/src/test/snapshots -name '*.png' \| wc -l`, na `ce80744` | 8-Nov-2026 |
| AAB | `bash ferramentas/teto-aab.sh` no CI do PR #81, run 42271396132, head `1017a4f` | 6-Nov-2026 |
| Run verde | GitHub Actions, run 42291246030 (push na `main`, `ce80744`) | 6-Nov-2026 |
