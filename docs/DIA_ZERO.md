# Dia Zero — <projeto>

Procedimento PFA-01, passo 1.4. Marque `[x]` ou escreva o motivo ao lado. Os números entre parênteses apontam para o catálogo de erros (04) do framework.

**Framework:** vX.Y · **Data:** AAAA-MM-DD · **Aprovado pelo dono:** [ ]

## Repositório e ambiente
- [ ] `CLAUDE.md` preenchido, sem `<…>` restante (12.5, 9.1)
- [ ] Versão do Node declarada em um lugar e igual na bancada, no CI e em produção (4.4)
- [ ] `.env.example` completo e teste comparando com as variáveis lidas no código (10.5)
- [ ] Script de worktree copia o `.env` e usa `npm ci` (10.2, 10.3)
- [ ] Pastas de maquete fora da varredura do CSS (6.7)
- [ ] Labels criadas: frente, erro, licao-candidata, guarda, migration, risco, decisao, urgente, data-fixa, estrutural, bloqueado

## CI desde o primeiro commit (4.1)
- [ ] Proteção de branch na `main`: PR obrigatório, checks obrigatórios, sem push direto (4.12)
- [ ] Convenção de commit `tipo(frente): texto (#issue)` com lint de mensagem (12.7)
- [ ] Lint (saída vazia), typecheck, testes e e2e, todos rodando a suíte inteira (4.2)
- [ ] CI em UTC; nenhum portão com `| tail` (4.6, 4.7)
- [ ] `npm audit` e lockfile conferidos (10.1)
- [ ] Alerta de cobrança do provedor de CI (4.11)

## Esqueleto
- [ ] Camadas: apresentação → services/actions → domínio puro → infra, mais kernel
- [ ] Fronteiras verificadas (dependency-cruiser / eslint-boundaries) com catraca
- [ ] Guarda de acesso como módulo puro, com 2 regras e testes em matriz (2.2)
- [ ] Middleware copia os cookies em todo redirect (2.1)
- [ ] Logger JSON com redação de segredos; monitoramento de erros lazy e sem PII
- [ ] Tokens de tema com par em todos os temas, com teste (6.2)
- [ ] Portal padrão para todo `fixed` (6.5)
- [ ] e2e com viewport de 360 px e axe (7.2, 5.7)

## Gestão (seção 09)
- [ ] Escala do projeto definida (S, M ou G) e `docs/GESTAO.md` preenchido
- [ ] Tese do produto com critério de sucesso **de valor** (não de esforço)
- [ ] Quadro no GitHub Projects com as colunas e limites de WIP da Definição de Fluxo
- [ ] Cadências marcadas na agenda (semanal e mensal)
- [ ] Primeiras issues `risco` e `decisao` abertas
- [ ] Orçamento mensal com alertas de cota ou cobrança configurados (4.11)
- [ ] Linha de base das 4 métricas de fluxo medida na primeira semanal

## Fornecedores (11.1, 2.7)
| Fornecedor | Preço | Cotas/limites | Política | Formato de webhook/secret | Conferido em |
|---|---|---|---|---|---|
| | | | | | |
