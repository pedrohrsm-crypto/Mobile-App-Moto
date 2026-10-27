#!/usr/bin/env bash
# Provas negativas conferidas: todo @Test acrescentado no diff do PR contra a base precisa aparecer, pelo nome
# da função, no corpo do PR. O corpo é entrada não confiável: chega só por variável de ambiente.
# Uso: BASE_SHA=<sha da base> CORPO_PR="<corpo>" bash ferramentas/provas-negativas.sh
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"
base="${BASE_SHA:-}"
corpo="${CORPO_PR:-}"
if [ -z "$base" ]; then
  echo "✗ BASE_SHA não informado"
  exit 1
fi

diff_testes="$(git diff -U3 --no-color "$base"...HEAD -- '*.kt')" || { echo "✗ git diff falhou contra $base"; exit 1; }

# Nomes de função de teste (anotados com @Test ou @ParameterizedTest) em linhas acrescentadas ("+") ou removidas ("-").
nomes() {
  local sinal=$1
  echo "$diff_testes" | awk -v sinal="$sinal" '
    /^diff --git / { dentro = ($0 ~ /\/src\/(test|androidTest)\//); pend = 0; next }
    !dentro { next }
    /^(\+\+\+|---) / { next }
    {
      marca = substr($0, 1, 1); linha = substr($0, 2)
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
  if ! grep -qF -- "$nome" <<<"$corpo"; then
    faltando+="    $nome"$'\n'
  fi
done <<<"$novos"

if [ -n "$faltando" ]; then
  echo "✗ @Test novo que não aparece no corpo do PR (cite cada teste pelo nome na tabela \"Provas negativas\"):"
  printf '%s' "$faltando"
  exit 1
fi
echo "Provas negativas: $(echo "$novos" | wc -l) @Test novo(s), todos citados no corpo do PR."
