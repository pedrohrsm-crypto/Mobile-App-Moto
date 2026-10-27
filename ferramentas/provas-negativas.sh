#!/usr/bin/env bash
# Provas negativas conferidas: todo @Test acrescentado no diff do PR contra a base precisa aparecer, pelo nome
# da função, no corpo do PR. O corpo é entrada não confiável: chega só por variável de ambiente.
# Base: no CI o checkout é o merge ref do PR contra a main atual, então a base é o primeiro pai do merge
# (HEAD^1); a base.sha do evento pode ser antiga e cobraria testes alheios. Na bancada, BASE_SHA=<sha> força a base.
# Uso: CORPO_PR="<corpo>" bash ferramentas/provas-negativas.sh   (ou BASE_SHA=<sha> CORPO_PR=... bash ...)
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"
corpo="${CORPO_PR:-}"
base="${BASE_SHA:-}"
if [ -z "$base" ]; then
  if git rev-parse --verify -q HEAD^2 >/dev/null; then
    base="$(git rev-parse HEAD^1)"
  else
    echo "✗ HEAD não é um merge ref: informe BASE_SHA=<sha da base>"
    exit 1
  fi
fi
echo "Base do diff: $base"

diff_testes="$(git diff -U3 --no-color "$base"...HEAD -- '*.kt')" || { echo "✗ git diff falhou contra $base"; exit 1; }

# Nomes de função de teste (anotados com @Test ou @ParameterizedTest) em linhas acrescentadas ("+") ou removidas ("-").
# Linhas do outro sinal são ignoradas: o "-fun antigo" de um teste renomeado no lugar não pode consumir o @Test de contexto.
nomes() {
  local sinal=$1
  echo "$diff_testes" | awk -v sinal="$sinal" '
    /^diff --git / { dentro = ($0 ~ /\/src\/(test|androidTest)\//); pend = 0; next }
    !dentro { next }
    /^(\+\+\+|---) / { next }
    {
      marca = substr($0, 1, 1); linha = substr($0, 2)
      if (marca != " " && marca != sinal) next
      if (linha ~ /^[ \t]*(\/\/|\/\*|\*)/) next
      if (linha ~ /@(Test|ParameterizedTest)([^A-Za-z0-9_]|$)/ && (marca == " " || marca == sinal)) { pend = 1 }
      if (pend && linha ~ /fun[ \t]+/) {
        if (marca == sinal) {
          nome = linha
          sub(/.*fun[ \t]+/, "", nome)
          if (nome ~ /^`/) { sub(/^`/, "", nome); sub(/`.*/, "", nome) } else { sub(/[^A-Za-z0-9_].*/, "", nome) }
          print nome
        }
        pend = 0
      } else if (linha !~ /^[ \t]*(@|$)/ && linha !~ /@(Test|ParameterizedTest)/) { pend = 0 }
    }' | sort -u
}

novos="$(nomes '+')"
removidos="$(nomes '-')"
# Teste movido ou reformatado (mesmo nome sai e entra) não é teste novo.
novos="$(comm -23 <(echo "$novos") <(echo "$removidos") | sed '/^$/d')"

if [ -z "$novos" ]; then
  echo "Provas negativas: nenhum @Test novo no diff."
  exit 0
fi

faltando=""
while IFS= read -r nome; do
  if ! grep -qwF -- "$nome" <<<"$corpo"; then
    faltando+="    $nome"$'\n'
  fi
done <<<"$novos"

if [ -n "$faltando" ]; then
  echo "✗ @Test novo que não aparece no corpo do PR (cite cada teste pelo nome na tabela \"Provas negativas\"):"
  printf '%s' "$faltando"
  exit 1
fi
echo "Provas negativas: $(echo "$novos" | wc -l) @Test novo(s), todos citados no corpo do PR."
