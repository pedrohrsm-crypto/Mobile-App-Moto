# Gestão — Diário de Bordo (Mobile App Moto)

> Kanban e PMBOK 8 adaptados (framework, seção 09). **Escala:** S
> **Atualizado em:** 2026-09-28 · **Dono:** Pedro
>
> Este é o **único** documento de gestão mantido à mão. Riscos e decisões são **issues**
> (`risco`, `decisao`); o quadro é o GitHub Projects; as métricas saem das datas do GitHub.
> O planejamento com as áreas fica em `docs/PLANEJAMENTO.md`, e as fases em `docs/plano-de-acao.md`.

## 1. Tese do produto (Governança)
- **Problema e para quem:** donos de moto esquecem revisões e perdem o histórico de manutenção, que vive no adesivo da troca de óleo, num caderno ou numa planilha.
- **Objetivo do próximo marco:** MVP usado pelo Pedro na própria moto por 4 semanas sem planilha paralela (plano de produto, fase 1).
- **Critério de sucesso (valor, não esforço):** 500 instalações e 20% de retenção em 30 dias, 3 meses após o lançamento grátis (plano de produto, fase 3).
- **Limites:** tempo do dono 8 a 10 h por semana · orçamento: US$ 25 da conta da Play e nenhum custo fixo até a fase 5.
- **Fora do escopo:** carros, recursos sociais, OBD, marketplace e qualquer cobrança enquanto na ativa.

## 2. Definição de Fluxo (Kanban)
Escala S: uma pessoa e poucas sessões de IA, então os limites são menores que os do modelo.

| Coluna | Entra quando | Limite de WIP |
|---|---|---|
| Opções | Ideia, pedido de campo ou achado registrado | — |
| Com evidência | Quem pediu, quantos, que problema resolve | 5 |
| Pronta para começar | Cumpre a Definição de Pronto | 3 |
| *— ponto de compromisso —* | | |
| Execução | Puxada por uma sessão | 2 |
| Integração | PR com portões verdes, esperando revisão do Pedro | 2 |
| Validação com cliente | No aparelho do Pedro ou no teste fechado, aguardando uso real | 10 |
| Concluída | Validada, ou dispensa com motivo | — |

**Definição de Pronto para começar:** evidência de valor · brief com fronteira · critério de aceite verificável · classe de serviço · riscos anotados.
**Definição de Concluído:** DoD do template de PR + validação com cliente (ou dispensa com motivo).
**Bloqueio:** label `bloqueado`; continua contando no WIP. PR parado esperando o runner `moto` conta como bloqueado.

## 3. Classes de serviço
| Classe | Label | Política |
|---|---|---|
| Urgente | `urgente` | Fura o WIP; no máximo 1; post-mortem obrigatório |
| Data fixa | `data-fixa` | Começa na data-limite menos o tempo de ciclo p85. Ex.: atualização anual do targetSdk em agosto |
| Padrão | — | Ordem por custo do atraso |
| Estrutural | `estrutural` | Cerca de 20% da capacidade reservada (portões, guardas, dependências) |

## 4. Cadências
| Cadência | Quando | Duração | Pauta |
|---|---|---|---|
| Revisão do quadro | Início de cada sessão de trabalho | 5 min | Itens envelhecendo, bloqueios, runner ligado? |
| Semanal | A combinar com o Pedro | 30 min | Reabastecimento · métricas de fluxo · riscos · decisões pendentes |
| Mensal | Primeira semana do mês | 1 h | Valor · orçamento · go/no-go do próximo marco |
| Retro | Fim de cada fase do plano de ação | 1 h | PFA-01, etapa P3 |

## 5. Marcos e go/no-go (Cronograma + Governança)
| Marco | Data-alvo | Previsão por vazão | Critério de go/no-go |
|---|---|---|---|
| Fase 0 — Fundação | Outubro/2026 | Medir na primeira semanal | CI verde na `main` protegida; app abre em API 26 e 36 |
| Fase 1 — MVP | Dezembro/2026 | — | Pedro usa por 4 semanas sem planilha |
| Fase 2 — Teste fechado | Janeiro/2027 | — | 12+ testadores por 14 dias; 99% de sessões sem falha |
| Fase 3 — Lançamento grátis | Fevereiro/2027 | — | Produção liberada pela Play |

## 6. Orçamento mensal (Finanças)
| Item | Previsto | Realizado | Limite ou cota | Alerta configurado? |
|---|---|---|---|---|
| Conta da Play Console | US$ 25 uma vez | — | — | Não se aplica |
| CI | R$ 0 (runner próprio) | — | Minutos hospedados bloqueados | Não se aplica |
| Domínio da política de privacidade | cerca de R$ 40/ano, fase 3 | — | — | — |

## 7. Partes interessadas
| Quem | Interesse | Como é ouvido | Frequência |
|---|---|---|---|
| Pedro | Dono do produto e primeiro usuário | Threads do projeto | Contínuo |
| Moto clube (15 a 20 testadores) | App que resolve a manutenção deles | Teste fechado e grupo | Fase 2 em diante |
| Assessoria jurídica | Publicação gratuita sem receita na ativa | Consulta antes de publicar | Uma vez, antes da fase 3 |

## 8. Recursos e capacidade
- **Capacidade semanal:** dono 8 a 10 h · sessões de IA: até 3 em paralelo (uma por frente)
- **Latência de decisão do dono** (meta: ≤ 2 dias úteis): medir a partir da primeira semanal

## 9. Linha de base das métricas de fluxo
| Data | WIP | Vazão/semana | Idade máxima | Tempo de ciclo mediano / p85 |
|---|---|---|---|---|
| | | | | |
