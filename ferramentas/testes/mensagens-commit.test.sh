#!/usr/bin/env bash
# Casos do mensagens-commit.sh, em repositórios git temporários. Rodar: bash ferramentas/testes/mensagens-commit.test.sh
# A guarda lê só mensagens de commit, não arquivos: os trechos proibidos podem aparecer aqui como texto de teste.
set -uo pipefail
script="$(git rev-parse --show-toplevel)/ferramentas/mensagens-commit.sh"
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
falhas=0

novo_repo() {
  rm -rf "$tmp/r"; mkdir -p "$tmp/r"; cd "$tmp/r" || exit 1
  git init -q -b main . && git config user.email t@t && git config user.name t
  git commit -q --allow-empty -m "chore: base"; base="$(git rev-parse HEAD)"
}
commitar() { git commit -q --allow-empty -m "$1"; }

# caso <nome> <esperado> <trecho da saída> [env...]  (a partir do intervalo $intervalo, vazio = modo sem argumento)
caso() {
  local nome=$1 esperado=$2 trecho=$3; shift 3
  local saida codigo
  saida="$(env "$@" bash "$script" ${intervalo:+"$intervalo"} 2>&1)"; codigo=$?
  if [ "$codigo" -ne "$esperado" ] || { [ -n "$trecho" ] && ! grep -qF -- "$trecho" <<<"$saida"; }; then
    echo "✗ $nome (saiu $codigo, esperado $esperado; trecho esperado: \"$trecho\")"; echo "$saida" | sed 's/^/    /'; falhas=$((falhas + 1))
  else
    echo "✓ $nome"
  fi
}

# --- Passam ---
novo_repo; commitar "feat(garagem): cadastro de moto (#12)"; intervalo="$base..HEAD"
caso "mensagem limpa passa" 0 "nenhum identificador de modelo" X=1

novo_repo; commitar $'fix(diario): ajusta o total\n\nCorpo longo, sem nada de IA.\n\nCloses #3'; intervalo="$base..HEAD"
caso "assunto e corpo comuns passam" 0 "1 commit(s) lido(s)" X=1

novo_repo; commitar "docs: ajusta o CLAUDE.md e a skill do Claude Code (#4)"; intervalo="$base..HEAD"
caso "a palavra claude sozinha, em contexto de projeto, passa" 0 "nenhum identificador de modelo" X=1

novo_repo; intervalo="$base..HEAD"
caso "intervalo válido sem commits passa avisando" 0 "0 commits lidos" X=1

novo_repo; commitar "chore: segundo"; intervalo="$base..HEAD"
caso "intervalo de um commit lê só ele" 0 "1 commit(s) lido(s)" X=1

# --- Cada padrão reprova ---
novo_repo; commitar "feat: usa claude-sonnet-5-5 (#1)"; intervalo="$base..HEAD"
caso "padrão id-de-modelo reprova" 1 "(padrão id-de-modelo)" X=1

novo_repo; commitar "feat: id antigo claude-3-5-sonnet (#1)"; intervalo="$base..HEAD"
caso "id-de-modelo também pega o id que começa por dígito" 1 "(padrão id-de-modelo)" X=1

novo_repo; commitar $'fix: texto\n\nFeito com Claude'" Sonnet 5.5"; intervalo="$base..HEAD"
caso "padrão nome-de-modelo reprova" 1 "(padrão nome-de-modelo)" X=1

novo_repo; commitar $'fix: texto\n\nescrito pelo '"CLAUDE"$'-opus'; intervalo="$base..HEAD"
caso "nome-de-modelo reprova em maiúsculas e com hífen" 1 "(padrão nome-de-modelo)" X=1

novo_repo; commitar $'fix: texto\n\nCo-Authored-By: Alguém <ia@anthropic'".com>"; intervalo="$base..HEAD"
caso "padrão coautor-ia reprova pelo domínio" 1 "(padrão coautor-ia)" X=1

novo_repo; commitar $'fix: texto\n\nco-authored-by: Cla'"ude <x@y.z>"; intervalo="$base..HEAD"
caso "coautor-ia reprova pelo nome, em minúsculas" 1 "(padrão coautor-ia)" X=1

novo_repo; commitar $'fix: texto\n\nCo-Authored-By: Maria <maria@exemplo.com.br>'; intervalo="$base..HEAD"
caso "coautor humano passa" 0 "nenhum identificador de modelo" X=1

novo_repo; commitar $'fix: texto\n\nCla'"ude-Session: https://exemplo/s"; intervalo="$base..HEAD"
caso "padrão rodape-sessao reprova" 1 "(padrão rodape-sessao)" X=1

novo_repo; commitar $'fix: texto\n\nveja https://cla'"ude.ai/code/session_abc"; intervalo="$base..HEAD"
caso "padrão link-sessao reprova" 1 "(padrão link-sessao)" X=1

# --- A mensagem de erro diz commit e linha ---
novo_repo; commitar $'fix: texto\n\nlinha limpa\nusa claude-opus-5-5 aqui'; sha="$(git rev-parse --short=7 HEAD)"; intervalo="$base..HEAD"
caso "erro diz o commit e a linha" 1 "commit $sha, linha 4 (padrão id-de-modelo)" X=1

# --- Intervalo errado nunca passa calado ---
novo_repo; commitar "chore: x"; intervalo="..HEAD"
caso "base vazia reprova" 1 "base vazia" X=1

novo_repo; commitar "chore: x"; intervalo="0123456789abcdef0123456789abcdef01234567..HEAD"
caso "base que não existe reprova" 1 "rev-list falhou" X=1

novo_repo; commitar "chore: x"; intervalo="HEAD"
caso "intervalo sem .. reprova" 1 "sem \"..\"" X=1

novo_repo; commitar "chore: x"; intervalo="$base...HEAD"
caso "intervalo com três pontos reprova" 1 "três pontos" X=1

# --- Modo do CI: PR (merge ref) ---
pr_repo() { # $1 = mensagem do commit ruim na main, $2 = mensagem na ponta do PR
  novo_repo
  git checkout -q -b pr; commitar "feat: do PR"; [ -n "${2:-}" ] && commitar "$2"
  git checkout -q main; [ -n "${1:-}" ] && commitar "$1"; commitar "chore: main do dia"
  git checkout -q --detach main; git merge -q --no-ff pr -m "Merge pr into main"
}
intervalo=
pr_repo "" ""
caso "PR limpo passa e lê só os commits do PR" 0 "Intervalo lido: HEAD^1..HEAD^2" X=1

pr_repo "" "fix: usa claude-opus-5-5"
caso "PR com id de modelo reprova" 1 "(padrão id-de-modelo)" X=1

pr_repo "fix: antigo com claude-opus-5-5" ""
caso "commit proibido só na main (fora do PR) não reprova o PR" 0 "nenhum identificador de modelo" X=1

# --- Modo do CI: push na main ---
novo_repo; commitar "chore: um"; commitar "fix: usa claude-opus-5-5"; commitar "chore: três"; intervalo=
caso "push sem ANTES lê só o último commit" 0 "Intervalo lido: HEAD^..HEAD" ANTES=
caso "push com ANTES zerado lê só o último commit" 0 "Intervalo lido: HEAD^..HEAD" ANTES=0000000000000000000000000000000000000000
caso "push com ANTES lê tudo desde ANTES" 1 "(padrão id-de-modelo)" ANTES="$base"

novo_repo; intervalo=
caso "repositório de um commit só, sem como ler a base, reprova" 1 "rev-list falhou" X=1

exit "$([ "$falhas" -eq 0 ] && echo 0 || echo 1)"
