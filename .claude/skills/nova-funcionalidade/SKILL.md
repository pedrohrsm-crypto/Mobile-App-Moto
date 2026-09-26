---
name: nova-funcionalidade
description: Cria uma funcionalidade completa do app (entidade do Room com campos de controle offline, DAO, repositório, ViewModel, tela Compose, rota, registro no banco e no Hilt, testes) a partir dos moldes do projeto. Use quando o pedido for "nova funcionalidade", "novo cadastro", "nova tabela com tela" ou /nova-funcionalidade <nome>.
---

# /nova-funcionalidade <nome>

Gera uma funcionalidade inteira no padrão do projeto, sem copiar e colar à mão. O código repetitivo sai dos moldes em `moldes/`; o que é da funcionalidade (campos, textos, ordenação) você preenche; a checagem no fim garante que nada fugiu do padrão.

Leia o `CLAUDE.md` da raiz antes: as regras de lá valem aqui (português do Brasil, escrita confere linhas, migração a cada mudança de entidade, cores só do tema).

## 1. Definir os nomes (pergunte ao Pedro se faltar algo)

| Nome | Regra | Exemplo |
| --- | --- | --- |
| pacote | minúsculo, sem acento, singular | `abastecimento` |
| Entidade | PascalCase, sem acento, singular | `Abastecimento` |
| tabela | minúsculo, plural | `abastecimentos` |
| campos | nome, tipo, obrigatório ou não | `litros: Double`, `valorCentavos: Long`, `hodometroKm: Int` |

Dinheiro em centavos (`Long`), distância em km (`Int`), instantes em milissegundos UTC (`Long`). Nada de `Float` para dinheiro.

## 2. Gerar os arquivos

```bash
.claude/skills/nova-funcionalidade/gerar.sh <pacote> <Entidade> <tabela>
```

O script para sem gerar nada se o pacote ou a tabela já existirem. Ele cria:

- `data/`: entidade com os campos de controle de `docs/offline-first.md`, DAO (lista só ativos, exclusão lógica, escritas devolvem linhas), repositório (único que escreve; preenche `criadoEm`, `alteradoEm`, `fusoHorario` pelo `Clock` injetado e confere as linhas)
- `ui/`: estado (`Carregando` / `Pronto`), ViewModel, tela Compose com Route + Screen + Preview
- testes na JVM: DAO falso em memória, testes do repositório e do ViewModel
- `core/di/TempoModule.kt` (só na primeira vez), que fornece o `Clock`

## 3. Ligar nos arquivos compartilhados

- `core/database/DiarioDatabase.kt`: a entidade em `entities`, `abstract fun <pacote>Dao()`, `version` + 1. Tabela nova: `autoMigrations = [AutoMigration(from = N, to = N + 1)]`. Mudança em tabela existente: `Migration` escrita à mão e teste de migração.
- `core/di/DatabaseModule.kt`: `@Provides fun provide<Entidade>Dao(database: DiarioDatabase) = database.<pacote>Dao()`.
- `navegacao/DiarioNavHost.kt`: constante em `Rotas` e `composable(Rotas.X) { <Entidade>Route() }`.
- `res/values/strings.xml`: `<pacote>_titulo` e `<pacote>_vazio`, e os textos novos da tela. Nenhum texto fixo no Kotlin.

## 4. Preencher o que é da funcionalidade

Troque cada comentário `AJUSTAR:` e apague o comentário:

- campos reais na entidade (no lugar de `descricao`), mantendo os campos de controle intactos;
- ordenação da consulta no DAO, se `criadoEm` não servir;
- o que o item da lista mostra na tela;
- o item de exemplo nos dois testes.

Regra de negócio (cálculo de consumo, vencimento etc.) vai numa função pura em `<pacote>/dominio/`, com teste próprio, nunca na tela nem no DAO.

## 5. Conferir

Rode cada um separado, sem `| tail`, e só siga com todos verdes:

```bash
.claude/skills/nova-funcionalidade/verificar-padrao.sh <pacote>
./gradlew lintDebug
./gradlew assembleDebug        # gera o schema novo em app/schemas/: commite
./gradlew testDebugUnitTest
```

Teste novo precisa ter sido visto falhar: quebre de propósito a regra que ele cobre, veja o vermelho e desfaça.

`verificar-padrao.sh` sem argumento confere todos os pacotes com entidade. Ele falha quando sobra `AJUSTAR:`, falta campo de controle, a entidade não está no banco, o DAO não está no Hilt, uma escrita não devolve linhas, há cor fixa fora do tema, falta teste ou o schema da versão atual não foi commitado.

Se este ambiente não tiver o SDK do Android, diga isso e deixe o CI do PR ser a prova do build; não afirme que compila sem ver o CI verde.

## 6. Entregar

Commit no formato `feat(<pacote>): cria funcionalidade <nome> (#issue)`. No PR, liste os campos escolhidos e a versão nova do banco.
