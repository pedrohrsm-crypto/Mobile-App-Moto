#!/usr/bin/env bash
# Casos do provas-negativas.sh, em repositórios temporários. Rodar: bash ferramentas/testes/provas-negativas.test.sh
set -uo pipefail
script="$(git rev-parse --show-toplevel)/ferramentas/provas-negativas.sh"
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
falhas=0
T=app/src/test/java/x/AlvoTest.kt

novo_repo() { # $1 = conteúdo do arquivo na base
  rm -rf "$tmp/r"; mkdir -p "$tmp/r/$(dirname "$T")"; cd "$tmp/r" || exit 1
  git init -q . && git config user.email t@t && git config user.name t
  printf '%s\n' "$1" > "$T"; git add -A; git commit -qm base; base="$(git rev-parse HEAD)"
}
caso() { # nome, esperado, conteúdo na cabeça, corpo, [trecho]
  local nome=$1 esperado=$2 cabeca=$3 corpo=$4 trecho=${5:-}
  printf '%s\n' "$cabeca" > "$T"; git add -A; git commit -qm cabeca
  saida="$(BASE_SHA="$base" CORPO_PR="$corpo" bash "$script" 2>&1)"; codigo=$?
  if [ "$codigo" -ne "$esperado" ] || { [ -n "$trecho" ] && ! grep -qF -- "$trecho" <<<"$saida"; }; then
    echo "✗ $nome (saiu $codigo, esperado $esperado)"; echo "$saida" | sed 's/^/    /'; falhas=$((falhas + 1))
  else
    echo "✓ $nome"
  fi
}

BASE=$'class AlvoTest {\n    @Test\n    fun existente() {}\n}'

novo_repo "$BASE"
caso "nome ausente do corpo reprova" 1 $'class AlvoTest {\n    @Test\n    fun existente() {}\n\n    @Test\n    fun novoTeste() {}\n}' "tabela sem o teste" "novoTeste"

novo_repo "$BASE"
caso "nome presente no corpo passa" 0 $'class AlvoTest {\n    @Test\n    fun existente() {}\n\n    @Test\n    fun novoTeste() {}\n}' "| novoTeste | bug plantado | vermelho |"

novo_repo "$BASE"
caso "teste removido não exige citação" 0 $'class AlvoTest {\n}' ""

novo_repo "$BASE"
caso "teste parametrizado exige o nome da função" 1 $'class AlvoTest {\n    @Test\n    fun existente() {}\n\n    @ParameterizedTest\n    @ValueSource(ints = [1, 2])\n    fun varios(n: Int) {}\n}' "nada" "varios"

novo_repo "$BASE"
caso "teste parametrizado citado passa" 0 $'class AlvoTest {\n    @Test\n    fun existente() {}\n\n    @ParameterizedTest\n    @ValueSource(ints = [1, 2])\n    fun varios(n: Int) {}\n}' "varios"

novo_repo "$BASE"
caso "nome com crase (espaços) é lido inteiro" 1 $'class AlvoTest {\n    @Test\n    fun existente() {}\n\n    @Test\n    fun `calcula o saldo`() {}\n}' "calcula" "calcula o saldo"

novo_repo $'class AlvoTest {\n    @Test\n    fun primeiro() {}\n\n    @Test\n    fun segundo() {}\n}'
caso "teste só movido de lugar não conta como novo" 0 $'class AlvoTest {\n    @Test\n    fun segundo() {}\n\n    @Test\n    fun primeiro() {}\n}' ""

novo_repo "$BASE"
caso "corpo vazio sem teste novo passa" 0 $'class AlvoTest {\n    @Test\n    fun existente() {}\n    fun auxiliar() {}\n}' ""

novo_repo "$BASE"
caso "função sem @Test não conta" 0 $'class AlvoTest {\n    @Test\n    fun existente() {}\n\n    private fun auxiliar() {}\n}' ""

novo_repo "$BASE"
caso "corpo com aspas e cifrão não é executado" 0 $'class AlvoTest {\n    @Test\n    fun existente() {}\n\n    @Test\n    fun seguro() {}\n}' $'seguro `touch /tmp/pwned-pn` $(touch /tmp/pwned-pn) "'
[ ! -e /tmp/pwned-pn ] && echo "✓ corpo não foi interpretado pelo shell" || { echo "✗ corpo foi interpretado pelo shell"; rm -f /tmp/pwned-pn; falhas=$((falhas + 1)); }

novo_repo "$BASE"
caso "teste reescrito no lugar exige citação" 1 $'class AlvoTest {\n    @Test\n    fun reescrito() {}\n}' "nada" "reescrito"

novo_repo "$BASE"
caso "@Test em comentário não conta" 0 $'class AlvoTest {\n    @Test\n    fun existente() {}\n\n    // @Test\n    fun comentado() {}\n\n    /*\n     * @Test\n     */\n    fun emBloco() {}\n}' ""

novo_repo "$BASE"
caso "nome só como parte de outra palavra não conta como citado" 1 $'class AlvoTest {\n    @Test\n    fun existente() {}\n\n    @Test\n    fun teto() {}\n}' "a tabela fala de tetos" "teto"

# Base avançada: a main ganhou um teste alheio depois que o PR saiu. No CI o checkout é o merge ref; a base é HEAD^1.
novo_repo "$BASE"
git branch -M main; base_antiga="$base"
git checkout -q -b feat
printf '%s\n' $'class AlvoTest {\n    @Test\n    fun existente() {}\n\n    @Test\n    fun proprio() {}\n}' > "$T"; git add -A; git commit -qm feat
git checkout -q main
printf '%s\n' $'class OutroTest {\n    @Test\n    fun alheio() {}\n}' > "$(dirname "$T")/OutroTest.kt"; git add -A; git commit -qm alheio
git merge -q --no-ff feat -m merge >/dev/null 2>&1
saida="$(CORPO_PR='só o proprio' bash "$script" 2>&1)"; codigo=$?
if [ "$codigo" -eq 0 ] && grep -qF "Base do diff: $(git rev-parse HEAD^1)" <<<"$saida"; then
  echo "✓ base avançou com teste alheio não exige citação"
else
  echo "✗ base avançou com teste alheio não exige citação (saiu $codigo)"; echo "$saida" | sed 's/^/    /'; falhas=$((falhas + 1))
fi
saida="$(BASE_SHA="$base_antiga" CORPO_PR='só o proprio' bash "$script" 2>&1)"; codigo=$?
if [ "$codigo" -eq 1 ] && grep -qF "alheio" <<<"$saida"; then
  echo "✓ BASE_SHA antiga cobra o teste alheio (era o falso positivo)"
else
  echo "✗ BASE_SHA antiga cobra o teste alheio (saiu $codigo)"; echo "$saida" | sed 's/^/    /'; falhas=$((falhas + 1))
fi

novo_repo "$BASE"
printf '%s\n' $'class AlvoTest {\n}' > "$T"; git add -A; git commit -qm cabeca
saida="$(CORPO_PR='' bash "$script" 2>&1)"; codigo=$?
if [ "$codigo" -eq 1 ] && grep -qF "BASE_SHA" <<<"$saida"; then
  echo "✓ sem merge ref e sem BASE_SHA reprova"
else
  echo "✗ sem merge ref e sem BASE_SHA reprova (saiu $codigo)"; echo "$saida" | sed 's/^/    /'; falhas=$((falhas + 1))
fi

[ "$falhas" -eq 0 ] && echo "provas-negativas: todos os casos passaram." || { echo "$falhas caso(s) falharam."; exit 1; }
