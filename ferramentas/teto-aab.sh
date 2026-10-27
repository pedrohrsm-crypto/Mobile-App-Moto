#!/usr/bin/env bash
# Teto do AAB: reprova se o AAB passar de teto + tolerância; avisa, sem reprovar, quando cair abaixo de teto - tolerância.
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

valor() { grep -E "^$1:" "$arquivo_teto" | head -n 1 | cut -d: -f2 | tr -d '[:space:]'; }
teto="$(valor teto)"
tolerancia="$(valor tolerancia)"
for chave in teto tolerancia; do
  if ! [[ "${!chave}" =~ ^[0-9]+$ ]]; then
    echo "✗ $arquivo_teto precisa de uma linha '$chave: <bytes>' com número inteiro (achei '${!chave}')"
    exit 1
  fi
done

tamanho="$(stat -c %s "$aab")"
limite=$((teto + tolerancia))
echo "AAB: $tamanho B · teto: $teto B · tolerância: $tolerancia B · limite: $limite B"
if [ "$tamanho" -gt "$limite" ]; then
  echo "✗ o AAB passou do limite (teto + tolerância) em $((tamanho - limite)) B. Suba o teto em ferramentas/teto-aab.txt com o motivo na mensagem do commit (o PM aceita) ou reduza o AAB."
  exit 1
fi
if [ "$tamanho" -lt $((teto - tolerancia)) ]; then
  echo "::warning::o AAB está $((teto - tamanho)) B abaixo do teto; baixe ferramentas/teto-aab.txt para $tamanho."
  echo "Aviso: teto pode baixar para $tamanho (sem reprovar)."
fi
echo "Teto do AAB: dentro."
