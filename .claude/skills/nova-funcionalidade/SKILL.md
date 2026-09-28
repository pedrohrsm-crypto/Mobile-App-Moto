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

O pacote `garagem` é a referência viva dos moldes: se um mudar, o outro muda junto.

```bash
.claude/skills/nova-funcionalidade/gerar.sh <pacote> <Entidade> <tabela>
```

O script para sem gerar nada se o pacote ou a tabela já existirem, ou se faltar `core/di/TempoModule.kt` (fornece o `Clock`). Ele cria:

- `data/`: entidade com os campos de controle de `docs/offline-first.md`, DAO (lista só ativos, exclusão lógica, escritas devolvem linhas), repositório (único que escreve; preenche `criadoEm`, `alteradoEm`, `fusoHorario` pelo `Clock` injetado e confere as linhas)
- `ui/`: estado (`Carregando` / `Pronto`), ViewModel, tela Compose com Route + Screen + Preview, dentro da casca `TelaDiario` (edge-to-edge; formulário usa `FormularioDiario`)
- testes na JVM: DAO falso em memória, testes do repositório e do ViewModel

## 3. Ligar nos arquivos compartilhados

- `core/database/DiarioDatabase.kt`: a entidade em `entities` e `abstract fun <pacote>Dao()`. Enquanto nenhuma versão do app tiver sido publicada, tudo fica na versão 1 (veja o comentário do arquivo). Depois da primeira publicação: `version` + 1; tabela nova com `autoMigrations = [AutoMigration(from = N, to = N + 1)]`, mudança em tabela existente com `Migration` escrita à mão e teste de migração.
- Backup (a entidade já sai com `@Serializable`):
  - `backup/dominio/CopiaDeSeguranca.kt`: `val <plural>: List<Entidade> = emptyList()`. O JSON ignora chaves desconhecidas, então lista nova com valor padrão não sobe o `FORMATO_ATUAL`.
  - `backup/data/BackupDao.kt`: `@Query("SELECT * FROM <tabela>") suspend fun <plural>()` e `@Upsert suspend fun gravar<Plural>(itens: List<Entidade>)`.
  - `backup/data/BackupRepositorio.kt`: exportar só os ativos (`excluidoEm == null`) e, na importação, `mesclar(dao.<plural>(), copia.<plural>, { it.id }, { it.alteradoEm })`.
  - `FakeBackupDao` nos testes e um caso no `BackupRepositorioTest`.
  - Entidade que não deve sair do aparelho diz "fora do backup" no KDoc e pula estes passos.
- `core/di/DatabaseModule.kt`: `@Provides fun provide<Entidade>Dao(database: DiarioDatabase) = database.<pacote>Dao()`.
- `navegacao/DiarioNavHost.kt`: constante em `Rotas` e `composable(Rotas.X) { <Entidade>Route() }`.
- `res/values/strings.xml`: `<pacote>_titulo` e `<pacote>_vazio`, e os textos novos da tela. Nenhum texto fixo no Kotlin.

## 4. Preencher o que é da funcionalidade

Troque cada comentário `AJUSTAR:` e apague o comentário:

- campos reais na entidade (no lugar de `descricao`), mantendo os campos de controle intactos;
- ordenação da consulta no DAO, se `criadoEm` não servir;
- o que o item da lista mostra na tela;
- o item de exemplo nos dois testes.

Espaçamentos e alvos de toque vêm de `LocalDimensoes.current` (o modo oficina aumenta tudo), nunca de `dp` fixo na tela.

Regra de negócio (cálculo de consumo, vencimento etc.) vai numa função pura em `<pacote>/dominio/`, com teste próprio, nunca na tela nem no DAO.

## 5. Conferir

Rode cada um separado, sem `| tail`, e só siga com todos verdes:

```bash
.claude/skills/nova-funcionalidade/verificar-padrao.sh <pacote>
bash ferramentas/guardas.sh
ktlint "**/*.kt" "**/*.kts" "!**/build/**"   # 1.7.1, regras em .editorconfig (120 colunas, trailing comma)
./gradlew lintDebug
./gradlew assembleDebug        # gera o schema novo em app/schemas/: commite
./gradlew testDebugUnitTest
```

Teste novo precisa ter sido visto falhar: quebre de propósito a regra que ele cobre, veja o vermelho e desfaça.

`verificar-padrao.sh` sem argumento confere todos os pacotes com entidade. Ele falha quando sobra `AJUSTAR:`, falta campo de controle, a entidade não está no banco ou no backup (`@Serializable`, `CopiaDeSeguranca`, `BackupDao`), o DAO não está no Hilt, uma escrita não devolve linhas, há cor fixa fora do tema, a tela usa `Scaffold` em vez de `TelaDiario`, falta teste ou o schema da versão atual não foi commitado.

Se este ambiente não tiver o SDK do Android, diga isso e deixe o CI do PR ser a prova do build; não afirme que compila sem ver o CI verde.

## 6. Entregar

Commit no formato `feat(<pacote>): cria funcionalidade <nome> (#issue)`. No PR, liste os campos escolhidos e a versão nova do banco.
