#!/usr/bin/env bash
# Casos do teto-aab.sh. Rodar: bash ferramentas/testes/teto-aab.test.sh
set -uo pipefail
script="$(git rev-parse --show-toplevel)/ferramentas/teto-aab.sh"
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
falhas=0
caso() { # nome, esperado, tamanho, conteúdo do arquivo do teto, [trecho esperado na saída]
  local nome=$1 esperado=$2 tamanho=$3 teto=$4 trecho=${5:-}
  head -c "$tamanho" /dev/zero > "$tmp/a.aab"
  printf '%b' "$teto" > "$tmp/teto.txt"
  saida="$(bash "$script" "$tmp/a.aab" "$tmp/teto.txt" 2>&1)"; codigo=$?
  if [ "$codigo" -ne "$esperado" ] || { [ -n "$trecho" ] && ! grep -qF -- "$trecho" <<<"$saida"; }; then
    echo "✗ $nome (saiu $codigo, esperado $esperado)"; echo "$saida" | sed 's/^/    /'; falhas=$((falhas + 1))
  else
    echo "✓ $nome"
  fi
}
T=$'teto: 10000\ntolerancia: 2048\n'
caso "1 B acima de teto + tolerância reprova" 1 12049 "$T" "passou do limite"
caso "exatamente em teto + tolerância passa" 0 12048 "$T" "dentro"
caso "acima do teto, dentro da tolerância, passa" 0 10100 "$T" "dentro"
caso "abaixo de teto - tolerância avisa sem reprovar" 0 7951 "$T" "::warning::"
caso "exatamente em teto - tolerância não avisa" 0 7952 "$T" "dentro"
caso "comentários e linhas em branco são ignorados" 0 10000 $'# motivo\n\nteto: 10000\ntolerancia: 0\n' "dentro"
caso "teto inválido reprova" 1 1000 $'teto: mil\ntolerancia: 0\n' "precisa de uma linha"
caso "tolerância ausente reprova" 1 1000 $'teto: 1000\n' "tolerancia"
bash "$script" "$tmp/inexistente.aab" "$tmp/teto.txt" >/dev/null 2>&1; [ $? -eq 1 ] && echo "✓ AAB ausente reprova" || { echo "✗ AAB ausente reprova"; falhas=$((falhas + 1)); }
[ "$falhas" -eq 0 ] && echo "teto-aab: todos os casos passaram." || { echo "$falhas caso(s) falharam."; exit 1; }
