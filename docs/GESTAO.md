# Gestão — <projeto>

> Kanban e PMBOK 8 adaptados (framework, seção 09). **Escala:** S | M | G
> **Atualizado em:** AAAA-MM-DD · **Dono:** <dono do produto>
>
> Este é o **único** documento de gestão mantido à mão. Riscos e decisões são **issues**
> (`risco`, `decisao`); o quadro é o GitHub Projects; as métricas saem das datas do GitHub.

## 1. Tese do produto (Governança)
- **Problema e para quem:**
- **Objetivo do próximo marco:**
- **Critério de sucesso (valor, não esforço):** <ex.: N clientes ativos, retenção de X% em 30 dias>
- **Limites:** tempo do dono <h/semana> · orçamento <R$/mês>
- **Fora do escopo:**

## 2. Definição de Fluxo (Kanban)
| Coluna | Entra quando | Limite de WIP |
|---|---|---|
| Opções | Ideia, pedido de campo ou achado registrado | — |
| Com evidência | Quem pediu, quantos, que problema resolve | 10 |
| Pronta para começar | Cumpre a Definição de Pronto | 5 |
| *— ponto de compromisso —* | | |
| Execução | Puxada por uma sessão ou frente | 3 |
| Integração | PR com portões verdes | 2 |
| Validação com cliente | Em produção, aguardando uso real | 20 |
| Concluída | Validada, ou dispensa com motivo | — |

**Definição de Pronto para começar:** evidência de valor · brief com fronteira · critério de aceite verificável · classe de serviço · riscos anotados.
**Definição de Concluído:** DoD do template de PR + validação com cliente (ou dispensa com motivo).
**Bloqueio:** label `bloqueado`; continua contando no WIP.

## 3. Classes de serviço
| Classe | Label | Política |
|---|---|---|
| Urgente | `urgente` | Fura o WIP; no máximo 1; post-mortem obrigatório |
| Data fixa | `data-fixa` | Começa na data-limite menos o tempo de ciclo p85 |
| Padrão | — | Ordem por custo do atraso |
| Estrutural | `estrutural` | Cerca de 20% da capacidade reservada |

## 4. Cadências
| Cadência | Quando | Duração | Pauta |
|---|---|---|---|
| Revisão do quadro | | | Itens envelhecendo, bloqueios |
| Semanal | | | Reabastecimento · métricas de fluxo · riscos · decisões pendentes |
| Mensal | | | Valor · orçamento · partes interessadas · go/no-go do próximo marco |
| Retro | Fim de cada marco | | PFA-01, etapa P3 |

## 5. Marcos e go/no-go (Cronograma + Governança)
| Marco | Data-alvo | Previsão por vazão | Critério de go/no-go |
|---|---|---|---|
| | | | |

## 6. Orçamento mensal (Finanças)
| Item | Previsto | Realizado | Limite ou cota | Alerta configurado? |
|---|---|---|---|---|
| | | | | |

## 7. Partes interessadas
| Quem | Interesse | Como é ouvido | Frequência |
|---|---|---|---|
| | | | |

## 8. Recursos e capacidade
- **Capacidade semanal:** dono <h> · sessões de IA <N em paralelo>
- **Latência de decisão do dono** (meta: ≤ 2 dias úteis):

## 9. Linha de base das métricas de fluxo
| Data | WIP | Vazão/semana | Idade máxima | Tempo de ciclo mediano / p85 |
|---|---|---|---|---|
| | | | | |
