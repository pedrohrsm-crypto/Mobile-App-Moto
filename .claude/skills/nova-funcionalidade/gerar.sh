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

for d in "$MAIN/$PACOTE" "$TESTE/$PACOTE"; do
  [ ! -e "$d" ] || { echo "já existe: ${d#$RAIZ/} (nada foi gerado)" >&2; exit 1; }
done
[ -e "$MAIN/core/di/TempoModule.kt" ] || { echo "falta core/di/TempoModule.kt (fornece o Clock dos repositórios)" >&2; exit 1; }
grep -rqs "tableName = \"$TABELA\"" "$MAIN" && { echo "tabela $TABELA já existe (nada foi gerado)" >&2; exit 1; }

renderizar() { # molde destino
  mkdir -p "$(dirname "$2")"
  sed -e "s/__PACOTE__/$PACOTE/g" -e "s/__ENTIDADE__/$ENTIDADE/g" -e "s/__TABELA__/$TABELA/g" "$MOLDES/$1" > "$2"
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
gerados=("$MAIN/$PACOTE" "$TESTE/$PACOTE")
if command -v ktlint >/dev/null; then
  ktlint -F "${gerados[@]/%//**/*.kt}" >/dev/null 2>&1 || true
  echo "  formatado com ktlint"
elif [ -n "${KTLINT_JAR:-}" ]; then
  java -jar "$KTLINT_JAR" -F "${gerados[@]/%//**/*.kt}" >/dev/null 2>&1 || true
  echo "  formatado com ktlint ($KTLINT_JAR)"
else
  echo "  aviso: ktlint não encontrado; rode ktlint -F nos arquivos gerados antes do commit"
fi

cat <<FIM

Falta ligar nos arquivos compartilhados (passo 3 da skill):
  - core/database/DiarioDatabase.kt: $ENTIDADE em entities, fun ${PACOTE}Dao(), version + 1 e migração
  - core/di/DatabaseModule.kt: provide${ENTIDADE}Dao
  - navegacao/DiarioNavHost.kt: rota e composable de ${ENTIDADE}Route
  - res/values/strings.xml: ${PACOTE}_titulo e ${PACOTE}_vazio
Depois troque todo "AJUSTAR:" e rode verificar-padrao.sh $PACOTE.
FIM
