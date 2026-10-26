# Cronograma — Diário de Bordo (moto)

> Framework, capítulo 16. Aprovado pelo dono em 26-Out-2026 (decisão D-4 da abertura).
> **Porte:** médio (semanas) · **Marco zero:** 2026-10-26 (decisão do dono de abrir o MVP de portfólio) · **Framework:** v2.0

## 0. Por que médio, e o que mudou desde o plano de 26-Set
- O primeiro marco é o **MVP de portfólio** (critério da issue #32: AAB de release pelo CI com os portões verdes; Fases 0 e 1 concluídas com o dono usando 4 semanas sem planilha; README com capturas nos 3 temas; estudo de caso com previsto × realizado). Até ele, são cerca de 8 semanas: porte médio (16 §1).
- O plano antigo (MVP em dez-2026, lançamento em fev-2027) partia de **8 a 10 h por semana do dono construindo** (`docs/plano-de-acao.md`, premissas). Agora quem constrói são as frentes de IA, e o dono só aceita.
- **Medido:** as frentes de IA escreveram a Fase 0 e a Fase 1 inteiras em 3 dias de calendário (commits de 26 a 28-Set-2026 no repositório anterior, privado; 159 arquivos `.kt`). Escrever código não é mais o gargalo. **Nada disso passou num CI** (13 runs, 0 verdes).
- Os gargalos reais, em ordem: (1) CI que funcione; (2) revisão e integração em fatias (WIP 2 em integração); (3) o tempo de aceite do dono, que tem a estreia do Velix em 4-Nov; (4) as **4 semanas de uso real**, que são calendário, não esforço; (5) na Fase 2, os 14 dias seguidos com 12 testadores, exigência da Play.

## 1. Datas fixas (viram itens `data-fixa` no quadro)
| Data | O quê | Por que não muda |
|---|---|---|
| 2026-11-04 | Estreia do Velix | Atenção do dono no Velix entre 2 e 6-Nov: nenhum aceite do Moto pedido nesses dias |
| 2026-12-24 a 2027-01-01 | Festas | Sem integração; o uso na moto continua contando |
| Agosto de cada ano | Atualização anual do targetSdk exigida pela Play | Política do Google (`docs/plano-de-produto.md`, Publicação) |

## 2. As etapas do porte, com datas reais
| Etapa (cap. 16 §3) | Relativo | Data prevista | Data real | Desvio e motivo |
|---|---|---|---|---|
| Planejamento: rever o plano para as 9 áreas da v2.0 (faltam Suporte, Back-end e dados, Pagamentos, Privacidade) e este cronograma | Sem 0 | 26-Out a 1-Nov (S44) | | |
| Fornecedores conferidos (Play Console, Crashlytics, GitHub Actions público) | Sem 0 a 1 | até 8-Nov | | |
| Repositório público com histórico limpo (decisão do dono) | Sem 0 a 1 | até 8-Nov | | |
| Dia 0: CLAUDE.md v2.0, labels, quadro, issues `risco`/`decisao`, **primeiro run verde** | Sem 1 | 2 a 8-Nov (S45) | | |
| Fundação (Fase 0): esqueleto revisado, corrigido e integrado; tema troca em tempo real com prova; proteção da `main` | Sem 1 a 2 | até 15-Nov (S46) | | |
| Frentes da Fase 1 em fluxo contínuo (itens 1.1 a 1.10, em fatias, a partir do código já escrito) | Sem 2 a 4 | 9 a 29-Nov (S46 a S48) | | |
| MVP no aparelho do dono (APK de release do CI) | Sem 5 | 30-Nov (S49) | | |
| Uso real: 4 semanas sem planilha paralela; achados viram issues | Sem 5 a 8 | 30-Nov a 27-Dez | | |
| Retro curta de onda | a cada 2 sem | 16-Nov, 30-Nov, 14-Dez | | |
| Endurecimento: barra completa (manifesto, 3 temas × 140%/200% × 360 dp × TalkBack, exportar/importar entre aparelhos, EXIF, logs sem PII) | Sem 8 | 21 a 27-Dez | | |
| **Marco: MVP de portfólio** (README com capturas, estudo de caso com previsto × realizado) | Sem 9 | **3-Jan-2027** | | |
| Fase 2 · Teste fechado na Play (12+ testadores × 14 dias) | depois do marco | jan-2027 | | Depende da conta da Play (US$ 25) |
| Fase 3 · Lançamento grátis | depois do marco | fim de jan a fev-2027 | | Mantém a janela do plano antigo; o ganho das frentes de IA aparece antes do marco, não no teste fechado |

## 3. Premissas do calendário
- Tempo de resposta do dono: 1 janela de aceite por semana (segunda, na reunião), com no máximo 2 PRs do Moto em integração; aceite extra só se o dono oferecer.
- Horas do dono: só aceite e uso na moto; nenhuma hora de construção.
- Cota de IA: conjunta com a `loja-online`, dividida pelo Maestro (18 §11) [ainda não informada].
- CI: GitHub Actions hospedado depois de o repositório ficar público [não conferido se o bloqueio de cobrança da conta também barra repositório público; o run 1 caiu em 3 s sem passo nenhum no `ubuntu-latest`], ou o runner `moto` ligado na bancada.
- Fora deste cronograma: nuvem, sincronização, premium e cobrança (fases 4 a 6); carros; OBD.

## 4. Na retro
- [ ] Coluna "Data real" preenchida
- [ ] Desvios de mais de 20% explicados e levados para `dados/estimativas.csv` do framework
