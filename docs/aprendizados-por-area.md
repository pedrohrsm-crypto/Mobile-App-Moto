# Diário de Bordo — Aprendizados por área

26/09/2026 · Pedro · **Atualizado em:** 2026-09-26

Este documento reúne o que os repositórios de cada área de produção ensinam e que vale para o app, adaptado de um SaaS web (o Velix, de onde tudo foi destilado) para um app Android nativo, offline-first e com GPS. Ele complementa o [plano de ação](plano-de-acao.md): a última seção diz em que fase e em que item cada aprendizado entra.

**Fontes lidas em 26/09/2026:**

| Área | Repositório | Commit lido | O que tem |
| --- | --- | --- | --- |
| UX/UI | [UX-UI](https://github.com/pedrohrsm-crypto/UX-UI) | `ef1eca7` | 16 princípios, método, catálogo de erros, guardas, checklists e o capítulo 08 (outros universos) |
| Segurança e UX/UI | [CyberOps](https://github.com/pedrohrsm-crypto/CyberOps) | `30cf1d8` | Práticas de segurança (catálogo de vulnerabilidades, verificação reexecutável, rotina) e um segundo padrão de UX/UI |
| DevOps | [DevOps](https://github.com/pedrohrsm-crypto/DevOps) | sem commits | Repositório vazio. Os aprendizados de CI e entrega vieram do capítulo 04 do [framework-aprendido](https://github.com/pedrohrsm-crypto/framework-aprendido) (erros 4.x) e do que já aconteceu neste projeto |
| Desempenho | [Desempenho](https://github.com/pedrohrsm-crypto/Desempenho) | `2459c3d` | Padrão do analista: método em 7 fases, 14 padrões de problema e a tabela de adaptação para app mobile |
| Marketing | [Marketing](https://github.com/pedrohrsm-crypto/Marketing) | `aa30199` | Padrão em 9 fases, lições M-01 a M-15, o que funcionou F-01 a F-11 e regras para interface |

O conteúdo desses repositórios foi tratado como material de consulta. Nenhum número do Velix foi trazido para cá como meta do app.

---

## O que vale em todas as áreas

Cinco regras aparecem nos cinco repositórios e já batem com o framework. Elas não viram item novo no plano, mas valem para toda frente:

1. **Medido, não suposto.** Todo número vem de um comando rodado, com data e commit. O que não deu para medir é escrito como "não medido: motivo".
2. **Perguntar antes de detalhar**, com uma recomendação em cada pergunta, e registrar a resposta como decisão.
3. **Na segunda ocorrência, consertar o molde**, com uma guarda vista falhar antes de passar.
4. **A tela (e a loja) só promete o que o app faz hoje.**
5. **O analista mede e registra; quem corrige é a frente.** Nenhum analista mexe no código do produto.

---

## UX/UI

**Resumo:** a maior parte dos erros do Velix passou por lint, tipos, testes e build e só apareceu na pior condição de uso. O aprendizado central é testar na pior condição desde a primeira tela, com cor e tamanho vindos de tokens, e nunca deixar a interface mentir.

| Aprendizado | Adaptação para o app | Onde entra |
| --- | --- | --- |
| A pior condição é a de teste (360 px, fonte 140%, todos os temas, teclado aberto) | Tela de 360 dp, fonte do sistema em 140% e também em 200% (máximo do Android 14), os 3 temas e o modo oficina, teclado aberto e TalkBack ligado | Portão visual da fase 0, ampliado |
| Safe-area e `vh` fixo quebraram layouts | O equivalente no Android são os insets. Com targetSdk 35 ou mais o app é edge-to-edge obrigatório, então toda tela usa os insets do `Scaffold` e `imePadding()` nos formulários | Nova entrega 0.11 |
| Toda cor vem de token nomeado pelo papel, com par em todos os temas; tamanho em `rem` | Cor só pelo `MaterialTheme.colorScheme` ou por tokens do app nomeados pela função (atenção, sucesso); texto em `sp` e espaçamento em `dp`, vindos da escala do tema. Guarda que conta `Color(0x…)` fora do pacote do tema, com teto que só desce | 0.6 e 0.8 |
| Até 5 destinos principais, definidos num arquivo só | Os 5 pacotes (`garagem`, `diario`, `plano`, `rotas`, `numeros`) cabem na barra inferior. Os destinos ficam numa lista única (sealed class) lida pela barra e pelo grafo de navegação; o rótulo da barra é o título da tela | 1.1 a 1.3 |
| A entidade central é um hub, e o alerta abre a entidade na aba do problema | A moto é o hub. A notificação de lembrete abre a moto já no item do plano vencido, e o "voltar" leva para a moto, não para a lista | 1.4 |
| Estado honesto: carregando, vazio e erro são diferentes; erro nunca vira zero | Um `Flow` do Room que falha não pode virar lista vazia. Cada tela tem os três estados, e o caso "nenhuma moto cadastrada" leva ao cadastro, nunca a uma tela em branco | Critério de pronto de toda frente da fase 1 |
| Linguagem do usuário, número no formato local, enum nunca cru | Km com separador brasileiro ("12.345 km"), consumo com vírgula ("32,5 km/l"), unidade sempre visível. Formatação por um único formatador com `Locale("pt","BR")`, e cada enum com seu rótulo em `strings.xml` | 1.1 e 1.2 |
| Alvo de toque mínimo | 48 dp no modo normal e maior no modo oficina | 1.8 |
| Contas de teste para cada estado de usuário | Dados de exemplo para "sem moto", "moto nova sem registros", "moto com anos de histórico" e "hodômetro alto", usados nos testes de captura | 0.7 e fase 1 |

**Não se aplica:** as guardas de Tailwind e flexbox, o `viewport-fit` do PWA e o menu por papel de usuário (o app tem um perfil só).

---

## Segurança

**Resumo:** a tese do CyberOps é que o controle de acesso tem mais de uma camada, e o atacante procura a que ninguém auditou. No MVP o app não tem servidor, então a superfície é o aparelho, o arquivo exportado e a localização. O catálogo de banco (RLS, grants, funções expostas) volta a valer quase inteiro na fase 6, quando o Supabase entrar.

| Aprendizado | Adaptação para o app | Onde entra |
| --- | --- | --- |
| Verificação reexecutável que devolve PASS ou FALHA por controle | Um teste que lê o `AndroidManifest` final do build de release e confere: nenhum componente exportado sem motivo, só as permissões esperadas, `usesCleartextTraffic` desligado, regras de backup iguais à decisão do offline-first, `debuggable` desligado | Nova entrega 0.12 |
| Dependência com falha crítica no portão | Dependabot ligado e um passo de verificação de dependências no CI que reprova em falha crítica ou alta de produção | 0.8 |
| Nada de dado pessoal em log | `Log` e `println` fora do build de release (guarda por lint). O Crashlytics nunca recebe coordenadas, placa ou texto livre dos registros | 2.1 |
| Consentimento por finalidade | Localização tem dois usos com duas permissões pedidas em momentos diferentes (já no plano). A política de privacidade descreve cada finalidade separada | 1.6, 1.7 e 2.2 |
| Dado sensível sai do aparelho sem o usuário perceber | Rotas mostram onde a pessoa mora. O arquivo exportado avisa que contém localização e oferece exportar sem rotas. Fotos têm os metadados de GPS (EXIF) removidos ao salvar | 1.2 e 1.9 |
| Segredo fora do repositório | Chave de upload da Play fora do git, nos segredos do GitHub, e Play App Signing ligado. Nenhuma chave de API no código do grátis | 0.8 e 0.10 |
| Plataforma não protege sozinha: conferir a configuração | Release com R8 ligado; conferir que o Crashlytics só roda no release | 0.5 e 2.1 |
| RLS não é o fim da autorização; webhook com assinatura e idempotência; MFA no admin; backup com teste de restauração | Na fase 6: RLS em toda tabela, `security_invoker` nas views, nada executável pelo papel anônimo, verificação da compra da Play no servidor (nunca só no app) com idempotência por token, e restauração do backup testada | Fase 6 |

**Não se aplica ao MVP:** CSRF, cabeçalhos HTTP, rate limit de rota pública e bucket de storage. Voltam a valer no premium.

---

## DevOps

**Resumo:** o repositório DevOps está vazio. O que vale para o app vem dos erros de CI do framework e do que já aconteceu aqui: o CI chegou a ficar sem rodar por cobrança e agora depende de um runner próprio compartilhado.

| Aprendizado | Adaptação para o app | Onde entra |
| --- | --- | --- |
| Portões desde o primeiro commit e proteção de branch (erros 4.1 e 4.12) | Já estão no plano | 0.8 e 0.9 |
| Alerta de cobrança do provedor de CI (4.11) | Com runner próprio, o risco muda de cobrança para disponibilidade: runner desligado deixa o PR parado. Registrar como risco, com a regra do erro 4.12 (congelar merges ou registrar os portões rodados na bancada) | Issue `risco` na fase 0 |
| Runner compartilhado com outro projeto | `concurrency` com `cancel-in-progress` para não empilhar runs antigos, e cache do Gradle para encurtar cada run | 0.8 |
| Nenhum portão com saída cortada (4.6, 4.7) | Cada tarefa do Gradle roda num passo próprio do workflow, sem `| tail` | 0.8 |
| Entrega em etapas | Faixa de teste interno a cada integração na `main`, teste fechado na fase 2 e lançamento com liberação gradual (começando em 10% a 20%) na fase 3 | 2.3 e 3.1 |

**Pendência:** o capítulo 13 do framework prevê um analista de DevOps que ainda não existe. Quando ele existir, este quadro é revisto.

---

## Desempenho

**Resumo:** com poucos dados tudo parece rápido. O que importa é o custo por unidade (por registro, por minuto de rolê, por foto) e ter uma medição de base antes do lançamento para comparar depois. O próprio padrão já traz a troca para app mobile: tempo de abertura a frio, tamanho do app, quadros perdidos e SQLite no lugar do banco do servidor.

| Aprendizado | Adaptação para o app | Onde entra |
| --- | --- | --- |
| Baseline medida antes de otimizar | Medir no aparelho Android 8 mais fraco disponível: abertura a frio, tamanho do AAB e do app instalado, quadros perdidos ao rolar o diário | Nova entrega 2.6 |
| Custo por unidade | Bytes gravados por hora de rolê, bateria gasta por hora de gravação, tamanho médio de foto comprimida e tempo para abrir uma moto com 500 registros | 1.7, 1.2 e 2.6 |
| Chave estrangeira sem índice | Índice do Room em toda coluna de relação (`motoId`, `rotaId`) e em toda coluna de filtro ou ordenação | 0.7 |
| Lista sem limite | Diário paginado (Paging 3) em ordem decrescente; a rota é simplificada ao salvar (já no plano) | 1.2 e 1.7 |
| Nenhuma retenção | Decidir se os pontos brutos da rota são apagados depois da simplificação, ou mantidos por um prazo | Decisão pendente abaixo |
| Tarefa agendada item a item | Lembretes recalculados em lote, numa única tarefa do WorkManager por atualização de hodômetro | 1.4 |
| Rotina que só mede e não comenta quando nada muda | Depois do lançamento, a mesma medição da 2.6 a cada versão publicada, com Baseline Profile se a abertura a frio passar da meta | Fase 4 |

---

## Marketing

**Resumo:** antes de pensar em canal, achar o gargalo. Em app gratuito ele costuma ser ativação: a pessoa instala e não chega ao primeiro valor. O app deve ser posicionado contra a alternativa real do motociclista, e nada na loja pode prometer o que o app ainda não faz.

| Aprendizado | Adaptação para o app | Onde entra |
| --- | --- | --- |
| Perguntar primeiro meta, verba, restrições e formato | Meta e prazo já estão no plano de produto. Falta registrar a verba de divulgação (provavelmente zero, o que exclui anúncio) | Decisão pendente abaixo |
| Mapear a concorrência local e a alternativa real, com fonte e data | Levantar os apps de manutenção e consumo mais baixados na Play Store brasileira, com preço e nota, marcando "não conferido" o que não foi aberto. A alternativa real é o adesivo da troca de óleo, o caderno, a planilha ou a memória | Nova entrega 3.5 |
| O gargalo costuma ser ativação | Definir o primeiro valor como "moto cadastrada, hodômetro atual e um lembrete agendado" e medir quantos chegam lá em 7 dias | 3.3 |
| Medir sem rastreamento invasivo e sem contar os próprios testes | Instalações e retenção vêm do Play Console. Os builds de debug e de CI não enviam nada ao Crashlytics nem a qualquer métrica | 2.1 e 3.3 |
| A loja só promete o que o app faz | Nada de "backup na nuvem" ou "sincroniza entre aparelhos" na ficha do grátis. Cada frase da descrição cita a tela que a sustenta | 3.1 |
| Não automatizar mensagem em rede social | Divulgação em grupos de WhatsApp e moto clubes feita à mão, com permissão do administrador | 3.2 |
| Ponte para a lacuna que o público valoriza | Importar histórico de uma planilha (CSV) ajuda quem já anota e acelera a ativação | Decisão pendente abaixo |
| Resposta pronta para a objeção principal | "E se eu perder o celular?": hoje, exportar o arquivo e guardar onde quiser; no premium, backup automático; quem entra agora terá desconto de fundador | 3.1 e fase 6 |
| Preço como fração do que o cliente já gasta | Comparar o anual do premium (R$ 49,90) com o custo de uma revisão ou de uma troca de óleo perdida | Fase 6 |
| Idioma na variante certa | Guarda que procura formas europeias ("guardar", "registado", "ficheiro") em `strings.xml` | 0.8 |

---

## Onde entra no plano de ação

As mudanças abaixo são propostas para o [plano de ação](plano-de-acao.md). Nenhuma muda uma decisão já registrada.

**Entregas novas**

| # | Fase | Entrega | Como provar | Área |
| --- | --- | --- | --- | --- |
| 0.11 | Fundação | Edge-to-edge com insets tratados em toda tela e `imePadding()` nos formulários | Captura com teclado aberto em 360 dp sem campo coberto | UX/UI |
| 0.12 | Fundação | Teste de segurança do manifesto do release (exportados, permissões, cleartext, backup, debuggable) | Teste plantado com um componente exportado fica vermelho | Segurança |
| 2.6 | Teste fechado | Baseline de desempenho no aparelho Android 8: abertura a frio, tamanho, quadros perdidos, bateria e bytes por hora de rolê | Tabela com data, versão e aparelho em `docs/desempenho/` | Desempenho |
| 3.5 | Lançamento | Tabela de concorrentes na Play Store e frase de posicionamento contra a alternativa real | Tabela com link e data de cada leitura | Marketing |

**Itens existentes que ganham critério**

| Item | O que acrescenta |
| --- | --- |
| Portão visual (fase 0) | Fonte em 200% além de 140%, TalkBack e teclado aberto |
| 0.6 | Guarda de cor crua fora do tema, com teto |
| 0.7 | Índices do Room nas colunas de relação e filtro; dados de exemplo por estado de usuário |
| 0.8 | Dependabot e verificação de dependências; `concurrency` com cancelamento; cache do Gradle; guarda de idioma em `strings.xml` |
| 1.1 a 1.3 | Destinos da barra numa lista única; formatador único de número e unidade |
| 1.2 e 1.9 | EXIF de GPS removido das fotos; exportar com aviso de localização e opção sem rotas |
| 1.4 | Notificação abre a moto no item vencido; recálculo em lote |
| Toda frente da fase 1 | Três estados (carregando, vazio, erro) e o caso "sem moto" |
| 2.1 | Crashlytics sem coordenadas nem texto livre, e desligado em debug e CI |
| 3.1 | Cada frase da ficha cita a tela que a sustenta; resposta pronta para "e se eu perder o celular?" |
| 3.3 | Ativação em 7 dias como métrica, ao lado de instalações e retenção |
| Fase 6 | Catálogo de segurança do Supabase e verificação da compra no servidor |

**Decisões pendentes (cada uma com a minha recomendação)**

1. **Verba de divulgação no lançamento.** Recomendo zero, com divulgação direta em grupos e moto clubes, porque a meta de 500 instalações cabe nesse canal e anúncio sem ativação medida só enche o topo do funil.
2. **Importar histórico de planilha (CSV).** Recomendo deixar para a fase 4, porque o MVP já está cheio e a necessidade pode ser confirmada no teste fechado.
3. **Pontos brutos da rota depois da simplificação.** Recomendo apagar ao salvar, porque a rota simplificada basta para o traçado, a distância e a duração, e o bruto só ocupa espaço e aumenta o risco de exposição.
4. **Criptografar o banco local.** Recomendo não criptografar no MVP e reavaliar no premium, porque o Android já criptografa o armazenamento do app e o backup automático está desligado para o banco. A criptografia no app custaria desempenho e complicaria a exportação.
