# Dia Zero — Diário de Bordo (Mobile App Moto)

Procedimento PFA-01, passo 1.4. Marque `[x]` ou escreva o motivo ao lado. Os números entre parênteses apontam para o catálogo de erros (04) do framework. Itens de web (Node, CSS, cookies, e2e no navegador) foram trocados pelo equivalente Android, com o item original citado.

**Framework:** v2.0 · **Data:** 2026-10-26 (revisto do Dia Zero de 28-Set) · **Aprovado pelo dono:** [ ]

## Repositório e ambiente
- [x] `CLAUDE.md` preenchido, sem `<…>` restante (12.5, 9.1)
- [x] Versões do JDK (21), do Gradle (wrapper) e das bibliotecas em um lugar só (`gradle/libs.versions.toml` e `gradle-wrapper.properties`), iguais na bancada e no CI. Substitui "versão do Node declarada" (4.4)
- [ ] ~~`.env.example` completo~~ Não se aplica no MVP: o app não lê variável de ambiente nem tem segredo. Volta na fase 6 (Supabase) e na assinatura do release (chave nos segredos do GitHub) (10.5)
- [ ] ~~Script de worktree com `.env` e `npm ci`~~ Não se aplica: o Gradle baixa tudo pelo wrapper e pelo catálogo; nada fica fora do git (10.2, 10.3)
- [ ] ~~Pastas de maquete fora da varredura do CSS~~ Não se aplica: não há CSS. Equivalente: `ferramentas/guardas.sh` só varre `app/src/main` (6.7)
- [ ] Labels criadas: frente, erro, licao-candidata, guarda, migration, risco, decisao, urgente, data-fixa, estrutural, bloqueado. **Pendente com o Pedro:** as sessões não têm permissão de criar labels (item 0.3 do plano de ação)

## CI desde o primeiro commit (4.1)
- [ ] Proteção de branch na `main`: PR obrigatório, checks obrigatórios, sem push direto (4.12). **Pendente com o Pedro** (item 0.9), depois do primeiro run verde
- [ ] Convenção de commit `tipo(frente): texto (#issue)` com lint de mensagem (12.7). A convenção está no `CLAUDE.md`; o lint de mensagem fica para depois do primeiro run verde, para não somar risco ao CI que ainda não rodou
- [x] Portões rodando a suíte inteira, cada um num passo: guardas, ktlint, testes do buildSrc, Android Lint, build, testes unitários, manifesto do release e AAB. Substitui "lint, typecheck, testes e e2e" (4.2); o e2e no navegador vira captura em emulador quando houver telas reais
- [x] CI em UTC (`TZ: UTC` no job); nenhum portão com `| tail` (4.6, 4.7)
- [x] Dependabot mensal para Gradle e Actions. Substitui `npm audit` e lockfile (10.1)
- [ ] Alerta de cobrança do provedor de CI (4.11): o CI roda no Actions hospedado, em repositório público. Marcar `[x]` só com o link do primeiro run verde (na abertura, 13 runs, 0 verdes, no repositório anterior)

## Esqueleto
- [x] Camadas: tela (Compose) → ViewModel → repositório → Room, com regra de negócio em funções puras em `<pacote>/dominio/` (`docs/offline-first.md`, "Arquitetura")
- [ ] Fronteiras verificadas com catraca. Não se aplica no módulo único: os pacotes por funcionalidade não têm ferramenta de fronteira no Gradle sem multimódulo. Reavaliar se o app ganhar módulos
- [ ] ~~Guarda de acesso como módulo puro~~ Não se aplica no MVP: não há login nem papéis (2.2). Volta com o premium
- [ ] ~~Middleware copia os cookies~~ Não se aplica: não há servidor (2.1)
- [ ] Logger com redação de dados pessoais; monitoramento de erros sem PII. Pendente: entra com o Crashlytics, sem coordenadas, placa nem texto livre (planejamento, seção Segurança)
- [ ] Tokens de tema com par em todos os temas, com teste (6.2). Parcial: os 3 `ColorScheme` existem e a guarda reprova cor fora do tema; as cores reais e o teste de tokens esperam o Figma (item 0.6)
- [x] Edge-to-edge com insets em toda tela e `imePadding()` nos formulários (`TelaDiario` e `FormularioDiario`). Substitui "portal padrão para todo `fixed`" (6.5)
- [ ] Captura em tela de 360 dp, fonte em 140% e TalkBack. Substitui "e2e com viewport de 360 px e axe" (7.2, 5.7). Pendente: depende de emulador no runner
- [x] Verificação do manifesto do release com componente exportado plantado nos testes (item 0.12)

## Gestão (seção 09)
- [x] Escala do projeto definida (S) e `docs/GESTAO.md` preenchido
- [x] Tese do produto com critério de sucesso **de valor**: 500 instalações e 20% de retenção em 30 dias (plano de produto, fase 3)
- [ ] Quadro no GitHub Projects com as colunas e limites de WIP da Definição de Fluxo. **Pendente com o Pedro** (item 0.3)
- [ ] Cadências marcadas na agenda (semanal e mensal). **Pendente com o Pedro**
- [ ] Primeiras issues `risco` e `decisao` abertas. Pendente: dependem das labels
- [x] Orçamento mensal com alertas: custo fixo zero até a fase 5 (`docs/GESTAO.md`, seção 6)
- [ ] Linha de base das 4 métricas de fluxo medida na primeira semanal

## Fornecedores (11.1, 2.7)
| Fornecedor | Preço | Cotas/limites | Política | Formato de webhook/secret | Conferido em |
|---|---|---|---|---|---|
| Google Play Console | US$ 25 uma vez | 12 testadores por 14 dias no teste fechado; targetSdk 36 | Política de dados e Segurança dos dados | Chave de upload nos segredos do GitHub | 2026-09-25 (plano de produto) |
| GitHub (repositório público) | Grátis | Actions hospedado sem custo em repositório público [não conferido com o bloqueio de cobrança da conta] | — | — | 2026-10-26 |
| Supabase (fase 6) | Grátis no início; US$ 25/mês depois | Plano grátis | — | Fase 6 | 2026-09-25 (plano de produto) |
