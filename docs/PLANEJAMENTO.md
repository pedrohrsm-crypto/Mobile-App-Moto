# Planejamento com as áreas — Diário de Bordo (Mobile-App-Moto)

> Procedimento PFA-01, passo 1.0 (framework, capítulo 14).
> **Framework:** v1.6 · **Data:** 2026-09-26 · **Escala:** S · **Aprovado pelo dono:** [x] (Pedro, 2026-09-28)
>
> Todo item cita de onde veio. As fontes vêm de um SaaS web (o Velix) e foram adaptadas para um app Android nativo, offline-first e com GPS. O conteúdo delas foi tratado como material de consulta, e nenhum número do Velix virou meta do app. A seção 8 diz em que fase e item do [plano de ação](plano-de-acao.md) cada ponto entra.

**Fontes lidas em 26/09/2026:** `UX-UI` (`ef1eca7`), `CyberOps` (`30cf1d8`), `DevOps` (sem commits), `Desempenho` (`2459c3d`), `Marketing` (`aa30199`) e o capítulo 04 do `framework-aprendido`.

## 0. O projeto em uma página
- **Problema e para quem:** donos de moto esquecem revisões e perdem o histórico de manutenção, que hoje vive no adesivo da troca de óleo, num caderno, numa planilha ou na memória.
- **Tipo de produto** (capítulo 11, §5): mobile (Android nativo em Kotlin, minSdk 26, offline-first, com GPS).
- **Primeiro marco e data:** MVP em dezembro de 2026 e lançamento grátis no início de fevereiro de 2027 (plano de ação, calendário sugerido).
- **Fora do escopo:** nuvem, sincronização e cobrança antes da fase 6; mapa de fundo no MVP; soma automática de km pelo GPS.

## 1. UX/UI
- **Exige:**
  - Testar na pior condição: tela de 360 dp, fonte do sistema em 140% e em 200% (máximo do Android 14), os 3 temas, o modo oficina, teclado aberto e TalkBack (`UX-UI/01-principios.md` §3; `UX-UI/08-outros-universos.md` "O que se adapta").
  - Edge-to-edge com insets tratados em toda tela e `imePadding()` nos formulários, porque o targetSdk 35 ou mais obriga o edge-to-edge. É o equivalente Android da safe-area e do `vh` fixo (`UX-UI/03-catalogo-de-erros.md` §2, erros 2.1 e 2.2).
  - Cor só por token nomeado pelo papel, com par nos 3 temas; texto em `sp` e espaçamento em `dp` vindos da escala do tema (`UX-UI/01-principios.md` §11).
  - Até 5 destinos na barra inferior (`garagem`, `diario`, `plano`, `rotas`, `numeros`), declarados numa lista única lida pela barra e pelo grafo de navegação; rótulo da barra igual ao título da tela (`UX-UI/05-padroes-saas.md` N1 e N4; `01-principios.md` §10).
  - A moto é o hub: a notificação de lembrete abre a moto no item vencido do plano, e o "voltar" leva à moto (`UX-UI/01-principios.md` §9; `05-padroes-saas.md` N5).
  - Estado honesto: carregando, vazio e erro são três estados; um `Flow` do Room que falha nunca vira lista vazia; "nenhuma moto cadastrada" leva ao cadastro (`UX-UI/01-principios.md` §13; `05-padroes-saas.md` E1 e E2).
  - Número no formato brasileiro com unidade ("12.345 km", "32,5 km/l") por um formatador único, e enum sempre pelo rótulo em `strings.xml` (`UX-UI/03-catalogo-de-erros.md` 6.2 a 6.4).
  - Alvo de toque de 48 dp, maior no modo oficina (`UX-UI/08-outros-universos.md` "App mobile nativo").
- **Arrisca** (→ issue `risco`): erros que passam por lint, testes e build e só aparecem na pior condição, como aconteceu com quase todos os erros de UI do Velix (`UX-UI/04-licoes-aprendidas.md` "O que mudaríamos", item 4).
- **Decidir** (→ issue `decisao`): nada novo. As decisões de tema e modo oficina já estão registradas.
- **Mede:** fluxo "registrar troca de óleo" em até 3 toques no modo oficina (já no plano, item 1.8); zero campo coberto pelo teclado em 360 dp.
- **Portão** (→ `DIA_ZERO.md` ou CI):
  - Portão visual ampliado: capturas nos 3 temas, no modo oficina e com fonte em 140% e 200%, em 360 dp, com teclado aberto nas telas de formulário.
  - Guarda que conta `Color(0x…)` fora do pacote do tema, com teto que só desce (`UX-UI/06-guardas-automatizaveis.md`, contrato de toda guarda).
  - Dados de exemplo por estado de usuário ("sem moto", "moto nova", "anos de histórico", "hodômetro alto") usados nas capturas (`UX-UI/04-licoes-aprendidas.md` "O que mudaríamos", item 3).
- **Fontes lidas:** `UX-UI` 01, 03, 04, 05, 06, 07 e 08; `CyberOps/ux-ui/PADRAO_UX_UI.md` §1.

## 2. Segurança
- **Exige:**
  - Sem servidor no MVP, a superfície é o aparelho, o arquivo exportado e a localização. O catálogo de banco (RLS, grants, funções expostas) volta a valer na fase 6 (`CyberOps/seguranca/PRATICAS_SEGURANCA.md` §6).
  - Nada de dado pessoal em log: `Log` e `println` fora do release, e o Crashlytics nunca recebe coordenadas, placa ou texto livre (§2, "PII em log").
  - Consentimento por finalidade: local do registro e rota do rolê pedidos em momentos diferentes (já no plano) e descritos separados na política de privacidade (`UX-UI/03-catalogo-de-erros.md` 8.3).
  - Rotas mostram onde a pessoa mora: o arquivo exportado avisa que contém localização e oferece exportar sem rotas; as fotos têm o GPS do EXIF removido ao salvar.
  - Chave de upload da Play fora do git, nos segredos do GitHub, com Play App Signing ligado; release com R8 (§2, "Segredo"; §3, "separar o que a plataforma faz do que é seu").
  - Fase 6: RLS em toda tabela, views com `security_invoker`, nada executável pelo papel anônimo, compra da Play verificada no servidor com idempotência por token, MFA no admin e restauração do backup testada (§2 e §5).
- **Arrisca** (→ issue `risco`): exposição da localização de casa pelas rotas, no arquivo exportado ou no Crashlytics.
- **Decidido** (26/09/2026): banco local sem criptografia no MVP, reavaliar no premium (seção 6).
- **Mede:** placar PASS/FALHA da verificação do manifesto, com tudo PASS em todo release.
- **Portão** (→ `DIA_ZERO.md` ou CI):
  - Verificação reexecutável do manifesto final do release: nenhum componente exportado sem motivo, só as permissões esperadas, `usesCleartextTraffic` desligado, regras de backup iguais à decisão do offline-first, `debuggable` desligado. Vista falhar com um componente exportado plantado (§1, "a verificação reexecutável é o coração").
  - Dependabot e verificação de dependências no CI, reprovando falha crítica ou alta de produção (§2, "Dependência com CVE crítico").
- **Fontes lidas:** `CyberOps/README.md`; `CyberOps/seguranca/PRATICAS_SEGURANCA.md` §1 a §6; `CyberOps/seguranca/ROTINA.md`.

## 3. DevOps
- **Exige:**
  - Portões desde o primeiro commit e proteção de branch (framework 04, erros 4.1 e 4.12). Já estão no plano (0.8 e 0.9).
  - Cada tarefa do Gradle num passo próprio do workflow, sem `| tail` (04, erros 4.6 e 4.7).
  - Runner próprio compartilhado: `concurrency` com `cancel-in-progress` e cache do Gradle (decisão do Pedro de 26/09/2026 sobre o runner).
  - Publicação em etapas: faixa de teste interno a cada integração na `main`, teste fechado na fase 2 e liberação gradual de 10% a 20% no lançamento.
- **Arrisca** (→ issue `risco`):
  - Planejamos DevOps sem padrão da área: o repositório `DevOps` está vazio (capítulo 14, §2, "Área sem material").
  - Runner desligado deixa o PR parado. Com runner próprio, o risco muda de cobrança (04, erro 4.11) para disponibilidade; a regra é a do 4.12: congelar merges ou registrar no PR os portões rodados na bancada.
- **Decidir** (→ issue `decisao`): nada novo.
- **Mede:** tempo do CI por PR e quantos PRs esperaram o runner.
- **Portão** (→ `DIA_ZERO.md` ou CI): `concurrency` e cache do Gradle no workflow; nenhum portão com saída cortada.
- **Fontes lidas:** `DevOps` (vazio); framework 04 categorias 4 e 10; framework 13 (escopo do analista de DevOps).

## 4. Desempenho
- **Exige:**
  - Custo por unidade, não custo de hoje: bytes por hora de rolê, bateria por hora de gravação, tamanho médio de foto e tempo para abrir uma moto com 500 registros (`Desempenho/PADRAO_AGENTE_DESEMPENHO.md` §2, princípio 3).
  - Índice do Room em toda coluna de relação (`motoId`, `rotaId`) e de filtro ou ordenação (§4, padrão 6).
  - Diário paginado (Paging 3) em ordem decrescente, e rota simplificada ao salvar (§4, padrão 7).
  - Lembretes recalculados em lote, numa única tarefa do WorkManager por atualização de hodômetro (§4, padrão 12).
- **Arrisca** (→ issue `risco`): pontos brutos de rota crescendo sem limite (§4, padrão 13) e bateria alta na gravação, que vira nota ruim na loja.
- **Decidido** (26/09/2026): pontos brutos apagados ao salvar a rota simplificada (seção 6).
- **Mede:** baseline no aparelho Android 8 mais fraco disponível, antes do lançamento: abertura a frio, tamanho do AAB e do app instalado, quadros perdidos ao rolar o diário, bateria e bytes por hora de rolê. Depois, a mesma medição a cada versão, sem comentar quando nada muda (§3, fases 1 e 6; §7, linha "App mobile nativo").
- **Portão** (→ `DIA_ZERO.md` ou CI): teste de migração e de abertura do banco com os índices; tamanho do AAB com teto que só sobe por decisão.
- **Fontes lidas:** `Desempenho/PADRAO_AGENTE_DESEMPENHO.md` §1 a §7.

## 5. Marketing
- **Exige:**
  - Posicionar contra a alternativa real (adesivo da troca de óleo, caderno, planilha, memória), não contra outro app (`Marketing/FUNCIONOU.md` F-03).
  - Mapear os apps de manutenção e consumo mais baixados na Play Store brasileira, com preço, nota, link e data, marcando "não conferido" o que não foi aberto (`Marketing/PADRAO.md` fase 2; `LICOES.md` M-01 e M-02).
  - A ficha da loja só promete o que o app faz: nada de "backup na nuvem" no grátis, e cada frase cita a tela que a sustenta (`LICOES.md` M-08; `UX-UI.md` §1).
  - Resposta pronta para "e se eu perder o celular?": hoje, exportar o arquivo; no premium, backup automático; quem entra agora terá desconto de fundador (`FUNCIONOU.md` F-11).
  - Divulgação em grupos e moto clubes feita à mão, com permissão do administrador, sem mensagem automática (`LICOES.md` M-11).
  - Preço do premium comparado com o que o motociclista já gasta, como uma revisão ou uma troca de óleo perdida (`FUNCIONOU.md` F-04).
- **Arrisca** (→ issue `risco`): o gargalo ser ativação (instala e não cadastra a moto), e não alcance (`LICOES.md` M-06).
- **Decidido** (26/09/2026): verba zero no lançamento; importação de planilha na fase 4; métricas só por Play Console e Crashlytics (seção 6).
- **Mede:** ativação em 7 dias, definida como "moto cadastrada, hodômetro atual e um lembrete agendado", ao lado de instalações, retenção em 30 dias e registros por usuário ativo, que já estão no plano de produto. Os builds de debug e de CI não enviam nada a nenhuma métrica (`LICOES.md` M-04). O critério de sucesso de valor segue o do plano de produto: 20% de retenção em 30 dias.
- **Portão** (→ `DIA_ZERO.md` ou CI): guarda que procura formas europeias ("guardar", "registado", "ficheiro") em `strings.xml` (`LICOES.md` M-15).
- **Fontes lidas:** `Marketing/README.md`, `PADRAO.md`, `LICOES.md`, `FUNCIONOU.md` e `UX-UI.md`.

## 6. Conflitos entre áreas

Decididas pelo Pedro em 26/09/2026, que aceitou as 5 recomendações. Cada linha vira uma issue `decisao` já fechada, para registro.

| Área A quer | Área B exige | Decisão |
|---|---|---|
| Segurança: criptografar o banco local | Desempenho e exportação: banco rápido e arquivo legível | Não criptografar no MVP e reavaliar no premium, porque o Android já criptografa o armazenamento do app e o backup automático do banco está desligado |
| Marketing: medir ativação e origem das instalações | Segurança: nenhum rastreamento além do necessário, e a política de privacidade promete isso | Só Play Console e Crashlytics sem dado pessoal; nada de analytics de terceiros no grátis |
| Marketing: importar histórico de planilha para acelerar a ativação | Plano de ação: MVP já cheio | Deixar para a fase 4 e confirmar a necessidade no teste fechado |
| Produto: guardar a rota com detalhe | Desempenho e segurança: pontos brutos crescem e expõem trajetos | Apagar os pontos brutos ao salvar a rota simplificada |
| Marketing: definir verba de divulgação | Plano de produto: fase grátis sem receita | Verba zero no lançamento, com divulgação direta em grupos e moto clubes |

## 7. O que sai deste plano
- [ ] Riscos abertos como issues `risco` (passo 1.5): pior condição de UI, localização exposta, DevOps sem padrão, runner parado, rota crescendo, ativação
- [x] Decisões da seção 6 tomadas pelo Pedro em 26/09/2026 (registrar como issues `decisao` no passo 1.5)
- [ ] Portões copiados para o `DIA_ZERO.md` (passo 1.4)
- [ ] Critério de sucesso de valor no `GESTAO.md` §1

## 8. Onde entra no plano de ação

**Entregas novas**

| # | Fase | Entrega | Como provar | Área |
| --- | --- | --- | --- | --- |
| 0.11 | Fundação | Edge-to-edge com insets em toda tela e `imePadding()` nos formulários | Captura com teclado aberto em 360 dp sem campo coberto | UX/UI |
| 0.12 | Fundação | Verificação do manifesto do release | Componente exportado plantado deixa o teste vermelho | Segurança |
| 2.6 | Teste fechado | Baseline de desempenho no aparelho Android 8 | Tabela com data, versão e aparelho em `docs/desempenho/` | Desempenho |
| 3.5 | Lançamento | Tabela de concorrentes e frase de posicionamento | Tabela com link e data de cada leitura | Marketing |

**Itens existentes que ganham critério**

| Item | O que acrescenta |
| --- | --- |
| Portão visual (fase 0) | Fonte em 200%, TalkBack e teclado aberto |
| 0.6 | Guarda de cor crua fora do tema |
| 0.7 | Índices do Room e dados de exemplo por estado de usuário |
| 0.8 | Dependabot, `concurrency`, cache do Gradle e guarda de idioma |
| 1.1 a 1.3 | Destinos numa lista única; formatador único de número e unidade |
| 1.2 e 1.9 | EXIF de GPS removido; exportar com aviso e opção sem rotas |
| 1.4 | Notificação abre a moto no item vencido; recálculo em lote |
| Toda frente da fase 1 | Três estados e o caso "sem moto" |
| 2.1 | Crashlytics sem dado pessoal e desligado em debug e CI |
| 3.1 | Cada frase da ficha cita a tela; resposta para "e se eu perder o celular?" |
| 3.3 | Ativação em 7 dias como métrica |
| Fase 6 | Catálogo de segurança do Supabase e compra verificada no servidor |
