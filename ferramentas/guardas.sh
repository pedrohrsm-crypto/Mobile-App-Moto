#!/usr/bin/env bash
# Guardas do projeto que não dependem do Android SDK. Roda no CI e na bancada: bash ferramentas/guardas.sh
# Sai com 1 e lista cada ocorrência quando alguma regra do CLAUDE.md é quebrada.
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"
problemas=0

reprovar() {
  local titulo=$1 saida=$2
  if [ -n "$saida" ]; then
    echo "✗ $titulo"
    echo "$saida" | sed 's/^/    /'
    problemas=$((problemas + $(echo "$saida" | wc -l)))
  fi
}

# 1. Português do Brasil nos textos da interface e no código (lição de idioma do framework).
reprovar "variante europeia do português" "$(grep -rniE '\b(registad[oa]s?|ficheiros?|ecrãs?|utilizador(es)?|telemóve(l|is)|equipa)\b' \
  app/src/main/res app/src/main/java 2>/dev/null)"

# 2. Cor só vem do ColorScheme: literal de cor em Kotlin só no pacote do tema.
reprovar "cor literal fora de core/designsystem/theme" "$(grep -rnE 'Color\(0x|Color\.(Red|Blue|Green|Black|White|Gray|Yellow|Cyan|Magenta)\b' \
  app/src/main/java --include=*.kt | grep -v '/core/designsystem/theme/')"

# 3. Nenhum texto fixo na interface: todo texto vem de strings.xml.
reprovar "texto fixo em Text(\"...\")" "$(grep -rnE '\bText\("' app/src/main/java --include=*.kt)"

# 4. Nada de log no código do app: coordenada, placa ou texto livre não podem vazar por logcat (risco #23).
reprovar "log no código do app (risco #23)" "$(grep -rnE 'android\.util\.Log\b|\bLog\.(v|d|i|w|e|wtf)\(|\bprintln\(|\.printStackTrace\(' \
  app/src/main/java --include=*.kt)"

if [ "$problemas" -gt 0 ]; then
  echo "$problemas ocorrência(s) fora das regras."
  exit 1
fi
echo "Guardas: tudo dentro das regras."
