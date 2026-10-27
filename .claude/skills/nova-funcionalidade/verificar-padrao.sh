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
COPIA="$MAIN/backup/dominio/CopiaDeSeguranca.kt"
BKDAO="$MAIN/backup/data/BackupDao.kt"
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

  # 2 e 3. Entidades: campos de controle, registro no banco e ida ao backup.
  #   Regras de backup valem a partir da #45 (quando backup/dominio/CopiaDeSeguranca.kt existir).
  #   Entidade que não vai no backup (ex.: ponto bruto de rota) diz isso no KDoc com
  #   "fora do backup" ou "não vai no arquivo exportado".
  saida=$(python3 - "$dir" "$DB" "$COPIA" "$BKDAO" <<'PY'
import os, re, sys
pasta, db, copia, bkdao = sys.argv[1:5]
ler = lambda c: open(c, encoding="utf-8").read() if os.path.exists(c) else None
m = re.search(r"entities\s*=\s*\[(.*?)\]", ler(db) or "", re.S)
no_banco = set(re.findall(r"(\w+)::class", m.group(1))) if m else set()
txt_copia, txt_bkdao = ler(copia), ler(bkdao)
controle = ["criadoEm: Long", "alteradoEm: Long", "fusoHorario: String", "excluidoEm: Long?", "pendenteSync: Boolean"]
for raiz, _, arquivos in os.walk(pasta):
    for a in sorted(arquivos):
        if not a.endswith(".kt"):
            continue
        texto = ler(os.path.join(raiz, a))
        for e in re.finditer(r"(/\*\*(?:(?!\*/).)*\*/\s*)?((?:@\w+(?:\([^)]*\))?\s*)*)@Entity\b.*?data class (\w+)\((.*?)\n\)", texto, re.S):
            doc, anot, classe, corpo = e.group(1) or "", e.group(2), e.group(3), e.group(4)
            if not re.search(r"@PrimaryKey val id: String", corpo):
                print(f"{classe}: id deve ser UUID em texto (@PrimaryKey val id: String)")
            for c in controle:
                if f"val {c}" not in corpo:
                    print(f"{classe}: falta o campo de controle 'val {c}'")
            if classe not in no_banco:
                print(f"{classe}: não está em entities de DiarioDatabase")
            if re.search(r"fora do backup|n[ãa]o vai no arquivo exportado|nem vai no arquivo exportado", doc, re.I):
                continue
            if txt_copia is None:
                continue  # a partir da #45: sem backup/ na árvore, não há o que exigir
            texto_antes = texto[max(0, e.start() - 200):e.start()] + anot
            if "@Serializable" not in (anot + doc + texto_antes).split("*/")[-1]:
                print(f"{classe}: falta @Serializable (vai no backup)")
            if not re.search(rf"List<{classe}>", txt_copia):
                print(f"{classe}: falta a lista em CopiaDeSeguranca")
            if txt_bkdao is not None and not re.search(rf"gravar\w*\(itens: List<{classe}>\)", txt_bkdao):
                print(f"{classe}: falta leitura e gravação em BackupDao")
PY
)
  if [ -n "$saida" ]; then
    echo "$saida" | sed 's/^/  ✗ /'
    problemas=$((problemas + $(echo "$saida" | wc -l)))
  fi

  # 4. Escritas devolvem as linhas afetadas; todo DAO é fornecido pelo Hilt.
  for f in $(grep -rl "^@Dao" "$dir" --include=*.kt); do
    dao=$(grep -oE "interface [A-Za-z0-9]+" "$f" | head -1 | awk '{print $2}')
    grep -A1 -E "@(Update|Delete)" "$f" | grep -E "suspend fun" | grep -vqE "\): Int$" && falha "$dao: @Update/@Delete deve devolver Int (linhas afetadas)"
    grep -qE "fun provide$dao\(" "$DI" || falha "$dao: falta provide$dao em DatabaseModule"
  done

  # 5. Cor só vem do ColorScheme.
  grep -rnE "Color\(0x|Color\.(Red|Blue|Green|Black|White|Gray|Yellow)" "$dir" --include=*.kt | sed "s#$RAIZ/##" | while read -r l; do echo "  ✗ cor fixa fora do tema: $l"; done
  n=$(grep -rnE "Color\(0x|Color\.(Red|Blue|Green|Black|White|Gray|Yellow)" "$dir" --include=*.kt | wc -l); problemas=$((problemas + n))

  # 6. Tela usa a casca TelaDiario (edge-to-edge), nunca Scaffold direto.
  grep -rnE "\bScaffold[[:space:]]*[({]" "$dir" --include=*.kt | sed "s#$RAIZ/##" | while read -r l; do echo "  ✗ use TelaDiario em vez de Scaffold: $l"; done
  n=$(grep -rnE "\bScaffold[[:space:]]*[({]" "$dir" --include=*.kt | wc -l); problemas=$((problemas + n))

  # 7. Toda funcionalidade tem teste na JVM.
  find "$TESTE/$p" -name "*Test.kt" 2>/dev/null | grep -q . || falha "sem testes em ${TESTE#$RAIZ/}/$p"
done

# 8. Schema exportado da versão atual do banco está commitado.
versao=$(grep -oE "version = [0-9]+" "$DB" | grep -oE "[0-9]+")
schema="$RAIZ/app/schemas/${BASE//\//.}.core.database.DiarioDatabase/$versao.json"
[ -f "$schema" ] || { echo "banco:"; falha "schema da versão $versao não está em ${schema#$RAIZ/} (rode ./gradlew assembleDebug e commite)"; }

if [ $problemas -gt 0 ]; then
  echo "$problemas problema(s) fora do padrão."
  exit 1
fi
echo "Tudo no padrão."
