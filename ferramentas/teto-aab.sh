#!/usr/bin/env bash
# Teto do AAB: reprova se o AAB passar do teto; avisa, sem reprovar, quando cair (para baixar o teto).
# Uso: bash ferramentas/teto-aab.sh [arquivo.aab] [arquivo-do-teto]
set -uo pipefail
raiz="$(git rev-parse --show-toplevel)"
aab="${1:-$raiz/app/build/outputs/bundle/release/app-release.aab}"
arquivo_teto="${2:-$raiz/ferramentas/teto-aab.txt}"

if [ ! -f "$aab" ]; then
  echo "✗ AAB não encontrado: $aab (rode ./gradlew bundleRelease antes)"
  exit 1
fi
if [ ! -f "$arquivo_teto" ]; then
  echo "✗ arquivo do teto não encontrado: $arquivo_teto"
  exit 1
fi

teto="$(grep -vE '^[[:space:]]*(#|$)' "$arquivo_teto" | head -n 1 | tr -d '[:space:]')"
if ! [[ "$teto" =~ ^[0-9]+$ ]]; then
  echo "✗ $arquivo_teto não tem um número de bytes na primeira linha útil: '$teto'"
  exit 1
fi

tamanho="$(stat -c %s "$aab")"
echo "AAB: $tamanho B · teto: $teto B"
if [ "$tamanho" -gt "$teto" ]; then
  echo "✗ o AAB passou do teto em $((tamanho - teto)) B. Suba o teto em ferramentas/teto-aab.txt com o motivo na mensagem do commit (o PM aceita) ou reduza o AAB."
  exit 1
fi
if [ "$tamanho" -lt "$teto" ]; then
  echo "::warning::o AAB está $((teto - tamanho)) B abaixo do teto; baixe ferramentas/teto-aab.txt para $tamanho."
  echo "Aviso: teto pode baixar para $tamanho (sem reprovar)."
fi
echo "Teto do AAB: dentro."
