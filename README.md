# App Mobile Motos

App Android de diário de bordo e manutenção para motociclistas, em Kotlin nativo (Jetpack Compose).

**Status:** fase 0 (fundação). Esqueleto do projeto compilando no CI.

## Documentos

- [Plano de produto](docs/plano-de-produto.md): escopo, stack, roadmap, publicação na Play Store, monetização, custos, métricas e riscos
- [Design no Figma](https://www.figma.com/design/XLfpBLh0ndZhvYecq2uYbz): wireframe do fluxo e telas em alta fidelidade nos temas Oficina, Estrada e Cromo
- [CLAUDE.md](CLAUDE.md): regras de trabalho (Framework Aprendido v1.4)

## Como rodar

Requisitos: JDK 21 e Android SDK com a plataforma 37 (o Android Studio instala sozinho).

```bash
./gradlew lintDebug          # portão 1
./gradlew assembleDebug      # portão 2: gera app/build/outputs/apk/debug/
./gradlew testDebugUnitTest  # portão 3
```

## Estrutura

Um módulo `:app`, com pacotes por funcionalidade em `app/src/main/java/com/pedrohrsm/diariodebordo/`:

| Pacote | O que tem |
| --- | --- |
| `core` | Banco Room, injeção (Hilt) e os 3 temas em Compose |
| `garagem` | Moto e hodômetro (entidade, DAO, tela) |
| `plano` | Regra de manutenção por km e tempo, e o worker de lembretes |
| `localizacao` | Permissões de GPS, pedidas só na hora do uso |
| `navegacao` | Rotas do app |

Versões de dependências ficam só em `gradle/libs.versions.toml`.
