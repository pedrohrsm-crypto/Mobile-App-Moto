#!/usr/bin/env bash
# Casos do teto-aab.sh. Rodar: bash ferramentas/testes/teto-aab.test.sh
set -uo pipefail
script="$(git rev-parse --show-toplevel)/ferramentas/teto-aab.sh"
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
falhas=0
caso() { # nome, esperado, tamanho, teto-conteúdo, [trecho esperado na saída]
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
caso "teto 1 byte abaixo do AAB reprova" 1 1000 "999\n" "passou do teto em 1 B"
caso "AAB igual ao teto passa" 0 1000 "1000\n" "dentro"
caso "AAB menor avisa sem reprovar" 0 900 "1000\n" "::warning::"
caso "comentários e linhas em branco são ignorados" 0 1000 "# motivo\n\n1000\n"
caso "teto inválido reprova" 1 1000 "mil\n" "não tem um número"
bash "$script" "$tmp/inexistente.aab" "$tmp/teto.txt" >/dev/null 2>&1; [ $? -eq 1 ] && echo "✓ AAB ausente reprova" || { echo "✗ AAB ausente reprova"; falhas=$((falhas + 1)); }
[ "$falhas" -eq 0 ] && echo "teto-aab: todos os casos passaram." || { echo "$falhas caso(s) falharam."; exit 1; }
