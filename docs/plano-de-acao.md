# Diário de Bordo — Plano de ação

26/09/2026 · Pedro · **Atualizado em:** 2026-09-26

Este documento diz **como** o app sai do papel: fases, entregas e o critério de pronto de cada uma. O **quê** e o **porquê** (escopo, monetização, custos, riscos de negócio) estão no [plano de produto](plano-de-produto.md). O método vem do repositório [framework-aprendido](https://github.com/pedrohrsm-crypto/framework-aprendido): procedimento PFA-01 (seção 06), gestão Kanban em escala S (seção 09), arquitetura para app mobile (seção 11) e as 20 lições (seção 01).

## Premissas

- **Plataforma:** Android nativo em Kotlin, minSdk 26 (Android 8) e targetSdk 36.
- **Offline-first:** o app funciona 100% sem internet. O Room no aparelho é a fonte da verdade; a nuvem só entra no premium, depois da saída do Exército.
- **GPS com dois usos (decidido em 26/09/2026):** (a) marcar o local de um registro e (b) gravar a rota de um rolê. Somar km automaticamente fica fora do MVP.
- **Mapa da rota no MVP (decidido em 26/09/2026):** só o traçado, com distância e duração, sem mapa de fundo, porque um mapa sem internet exige baixar os blocos antes. O mapa offline será reavaliado na fase 4.
- **Fonte de localização (decidido em 26/09/2026):** `FusedLocationProviderClient` do Google Play Services, que gasta menos bateria, com o `LocationManager` do Android como alternativa em aparelhos sem esses serviços.
- **Rotas no plano grátis (decidido em 26/09/2026):** sem limite de quantidade, porque tudo fica guardado no aparelho.
- **Um único módulo `:app`** com pacotes por funcionalidade (`garagem`, `diario`, `plano`, `rotas`, `numeros`), decidido em 26/09/2026. Separar em módulos Gradle só quando o tempo de build ou um limite de fronteira pedir.
- **Decisões da revisão pelas áreas (26/09/2026):**
  - O banco local não é criptografado no MVP, porque o Android já criptografa o armazenamento do app e o backup automático do banco fica desligado. A criptografia será reavaliada no premium.
  - A medição usa só a Play Console e o Crashlytics sem dados pessoais. Nenhum analytics de terceiros entra no plano grátis.
  - A importação de histórico de planilha fica para a fase 4 e é confirmada no teste fechado.
  - Os pontos brutos da rota são apagados quando a rota simplificada é salva.
  - A verba de divulgação no lançamento é zero, com divulgação direta em grupos e moto clubes.
- **Capacidade:** 8 a 10 h por semana, com o cronograma contado em meses a partir do início (M1, M2…).
- **Limite de trabalho em andamento:** no máximo 3 itens em execução e 2 em integração (escala S do framework).

## Visão geral

| Fase | Quando | Resultado | Critério de pronto (resumo) |
| --- | --- | --- | --- |
| 0. Fundação | M1, 2 semanas | Projeto compila, CI verde, tema e banco de pé | CI verde na `main` protegida e tema trocando em tempo real |
| 1. MVP | M1–M3 | Garagem, diário, plano com lembretes, GPS e modo oficina | Pedro usa na M800 por 4 semanas sem planilha paralela |
| 2. Teste fechado | M4 | 12+ testadores por 14 dias | Acesso à produção liberado e ≥ 99% de sessões sem falha |
| 3. Lançamento grátis | M4–M5 | App na Play Store | 500 instalações e 20% de retenção em 30 dias |
| 4. Evolução na ativa | M5 até a saída | Estatísticas, PDF e backup atrás de flag | Pedidos dos usuários priorizados e premium pronto, desligado |
| 5. Empresa | Saída + 1 a 2 meses | LTDA, D-U-N-S, conta de organização | App transferido e perfil de pagamentos ativo |
| 6. Premium | Após a fase 5 | Assinatura e vitalício | 2% a 3% dos usuários ativos pagando |

**Calendário sugerido (estimativa de 26/09/2026):** com a fundação começando em outubro de 2026, M1 é outubro. O MVP fica pronto em dezembro, janeiro reúne as 4 semanas de uso na M800 e o teste fechado, e o lançamento grátis fica para o início de fevereiro de 2027. Automatizar a criação de telas, entidades e frentes pode adiantar essas datas. Elas são revistas na reunião mensal pela vazão medida, não pela vontade.

As fases 0 a 3 estão detalhadas abaixo. As entregas 0.11, 0.12, 2.6 e 3.5 e vários critérios vieram da revisão pelas áreas (UX/UI, CyberOps, Desempenho, Marketing e DevOps) em `docs/PLANEJAMENTO.md`, seção 8. Os cinco conflitos da seção 6 desse documento foram decididos por Pedro em 26/09/2026 e estão nas premissas abaixo. As fases 4 a 6 dependem dos dados reais do lançamento e da data de saída, então ficam em nível de marco e são detalhadas na retro da fase 3.

---

## Fase 0 — Fundação (M1, 2 semanas)

Objetivo: deixar o projeto de pé com os portões automáticos rodando desde o primeiro commit (lição 4.1 do framework), antes de qualquer tela de produto.

| # | Entrega | Como provar |
| --- | --- | --- |
| 0.1 | Kit do framework instalado: `CLAUDE.md` preenchido para Android, templates de PR e de issue, `docs/retros/` | `CLAUDE.md` sem `<…>` restante e com a linha `Framework: vX.Y` |
| 0.2 | `docs/DIA_ZERO.md` percorrido, com os itens de web (Node, CSS, cookies) marcados como "não se aplica" e trocados pelos equivalentes Android | Todos os itens `[x]` ou com motivo escrito |
| 0.3 | Labels do GitHub (`frente`, `erro`, `licao-candidata`, `guarda`, `migration`, `risco`, `decisao`, `urgente`, `data-fixa`, `estrutural`, `bloqueado`) e quadro no GitHub Projects | Labels e quadro visíveis no repositório |
| 0.4 | `docs/GESTAO.md` em escala S: tese do produto, colunas com WIP, cadência semanal e mensal | Arquivo commitado, com data |
| 0.5 | Projeto Gradle (Kotlin DSL e version catalog), Compose + Material 3, Hilt, Room, WorkManager, minSdk 26 e targetSdk 36 | `./gradlew assembleDebug` passa na bancada e no CI |
| 0.6 | Os 3 temas do Figma (Oficina, Estrada e Cromo) como `ColorScheme`, e o modo oficina como escala de tipografia e espaçamento | Tema troca em tempo real; teste garante que cada token existe nos 3 temas; guarda que reprova cor crua fora do tema |
| 0.7 | Banco Room v1 (Moto, Registro, ItemDoPlano, Rota, PontoDeRota) com `exportSchema = true` e o esquema versionado no repositório | JSON do esquema v1 commitado e teste de abertura do banco passando; índices nas consultas principais e dados de exemplo para cada estado de usuário (novo, com uma moto, com histórico longo) |
| 0.8 | GitHub Actions com os portões: ktlint ou detekt, Android Lint, testes unitários, build do APK e do AAB | Link de um run verde; Dependabot, `concurrency` para cancelar runs antigos, cache do Gradle e guarda de idioma nos textos |
| 0.9 | Proteção de branch na `main`: PR obrigatório, checks obrigatórios, sem push direto | Tentativa de push direto recusada |
| 0.10 | Conta pessoal na Play Console (US$ 25) e verificação de identidade | Conta aprovada |
| 0.11 | Edge-to-edge com insets em toda tela e `imePadding()` nos formulários | Captura com o teclado aberto em 360 dp sem nenhum campo coberto |
| 0.12 | Verificação do manifesto do build de release | Um componente exportado plantado deixa o teste vermelho |

**Critério de pronto:** CI verde na `main` protegida, app abrindo num emulador API 26 e num API 36, tema trocando em tempo real e Pedro aprovando o `CLAUDE.md` preenchido.

**Portões do projeto** (valem a partir daqui, em todo PR):

1. Lint e formatação sem nenhum aviso novo (catraca: o número só pode cair).
2. Testes unitários do domínio, cada teste novo visto falhar antes de passar (lição 6).
3. Testes de migration do Room para toda mudança de esquema.
4. Build do AAB.
5. Portão visual: capturas nos 3 temas, no modo oficina, com fonte em 140% e em 200%, com o TalkBack ligado e com o teclado aberto, em tela de 360 dp.

---

## Fase 1 — MVP (M1 a M3)

Objetivo: o gratuito resolve sozinho o problema de lembrar e registrar a manutenção, sem internet. Cada entrega é uma frente com brief (template `frente.md`) e entra no quadro na ordem abaixo.

| # | Frente | Entregas | Critério de pronto |
| --- | --- | --- | --- |
| 1.1 | Garagem e hodômetro | Cadastro de 1 moto (grátis), atualização do hodômetro com histórico, validação de km que não pode diminuir | Hodômetro nunca aceita valor menor que o último, com teste; destinos de navegação numa lista única e um formatador único de número e unidade |
| 1.2 | Diário | Registros de manutenção, abastecimento, ocorrência, rolê e nota; até 3 fotos por registro, comprimidas, no armazenamento interno | Registro criado, editado e apagado em modo avião; fotos sobrevivem a reiniciar o app; EXIF de GPS removido das fotos; formatador único de número e unidade |
| 1.3 | Plano de manutenção | Planos-base por categoria (custom, street, trail, scooter) com intervalos por km e por tempo, ajustáveis pelo usuário; especificações marcadas como "dado do usuário" | Mudar um intervalo recalcula a próxima revisão, com teste do cálculo; formatador único de número e unidade |
| 1.4 | Lembretes | WorkManager para os lembretes por tempo; recálculo dos lembretes por km a cada atualização do hodômetro; notificações | Lembrete dispara com o app fechado e após reiniciar o aparelho; tocar na notificação abre a moto no item vencido; recálculo feito em lote |
| 1.5 | Ocorrência gera pendência | Uma ocorrência (queda, barulho, vazamento) cria itens de inspeção no plano | Pendência aparece no plano e some quando resolvida |
| 1.6 | GPS: local do registro | Botão "usar local atual" no registro, com permissão pedida só nesse momento; funciona sem internet (o GPS não precisa de rede) | Local gravado em modo avião; negar a permissão não quebra o registro |
| 1.7 | GPS: rota do rolê | Gravação iniciada pelo usuário num serviço em primeiro plano do tipo `location`, com notificação fixa; pontos gravados no Room durante a gravação; distância e duração calculadas no aparelho; rota exibida só como traçado, sem mapa de fundo | Rolê de 1 h gravado em modo avião, sem perder pontos se o app for para segundo plano; pontos brutos apagados depois de salvar a rota simplificada, com teste |
| 1.8 | Modo oficina | Escala de tipografia e espaçamento maior, alvos de toque grandes, uso com luva | Fluxo "registrar troca de óleo" feito em até 3 toques no modo oficina |
| 1.9 | Exportar e importar | Backup em arquivo (JSON com as fotos) escolhido pelo usuário pelo seletor do sistema | Exportar num aparelho e importar em outro recupera todos os dados; exportação avisa que o arquivo contém dados pessoais e oferece a opção de sair sem rotas |
| 1.10 | Flag de premium | Recursos premium atrás de uma flag local, desligada | Nenhum recurso premium visível com a flag desligada, com teste |

**Toda frente da fase 1** cobre os três estados de tela (carregando, vazio e com dados) e o caso de quem ainda não cadastrou nenhuma moto.

**Regras de GPS e permissões**

- Pedir `ACCESS_FINE_LOCATION` e `ACCESS_COARSE_LOCATION` só quando o usuário tocar em "usar local" ou "gravar rota", nunca na abertura do app.
- Não pedir `ACCESS_BACKGROUND_LOCATION`: a rota é gravada por um serviço em primeiro plano iniciado pelo usuário, o que evita a revisão extra da Play para localização em segundo plano.
- Declarar `FOREGROUND_SERVICE` e `FOREGROUND_SERVICE_LOCATION` (exigido a partir do Android 14) e pedir `POST_NOTIFICATIONS` a partir do Android 13.
- Intervalo de captura entre 2 e 5 segundos, com distância mínima entre pontos, para poupar bateria. Ao salvar, gravar só a rota simplificada e apagar os pontos brutos, para não inflar o banco nem expor o trajeto detalhado.
- O local é opcional e aparece assim na política de privacidade e no formulário de Segurança dos dados.

**Regras de dados offline**

- Toda escrita confere o resultado e nenhum erro é descartado em silêncio (lição 5 e item 3 do núcleo invariável da seção 11).
- Toda mudança de esquema tem migration testada a partir de todas as versões já publicadas, porque versões antigas do app ficam instaladas por meses (seção 11, linha "App mobile").
- Nenhuma tela depende de rede. O desenho da sincronização futura do premium fica em `docs/offline-first.md`.

**Critério de pronto da fase:** Pedro usa o app na M800 por 4 semanas seguidas sem planilha paralela, com pelo menos 1 rolê gravado e 1 lembrete disparado de verdade, e sem nenhum erro aberto de perda de dados.

---

## Fase 2 — Teste fechado (M4)

| # | Entrega | Critério de pronto |
| --- | --- | --- |
| 2.1 | Firebase Crashlytics ligado, sem dados pessoais nos relatórios | Falha plantada aparece no painel e depois é removida; Crashlytics desligado em builds de debug e no CI |
| 2.2 | Política de privacidade publicada (LGPD) e formulário de Segurança dos dados preenchido, citando fotos, hodômetro e localização opcional | Link da política na ficha e formulário aprovado |
| 2.3 | 15 a 20 testadores do moto clube inscritos na faixa de teste fechado | 12 ou mais testadores ativos por 14 dias seguidos |
| 2.4 | Correções dos achados, cada um como issue `erro` com causa raiz | Todo erro fechado com causa raiz; erro repetido ganha guarda (regra da segunda ocorrência) |
| 2.5 | Teste em aparelhos reais: pelo menos um Android 8 e um Android 14 ou mais recente | Fluxos de GPS e lembretes conferidos nos dois |
| 2.6 | Baseline de desempenho no aparelho Android 8 | Tabela com data, versão e aparelho em `docs/desempenho/` |

**Critério de pronto:** acesso à produção liberado pela Play e ≥ 99% de sessões sem falha no Crashlytics.

---

## Fase 3 — Lançamento grátis (M4 a M5)

| # | Entrega | Critério de pronto |
| --- | --- | --- |
| 3.1 | Ficha da loja: ícone, 6 a 8 capturas com os temas e o modo oficina, descrição com "manutenção moto", "revisão moto", "consumo moto" e "troca de óleo" | Ficha publicada; cada frase da ficha aponta para a tela que a comprova; a ficha responde "e se eu perder o celular?" |
| 3.2 | Divulgação em grupos de motociclistas | Registro de onde foi divulgado e quantas instalações vieram; verba zero |
| 3.3 | Métricas de produto medidas: instalações, ativação em 7 dias, retenção em 30 dias e registros por usuário ativo, só com a Play Console e o Crashlytics | Primeira leitura registrada com data |
| 3.4 | Retro completa (PFA-01, etapa P3), com as lições enviadas ao framework-aprendido | Arquivo em `docs/retros/` e PR aberto no framework |
| 3.5 | Tabela de concorrentes e frase de posicionamento | Tabela com link e data de cada leitura |

**Critério de pronto:** 500 instalações e 20% de retenção em 30 dias, três meses após o lançamento. Se a retenção ficar abaixo disso, a fase 4 começa por entender o motivo com usuários reais, antes de qualquer recurso premium.

---

## Fases 4 a 6 — Marcos

- **Fase 4, evolução na ativa:** planos por categoria para as motos mais vendidas, importação de histórico de planilha, estatísticas, relatório em PDF e backup na nuvem, tudo atrás da flag e desligado. Nenhuma receita.
- **Fase 5, empresa:** LTDA, contador, D-U-N-S, conta de organização na Play e transferência do app (ver o plano de produto).
- **Fase 6, premium:** Play Billing (assinatura mensal, anual e vitalício), backup no Supabase e desconto de fundador, com o catálogo de segurança do Supabase conferido e toda compra verificada no servidor.

---

## Gestão e cadência

- **Quadro:** GitHub Projects com as colunas da seção 09 do framework (Opções, Com evidência, Pronta, Execução, Integração, Validação, Concluída).
- **Semanal (30 min):** puxar a próxima frente, ver bloqueios, riscos e decisões pendentes.
- **Mensal (1 h):** conferir o critério de pronto da fase atual e decidir se avança.
- **Retro curta (15 min):** ao fim de cada fase, em `docs/retros/`.
- **Riscos e decisões:** viram issues `risco` e `decisao` no momento em que surgem. Decisão registrada não se reabre sem motivo novo.
- **Toda sessão termina** com push feito, PR aberta e uma linha de retomada (lição 20).
