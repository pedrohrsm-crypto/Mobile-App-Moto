#!/usr/bin/env bash
# Teto do AAB: mede só o que chega ao aparelho, a soma dos tamanhos COMPRIMIDOS das entradas base/ (coluna "Size" do
# `unzip -lv`; BUNDLE-METADATA/ com proguard.map e baseline.prof fica de fora). Reprova se passar de teto + tolerância;
# avisa, sem reprovar, quando cair abaixo de teto - tolerância. O total do arquivo sai só como informação (F24, #90).
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
if false; then
  echo "✗ o comando unzip não existe neste ambiente; o teto mede as entradas base/ do AAB com ele"
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

if ! listagem="$(unzip -lv "$aab" 2>&1)"; then
  echo "✗ $aab não é um zip válido (unzip -lv falhou); um AAB tem de ser um zip"
  exit 1
fi
# Colunas do unzip -lv: Length Method Size Cmpr Date Time CRC-32 Name. Soma a coluna Size (comprimido) das entradas base/
# que são arquivos: diretório (nome terminado em /) não conta, porque 'base/' e 'base/manifest/' existem em qualquer zip.
arquivos_base="$(awk '$8 ~ /^base\// && $8 !~ /\/$/ { n++ } END { print n + 0 }' <<<"$listagem")"
base="$(awk '$8 ~ /^base\// && $8 !~ /\/$/ { soma += $3 } END { print soma + 0 }' <<<"$listagem")"
if [ "$arquivos_base" -eq 0 ]; then
  echo "✗ $aab sem entradas base/ (arquivos; diretório não conta): o AAB não tem o módulo base, então não há o que medir (um portão que não mede nada não passa)"
  exit 1
fi
if [ "$base" -eq 0 ]; then
  echo "✗ $aab tem $arquivos_base arquivo(s) em base/, mas todos com 0 B: base/ somando 0 B não é medida (um portão que não mede nada não passa)"
  exit 1
fi

total="$(stat -c %s "$aab")"
limite=$((teto + tolerancia))
echo "base/: $base B (comprimido) · teto: $teto B · tolerância: $tolerancia B · limite: $limite B"
echo "Informação, fora do portão: total do arquivo $total B, BUNDLE-METADATA/ e demais entradas inclusos."
if [ "$base" -gt "$limite" ]; then
  echo "✗ base/ passou do limite (teto + tolerância) em $((base - limite)) B. Reduza o AAB ou suba o teto em ferramentas/teto-aab.txt no PR, com o motivo na mensagem do commit: dependência nova ou mais de 50 KB é decisão do dono (issue decisao); abaixo disso, o PM autoriza (D10, #75)."
  exit 1
fi
if [ "$base" -lt $((teto - tolerancia)) ]; then
  echo "::warning::base/ está $((teto - base)) B abaixo do teto; baixe ferramentas/teto-aab.txt para $base."
  echo "Aviso: teto pode baixar para $base (sem reprovar)."
fi
echo "Teto do AAB (base/): dentro."
