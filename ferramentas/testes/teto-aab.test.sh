#!/usr/bin/env bash
# Casos do teto-aab.sh. Rodar: bash ferramentas/testes/teto-aab.test.sh
# Cada caso monta um zip de verdade (python3, ZIP_STORED: tamanho comprimido = tamanho do conteúdo) com entradas base/ e BUNDLE-METADATA/.
set -uo pipefail
script="$(git rev-parse --show-toplevel)/ferramentas/teto-aab.sh"
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
falhas=0

# montar_zip <destino> <bytes em base/> <bytes em BUNDLE-METADATA/> [zeros]: com "zeros" o conteúdo é comprimido (deflate), para
# o tamanho comprimido ficar bem abaixo do descomprimido. Com 0 em um dos tamanhos, a pasta não entra no zip.
montar_zip() {
  python3 -I - "$@" <<'PY'
import sys, zipfile
destino, base, meta = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
modo = zipfile.ZIP_DEFLATED if len(sys.argv) > 4 else zipfile.ZIP_STORED
with zipfile.ZipFile(destino, "w", modo) as z:
    z.writestr("BundleConfig.pb", b"\0" * 16, compress_type=zipfile.ZIP_STORED)
    if base:
        z.writestr("base/dex/classes.dex", b"\0" * base)
    if meta:
        z.writestr("BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map", b"\0" * meta)
PY
}

caso() { # nome, esperado, bytes em base/, bytes em BUNDLE-METADATA/, conteúdo do arquivo do teto, [trecho esperado na saída], [trecho que não pode aparecer], [zeros]
  local nome=$1 esperado=$2 base=$3 meta=$4 teto=$5 trecho=${6:-} proibido=${7:-}
  montar_zip "$tmp/a.aab" "$base" "$meta" ${8:-}
  printf '%b' "$teto" > "$tmp/teto.txt"
  rodar "$nome" "$esperado" "$tmp/a.aab" "$trecho" "$proibido"
}

rodar() { # nome, esperado, arquivo, trecho, proibido
  local nome=$1 esperado=$2 arquivo=$3 trecho=${4:-} proibido=${5:-}
  saida="$(bash "$script" "$arquivo" "$tmp/teto.txt" 2>&1)"; codigo=$?
  if [ "$codigo" -ne "$esperado" ] || { [ -n "$trecho" ] && ! grep -qF -- "$trecho" <<<"$saida"; } || { [ -n "$proibido" ] && grep -qF -- "$proibido" <<<"$saida"; }; then
    echo "✗ $nome (saiu $codigo, esperado $esperado)"; echo "$saida" | sed 's/^/    /'; falhas=$((falhas + 1))
  else
    echo "✓ $nome"
  fi
}

T=$'teto: 10000\ntolerancia: 2048\n'
caso "1 B acima de teto + tolerância reprova" 1 12049 0 "$T" "passou do limite"
caso "a reprovação diz quem decide subir o teto (D10)" 1 12049 0 "$T" "mais de 50 KB é decisão do dono"
caso "exatamente em teto + tolerância passa" 0 12048 0 "$T" "dentro"
caso "acima do teto, dentro da tolerância, passa" 0 10100 0 "$T" "dentro"
caso "abaixo de teto - tolerância avisa sem reprovar" 0 7951 0 "$T" "::warning::"
caso "exatamente em teto - tolerância não avisa" 0 7952 0 "$T" "dentro" "::warning::"
caso "comentários e linhas em branco são ignorados" 0 10000 0 $'# motivo\n\nteto: 10000\ntolerancia: 0\n' "dentro"
caso "teto inválido reprova" 1 1000 0 $'teto: mil\ntolerancia: 0\n' "precisa de uma linha"
caso "tolerância ausente reprova" 1 1000 0 $'teto: 1000\n' "tolerancia"
bash "$script" "$tmp/inexistente.aab" "$tmp/teto.txt" >/dev/null 2>&1; [ $? -eq 1 ] && echo "✓ AAB ausente reprova" || { echo "✗ AAB ausente reprova"; falhas=$((falhas + 1)); }

# Casos da F24 (#90): o portão mede só base/ (o que chega ao aparelho), pelo tamanho comprimido.
caso "BUNDLE-METADATA/ grande não reprova (a)" 0 10000 500000 "$T" "dentro"
caso "base/ acima do limite reprova mesmo com BUNDLE-METADATA/ pequeno (b)" 1 12049 10 "$T" "passou do limite"
printf 'isto não é um zip\n' > "$tmp/lixo.aab"; printf '%b' "$T" > "$tmp/teto.txt"
rodar "arquivo que não é zip reprova (c)" 1 "$tmp/lixo.aab" "não é um zip válido"
caso "zip sem base/ reprova (d)" 1 0 5000 "$T" "sem entradas base/"
caso "zip só com a pasta base/ vazia reprova (d)" 1 0 0 "$T" "sem entradas base/"
caso "usa o tamanho comprimido, não o descomprimido (e)" 0 2000000 0 "$T" "dentro" "" zeros
caso "imprime o total do arquivo só como informação (f)" 0 10000 300000 "$T" "total do arquivo"

[ "$falhas" -eq 0 ] && echo "teto-aab: todos os casos passaram." || { echo "$falhas caso(s) falharam."; exit 1; }
