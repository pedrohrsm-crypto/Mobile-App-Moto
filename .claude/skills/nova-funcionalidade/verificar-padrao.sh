#!/usr/bin/env bash
# Confere se as funcionalidades seguem o molde da skill nova-funcionalidade.
# Uso: verificar-padrao.sh [pacote...]   (sem argumento: todos os pacotes com entidade do Room)
# Sai com 1 e lista cada problema se algo fugir do padrão.
set -uo pipefail

RAIZ=$(git rev-parse --show-toplevel)
BASE=com/pedrohrsm/diariodebordo
MAIN="$RAIZ/app/src/main/java/$BASE"
TESTE="$RAIZ/app/src/test/java/$BASE"
DB="$MAIN/core/database/DiarioDatabase.kt"
DI="$MAIN/core/di/DatabaseModule.kt"
problemas=0
falha() { echo "  ✗ $*"; problemas=$((problemas + 1)); }

if [ $# -gt 0 ]; then
  pacotes=("$@")
else
  mapfile -t pacotes < <(grep -rl "^@Entity" "$MAIN" --include=*.kt | sed -E "s#^$MAIN/([^/]+)/.*#\1#" | sort -u)
fi

for p in "${pacotes[@]}"; do
  echo "$p:"
  dir="$MAIN/$p"
  [ -d "$dir" ] || { falha "pacote não existe: ${dir#$RAIZ/}"; continue; }

  # 1. Nada de marcador pendente do gerador.
  grep -rn "AJUSTAR:" "$dir" "$TESTE/$p" 2>/dev/null | sed "s#$RAIZ/##" | while read -r l; do echo "  ✗ marcador pendente: $l"; done
  n=$(grep -rn "AJUSTAR:" "$dir" "$TESTE/$p" 2>/dev/null | wc -l); problemas=$((problemas + n))

  # 2. Toda entidade tem os campos de controle de docs/offline-first.md.
  for f in $(grep -rl "^@Entity" "$dir" --include=*.kt); do
    classe=$(grep -oE "data class [A-Za-z0-9]+" "$f" | head -1 | awk '{print $3}')
    grep -qE "@PrimaryKey val id: String" "$f" || falha "$classe: id deve ser UUID em texto (@PrimaryKey val id: String)"
    for campo in "criadoEm: Long" "alteradoEm: Long" "fusoHorario: String" "excluidoEm: Long?" "pendenteSync: Boolean"; do
      grep -qF "val $campo" "$f" || falha "$classe: falta o campo de controle 'val $campo'"
    done
    # 3. Toda entidade está registrada no banco.
    grep -qE "entities = \[[^]]*\b$classe::class" "$DB" || falha "$classe: não está em entities de DiarioDatabase"
  done

  # 4. Escritas devolvem as linhas afetadas; todo DAO é fornecido pelo Hilt.
  for f in $(grep -rl "^@Dao" "$dir" --include=*.kt); do
    dao=$(grep -oE "interface [A-Za-z0-9]+" "$f" | head -1 | awk '{print $2}')
    grep -A1 -E "@(Update|Delete)" "$f" | grep -E "suspend fun" | grep -vqE "\): Int$" && falha "$dao: @Update/@Delete deve devolver Int (linhas afetadas)"
    grep -qE "fun provide$dao\(" "$DI" || falha "$dao: falta provide$dao em DatabaseModule"
  done

  # 5. Cor só vem do ColorScheme.
  grep -rnE "Color\(0x|Color\.(Red|Blue|Green|Black|White|Gray|Yellow)" "$dir" --include=*.kt | sed "s#$RAIZ/##" | while read -r l; do echo "  ✗ cor fixa fora do tema: $l"; done
  n=$(grep -rnE "Color\(0x|Color\.(Red|Blue|Green|Black|White|Gray|Yellow)" "$dir" --include=*.kt | wc -l); problemas=$((problemas + n))

  # 6. Toda funcionalidade tem teste na JVM.
  find "$TESTE/$p" -name "*Test.kt" 2>/dev/null | grep -q . || falha "sem testes em ${TESTE#$RAIZ/}/$p"
done

# 7. Schema exportado da versão atual do banco está commitado.
versao=$(grep -oE "version = [0-9]+" "$DB" | grep -oE "[0-9]+")
schema="$RAIZ/app/schemas/${BASE//\//.}.core.database.DiarioDatabase/$versao.json"
[ -f "$schema" ] || { echo "banco:"; falha "schema da versão $versao não está em ${schema#$RAIZ/} (rode ./gradlew assembleDebug e commite)"; }

if [ $problemas -gt 0 ]; then
  echo "$problemas problema(s) fora do padrão."
  exit 1
fi
echo "Tudo no padrão."
