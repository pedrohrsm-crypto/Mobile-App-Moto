#!/usr/bin/env bash
# Gera o esqueleto de uma funcionalidade a partir dos moldes.
# Uso: gerar.sh <pacote> <Entidade> <tabela>
#   ex.: gerar.sh abastecimento Abastecimento abastecimentos
# Não sobrescreve nada: para se o pacote já existir.
set -euo pipefail

if [ $# -ne 3 ]; then
  echo "Uso: $0 <pacote> <Entidade> <tabela>   (ex.: $0 abastecimento Abastecimento abastecimentos)" >&2
  exit 2
fi
PACOTE=$1 ENTIDADE=$2 TABELA=$3

[[ $PACOTE =~ ^[a-z][a-z0-9]*$ ]] || { echo "pacote deve ser minúsculo, sem acento: $PACOTE" >&2; exit 2; }
[[ $ENTIDADE =~ ^[A-Z][A-Za-z0-9]*$ ]] || { echo "Entidade deve ser PascalCase, sem acento: $ENTIDADE" >&2; exit 2; }
[[ $TABELA =~ ^[a-z][a-z0-9_]*$ ]] || { echo "tabela deve ser minúscula, sem acento: $TABELA" >&2; exit 2; }

RAIZ=$(git rev-parse --show-toplevel)
MOLDES="$RAIZ/.claude/skills/nova-funcionalidade/moldes"
BASE=com/pedrohrsm/diariodebordo
MAIN="$RAIZ/app/src/main/java/$BASE"
TESTE="$RAIZ/app/src/test/java/$BASE"

# O ktlint é parte do gerador (ordem de import): sem ele, nada é gerado.
if command -v ktlint >/dev/null; then
  KTLINT=(ktlint)
elif [ -n "${KTLINT_JAR:-}" ] && [ -f "$KTLINT_JAR" ]; then
  KTLINT=(java -jar "$KTLINT_JAR")
else
  echo "ktlint não encontrado no PATH (nem KTLINT_JAR). Instale o ktlint 1.7.1 e rode de novo; nada foi gerado." >&2
  exit 1
fi

for d in "$MAIN/$PACOTE" "$TESTE/$PACOTE"; do
  [ ! -e "$d" ] || { echo "já existe: ${d#$RAIZ/} (nada foi gerado)" >&2; exit 1; }
done
[ -e "$MAIN/core/di/TempoModule.kt" ] || { echo "falta core/di/TempoModule.kt (fornece o Clock dos repositórios)" >&2; exit 1; }
grep -rqs "tableName = \"$TABELA\"" "$MAIN" && { echo "tabela $TABELA já existe (nada foi gerado)" >&2; exit 1; }

# Linhas do molde terminadas em "// BACKUP" só valem a partir da #45 (frente de backup, que cria
# backup/dominio/CopiaDeSeguranca.kt e o plugin kotlinx-serialization no libs.versions.toml). Sem eles, o gerador as remove para a entidade compilar.
if [ -e "$MAIN/backup/dominio/CopiaDeSeguranca.kt" ] && grep -qi "serialization" "$RAIZ/gradle/libs.versions.toml"; then
  BACKUP=(-e 's# // BACKUP$##')
else
  BACKUP=(-e '/ \/\/ BACKUP$/d')
  echo "aviso: backup/ ou o plugin de serialização ainda não existem (a partir da #45); entidade gerada sem @Serializable"
fi

renderizar() { # molde destino
  mkdir -p "$(dirname "$2")"
  sed "${BACKUP[@]}" -e "s/__PACOTE__/$PACOTE/g" -e "s/__ENTIDADE__/$ENTIDADE/g" -e "s/__TABELA__/$TABELA/g" "$MOLDES/$1" > "$2"
  echo "  criado ${2#$RAIZ/}"
}

echo "Gerando $PACOTE ($ENTIDADE, tabela $TABELA):"
renderizar Entidade.kt.molde               "$MAIN/$PACOTE/data/$ENTIDADE.kt"
renderizar EntidadeDao.kt.molde            "$MAIN/$PACOTE/data/${ENTIDADE}Dao.kt"
renderizar EntidadeRepositorio.kt.molde    "$MAIN/$PACOTE/data/${ENTIDADE}Repositorio.kt"
renderizar EntidadeEstado.kt.molde         "$MAIN/$PACOTE/ui/${ENTIDADE}Estado.kt"
renderizar EntidadeViewModel.kt.molde      "$MAIN/$PACOTE/ui/${ENTIDADE}ViewModel.kt"
renderizar EntidadeScreen.kt.molde         "$MAIN/$PACOTE/ui/${ENTIDADE}Screen.kt"
renderizar FakeEntidadeDao.kt.molde        "$TESTE/$PACOTE/data/Fake${ENTIDADE}Dao.kt"
renderizar EntidadeRepositorioTest.kt.molde "$TESTE/$PACOTE/data/${ENTIDADE}RepositorioTest.kt"
renderizar EntidadeViewModelTest.kt.molde  "$TESTE/$PACOTE/ui/${ENTIDADE}ViewModelTest.kt"


# A ordem dos imports depende do nome do pacote; o ktlint do projeto acerta isso e o resto do estilo.
# Falha do ktlint não é descartada: o script sai com o código dele.
gerados=("$MAIN/$PACOTE" "$TESTE/$PACOTE")
if [ "${#KTLINT[@]}" -gt 0 ]; then
  "${KTLINT[@]}" -F "${gerados[@]/%//**/*.kt}" || {
    status=$?
    echo "ktlint -F falhou (exit $status): corrija o que ele listou acima antes de seguir" >&2
    exit "$status"
  }
  echo "  formatado com ktlint"
fi

cat <<FIM

Falta ligar nos arquivos compartilhados (passo 3 da skill):
  - core/database/DiarioDatabase.kt: $ENTIDADE em entities, fun ${PACOTE}Dao(); a versão fica em 1 até a primeira publicação (#12); depois, version + 1 e Migration testada
  - core/di/DatabaseModule.kt: provide${ENTIDADE}Dao
  - backup (só a partir da #45): @Serializable, lista em CopiaDeSeguranca, leitura e gravação em BackupDao, exportar/mesclar em BackupRepositorio
  - navegacao/DiarioNavHost.kt: rota e composable de ${ENTIDADE}Route
  - res/values/strings.xml: ${PACOTE}_titulo e ${PACOTE}_vazio
Depois troque todo "AJUSTAR:" e rode verificar-padrao.sh $PACOTE.
FIM
