#!/usr/bin/env bash
# Mensagem de commit sem identificador de modelo (F26, #92; regra da segunda ocorrência, PFA-01 2.3).
# O CLAUDE.md proíbe nome ou id de modelo, "Co-Authored-By" de IA e rodapé de sessão em commit; esta guarda faz o CI cumprir.
#
# Uso:
#   bash ferramentas/mensagens-commit.sh BASE..HEAD   intervalo explícito (bancada)
#   bash ferramentas/mensagens-commit.sh              no CI:
#     - PR: HEAD é o merge ref; lê os commits do PR (HEAD^1..HEAD^2), sem os da main nem o commit de merge;
#     - push na main: lê ANTES..HEAD (ANTES = github.event.before, por env); sem ANTES válido, só o commit novo (HEAD^..HEAD).
#
# Nunca passa calado: intervalo malformado, base que não existe ou git que falha saem com 1.
# Intervalo válido e sem commits sai com 0, avisando "0 commits lidos".
#
# Padrões (sem diferenciar maiúscula de minúscula; linha por linha, assunto e corpo). Estreitos de propósito:
# a palavra "claude" sozinha NÃO reprova ("docs: ajusta o CLAUDE.md", "skill do Claude Code" passam), porque nomeia
# o arquivo e a ferramenta do projeto, não o modelo que escreveu o commit.
#   id-de-modelo   claude-opus|sonnet|haiku|fable|instant ou claude-<dígito> (ex.: claude-sonnet-5-5, claude-3-5-sonnet)
#   nome-de-modelo Claude seguido de Opus, Sonnet, Haiku ou Fable, com espaço ou hífen
#   coautor-ia     "Co-Authored-By:" com anthropic.com ou com nome começando em Claude
#   rodape-sessao  linha de rodapé "Claude-Session:"
#   link-sessao    link claude.ai/code/session (a mesma classe do rodapé, colado no texto)
set -uo pipefail
cd "$(git rev-parse --show-toplevel)" || exit 1

PADROES=(
  'id-de-modelo|\bclaude-(opus|sonnet|haiku|fable|instant|[0-9])'
  'nome-de-modelo|\bclaude[ -]+(opus|sonnet|haiku|fable)\b'
  'rodape-sessao|^[[:space:]]*claude-session:'
  'link-sessao|claude\.ai/code/session'
)

erro() { echo "✗ $*" >&2; exit 1; }

zero='^0+$'
if [ $# -ge 1 ]; then
  intervalo=$1
elif git rev-parse --verify -q HEAD^2 >/dev/null; then
  intervalo="HEAD^1..HEAD^2"
elif [ -n "${ANTES:-}" ] && ! [[ "$ANTES" =~ $zero ]]; then
  intervalo="$ANTES..HEAD"
else
  intervalo="HEAD^..HEAD"
fi

esquerda="${intervalo%%..*}"
direita="${intervalo##*..}"
case "$intervalo" in
  *...*) erro "intervalo \"$intervalo\" com três pontos: use BASE..HEAD" ;;
  *..*) ;;
  *) erro "intervalo \"$intervalo\" sem \"..\": use BASE..HEAD" ;;
esac
[ -n "$esquerda" ] || erro "base vazia em \"$intervalo\": sem base, nada seria lido"
[ -n "$direita" ] || erro "ponta vazia em \"$intervalo\""

commits="$(git rev-list --reverse "$intervalo" 2>&1)" || erro "git rev-list falhou em \"$intervalo\" (base fora do histórico? checkout raso?): $commits"
echo "Intervalo lido: $intervalo"
if [ -z "$commits" ]; then
  echo "Mensagens de commit: 0 commits lidos no intervalo."
  exit 0
fi

lidos=0
reprovados=0
while IFS= read -r commit; do
  lidos=$((lidos + 1))
  mensagem="$(git log -1 --format=%B "$commit")" || erro "git log falhou no commit $commit"
  for entrada in "${PADROES[@]}"; do
    nome="${entrada%%|*}"
    regex="${entrada#*|}"
    achou="$(grep -inE -e "$regex" <<<"$mensagem")"
    if [ -n "$achou" ]; then
      while IFS= read -r linha; do
        echo "✗ commit ${commit:0:7}, linha ${linha%%:*} (padrão $nome): ${linha#*:}"
        reprovados=$((reprovados + 1))
      done <<<"$achou"
    fi
  done
done <<<"$commits"

if [ "$reprovados" -gt 0 ]; then
  echo "✗ $reprovados ocorrência(s) de identificador de modelo em $lidos commit(s). Reescreva a mensagem sem o trecho (git rebase, git commit --amend)."
  exit 1
fi
echo "Mensagens de commit: $lidos commit(s) lido(s), nenhum identificador de modelo."
