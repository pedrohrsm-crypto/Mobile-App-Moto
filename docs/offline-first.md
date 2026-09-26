# Diário de Bordo — Offline-first

26/09/2026 · Pedro

## Resumo

É viável, e sem custo de servidor: o app inteiro funciona sem internet porque tudo é gravado primeiro no aparelho. A rede só serve para três coisas opcionais, que nunca bloqueiam o uso: backup na nuvem (premium), nome do lugar a partir das coordenadas e mapa de fundo da rota.

- **Fonte da verdade:** o banco Room no aparelho. A tela só lê do Room; nada espera resposta da rede.
- **GPS:** funciona sem internet, porque o sinal vem dos satélites. Sem rede, a primeira posição só demora mais (sem A-GPS).
- **Sincronização:** só no premium, feita pelo WorkManager quando houver conexão, atrás de uma flag desde o início.
- **O que muda no esquema desde o dia 1:** ids gerados no aparelho, data de alteração e exclusão lógica em toda tabela. Assim o backup premium entra depois sem migração de dados.

Este documento detalha o [plano de produto](plano-de-produto.md) na parte de dados, GPS e sincronização.

## Requisitos

| Requisito | Origem | Como o plano atende |
| --- | --- | --- |
| Funcionar sem internet | Objetivo do projeto | Room como fonte da verdade; nenhuma tela depende de rede |
| Android 8+ | Objetivo do projeto | minSdk 26; serviço em primeiro plano para a rota, por causa dos limites de execução em segundo plano do Android 8 |
| GPS marca o local do registro | Decisão de 26/09/2026 | Uma leitura de posição ao salvar o registro, opcional |
| GPS grava a rota do rolê | Decisão de 26/09/2026 | Serviço em primeiro plano gravando pontos direto no Room |
| Um único módulo `:app` | Decisão de 26/09/2026 | Pacotes por funcionalidade (`diario`, `plano`, `numeros`, `garagem`, `rota`), e `core/data` para banco e sync. Substitui "um módulo por funcionalidade" do plano de produto |
| Lembretes por km e por tempo | Plano de produto | WorkManager local, sem servidor |
| Backup no grátis | Plano de produto | Exportar e importar arquivo |
| Backup automático no premium | Plano de produto | Supabase, sincronizado pelo WorkManager |

## Arquitetura

O fluxo é sempre o mesmo: a tela chama o ViewModel, o ViewModel chama o repositório, o repositório grava no Room, e o Room avisa a tela por `Flow`. A rede aparece só em workers, fora do caminho da tela.

```
Tela (Compose) ──► ViewModel ──► Repositório ──► Room ──(Flow)──► Tela
                                      │
                                      └─ marca "pendente de envio" (premium)
                                                     │
                              WorkManager (com rede) ─┴─► Supabase
```

- **Escrita:** sempre local e imediata. Uma alteração nunca falha por falta de sinal.
- **Leitura:** a tela observa o Room por `Flow`. Quando o sync traz dados de outro aparelho, a tela atualiza sozinha.
- **Estado da rede:** o `ConnectivityManager` serve só para mostrar um indicador discreto ("backup pendente"). Nenhum botão fica desabilitado por falta de rede.
- **Uma regra, um lugar:** cálculo de km até a próxima troca, consumo e distância da rota ficam em funções de domínio puras, testadas sem Android. Lição do framework-aprendido (erro 8.2: a mesma regra em 4 lugares).

## Modelo de dados

Toda tabela sincronizável carrega os mesmos campos de controle. Eles custam pouco no grátis e evitam uma migração de dados arriscada quando o premium chegar.

| Campo | Tipo | Por quê |
| --- | --- | --- |
| `id` | UUID gerado no aparelho (texto) | Dois aparelhos criam registros sem conflito de id, sem consultar o servidor |
| `criadoEm`, `alteradoEm` | Instante em UTC (milissegundos) | Ordem das alterações e resolução de conflito |
| `fusoHorario` | Id do fuso (ex.: `America/Sao_Paulo`) | Mostrar a data certa do registro em qualquer lugar. Lição 8.3 do framework: tempo sempre com fuso explícito |
| `excluidoEm` | Instante ou nulo | Exclusão lógica, para que a exclusão também sincronize |
| `pendenteSync` | Booleano | Só é usado no premium; no grátis fica sempre falso |

**Tabelas iniciais:**

| Tabela | Conteúdo principal |
| --- | --- |
| `moto` | Apelido, marca, modelo, ano, categoria (custom, street, trail, scooter) |
| `leitura_hodometro` | Moto, km, data. O km atual é a leitura mais recente, não um campo solto na moto |
| `registro` | Moto, tipo (manutenção, abastecimento, ocorrência, rolê, nota), data, km, texto, custo, latitude, longitude, precisão, nome do lugar (opcional) |
| `item_plano` | Moto, serviço, intervalo em km, intervalo em dias, última execução |
| `pendencia` | Ocorrência que gerou a pendência, descrição, resolvida em |
| `foto` | Registro, caminho do arquivo local, caminho remoto (premium) |
| `rota` | Moto, registro de rolê, início, fim, distância, estado (gravando, concluída, interrompida) |
| `ponto_rota` | Rota, instante, latitude, longitude, altitude, velocidade, precisão. Não sincroniza ponto a ponto; vai compactada com a rota |

**Regras que evitam bugs conhecidos:**

- "Sem dado" é nulo, nunca zero. Custo não informado não entra na média; km não informado não vira km 0. Lição 8.4 do framework.
- Coordenadas ficam em `Double`, com a precisão em metros ao lado. Um ponto com precisão pior que 50 m é descartado da rota.
- Localização é sempre opcional. Registro sem local é um registro válido.

**Migrations:**

- `exportSchema = true` desde a versão 1, com os JSON do esquema versionados no repositório.
- Cada mudança de esquema tem uma `Migration` escrita à mão e um teste com `MigrationTestHelper`. Nunca `fallbackToDestructiveMigration`: ele apaga o diário do usuário.
- O teste migra a partir de **toda** versão já publicada, não só da anterior. Quem ficou meses sem atualizar o app precisa chegar à versão nova com os dados inteiros. É a "migration tolerante por N versões" do framework (seção 11, app mobile).

## GPS sem rede

O GPS recebe sinal dos satélites e não precisa de internet. O que depende de rede é o que vem em volta: a primeira posição rápida, o nome do lugar e o mapa.

| Parte | Sem internet | Como tratar |
| --- | --- | --- |
| Posição | Funciona. A primeira leitura pode levar de 30 s a alguns minutos em céu aberto | Mostrar "procurando satélites" e deixar o usuário salvar sem esperar |
| Nome do lugar (`Geocoder`) | Não funciona | Salvar só as coordenadas; um worker com rede preenche o nome depois. O usuário também pode digitar |
| Mapa de fundo | Não funciona, a menos que a área já esteja em cache | Mostrar a rota como traçado sobre fundo neutro; o mapa aparece quando houver rede. Mapas offline por região ficam para depois do MVP |
| Distância da rota | Funciona | Soma de `Location.distanceBetween` entre pontos válidos, calculada no aparelho |

**Provedor de localização:** `FusedLocationProviderClient` (Google Play Services), com alta precisão. Em aparelhos sem Play Services, como alguns Huawei, o fallback é o `LocationManager` com `GPS_PROVIDER`. Os dois ficam atrás de uma interface `FonteDeLocalizacao`, o que também permite um provedor falso nos testes.

### Local do registro

Ao salvar um registro com "marcar local" ligado, o app pede uma única posição com `getCurrentLocation` e tempo-limite de cerca de 20 s. Se o tempo acabar, o registro é salvo sem local e o usuário é avisado. Pede só `ACCESS_FINE_LOCATION`, em uso, na primeira vez que o recurso é ligado.

### Gravação da rota

A rota é gravada por um **serviço em primeiro plano** iniciado pelo usuário, com notificação fixa e botão de parar. Isso atende o Android 8, que limita serviços em segundo plano, e evita pedir localização em segundo plano (`ACCESS_BACKGROUND_LOCATION`), que exige declaração especial na Play e assusta o usuário.

- **Permissões:** `ACCESS_FINE_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION` (Android 14+) com `foregroundServiceType="location"`, e `POST_NOTIFICATIONS` (Android 13+) para a notificação.
- **Frequência:** um ponto a cada 3 a 5 s ou 10 m, o que vier por último. Uma hora de rolê dá cerca de mil pontos, algo como 50 KB no banco.
- **Gravação:** os pontos vão para o Room em lotes de poucos segundos. Se o sistema matar o app, perde-se no máximo o último lote, e a rota fica marcada como "interrompida", com o que já foi gravado.
- **Bateria:** o serviço para sozinho após um tempo parado configurável (ex.: 15 min sem se mover), com aviso.
- **Hodômetro:** ao terminar, o app **sugere** somar a distância ao hodômetro, e o usuário confirma. Somar km automaticamente ficou fora do escopo (opção (c), não escolhida).
- **Fabricantes agressivos:** Xiaomi, Samsung e outros podem encerrar serviços para poupar bateria. O app mostra, uma vez, como tirar o Diário de Bordo da otimização de bateria.

## Lembretes

Os lembretes não dependem de rede nem de servidor.

- **Por km:** recalculados na hora em que o usuário atualiza o hodômetro (ou confirma a distância de uma rota). Se um item venceu, a notificação sai no mesmo momento.
- **Por tempo:** um `PeriodicWorkRequest` diário confere os itens do plano e notifica o que vence nos próximos dias. Não precisa de alarme exato, então não pede a permissão `SCHEDULE_EXACT_ALARM`.
- **Reinício do aparelho:** o WorkManager reagenda sozinho após o boot.

## Fotos

As fotos ficam no armazenamento interno do app (`filesDir/fotos`), comprimidas em JPEG com lado maior de 1600 px. O banco guarda só o caminho. Não é preciso permissão de armazenamento: a câmera e a galeria entram pelo seletor de fotos do sistema.

No grátis, o limite é de 3 fotos por registro, como no plano de produto.

## Backup no plano grátis

O backup do grátis é um arquivo que o usuário guarda onde quiser, sem nenhum servidor.

- **Exportar:** gera um `.zip` com os dados em JSON (com a versão do esquema) e as fotos. O usuário escolhe onde salvar pelo seletor do sistema (`ACTION_CREATE_DOCUMENT`), que pode ser o Drive, o WhatsApp ou um cartão SD. Não precisa de permissão.
- **Importar:** lê o `.zip`, converte a partir da versão do esquema que o gerou e grava tudo numa transação. Se algo falhar, nada muda.
- **Backup automático do Android:** o sistema pode guardar até 25 MB do app na conta Google do usuário, de graça. Fica ligado só para o arquivo exportado mais recente, e não para o banco aberto, que pode ser copiado no meio de uma escrita.

## Backup e sincronização no premium

O premium usa Supabase (Postgres, Storage e Auth), como no plano de produto. Todo o código fica atrás da flag `premium` desde a fase 4, e só é liberado na fase 6.

**Envio (aparelho para nuvem):**

1. Toda escrita local marca o registro com `pendenteSync = true` e atualiza `alteradoEm`.
2. Um `SyncWorker` único (`enqueueUniqueWork`) roda com a restrição `NetworkType.CONNECTED`, após cada alteração (com alguns segundos de espera para juntar várias) e periodicamente.
3. Ele envia os pendentes em lotes com `upsert` pelo `id`, e só então limpa a marca. Se a rede cair no meio, o WorkManager tenta de novo com espera exponencial.

**Recebimento (nuvem para aparelho):**

- O servidor carimba cada linha com `atualizado_no_servidor`. O aparelho guarda o maior carimbo recebido e pede só o que mudou depois dele. O relógio do aparelho não entra nessa conta.

**Conflitos:**

- É um usuário com poucos aparelhos, então vence a alteração mais recente por registro (`alteradoEm`). Uma exclusão (`excluidoEm`) vence uma edição mais antiga.
- As rotas vão como um arquivo compactado no Storage, não ponto a ponto, e nunca são editadas depois de concluídas, então não geram conflito.
- As fotos sobem para o Storage depois dos dados; o registro guarda o caminho remoto.

**Segurança:**

- RLS no Postgres por `user_id` em toda tabela: cada usuário só lê e grava o que é seu.
- O token de sessão fica guardado com chave do Android Keystore, nunca em texto puro.
- A localização é dado pessoal (LGPD). Só sobe para a nuvem com backup ativo, e a política de privacidade diz isso.

## Testes

A garantia de que "funciona sem internet" vem de testes automáticos e de um roteiro manual curto em modo avião.

| O quê | Como |
| --- | --- |
| Regras de domínio (km, consumo, distância) | Testes JVM puros, com casos de "sem dado" |
| DAOs e consultas do Room | Testes com banco em memória |
| Migrations | `MigrationTestHelper` a partir de toda versão publicada |
| Workers (lembrete, sync) | `WorkManagerTestInitHelper` e `TestDriver` |
| Gravação de rota | `FonteDeLocalizacao` falsa reproduzindo uma rota gravada |
| Aparelhos | Emulador API 26 (Android 8) e API 36 no CI |
| Roteiro manual | Modo avião: criar moto, registrar com local, gravar 10 min de rota com a tela apagada, exportar e importar em outro aparelho |

## Plano de implementação

Os passos seguem o roadmap do plano de produto. Cada um termina com um critério verificável.

| Fase | Entregas offline-first | Critério de pronto |
| --- | --- | --- |
| 0. Fundação | Room versão 1 com os campos de controle, `exportSchema`, `FonteDeLocalizacao` com fallback, teste de migration no CI | O projeto compila e o CI roda os testes do Room em API 26 e 36 |
| 1. MVP | Moto, hodômetro, diário com local opcional, gravação de rota, lembretes por km e tempo, exportar e importar | Roteiro manual em modo avião passa inteiro; uma rota de 2 h com a tela apagada é gravada sem buracos maiores que 30 s |
| 2. Teste fechado | Medir rotas interrompidas e falhas de GPS pelos testadores | Menos de 5% de rotas "interrompidas" |
| 4. Evolução na ativa | `SyncWorker`, Supabase com RLS, envio de fotos, tudo atrás da flag `premium` | Dois aparelhos convergem para os mesmos dados após editar os dois offline |
| 6. Premium | Liberar a flag para assinantes | Backup restaurado num aparelho novo com fotos e rotas |

## Decisões

Decididas por Pedro em 26/09/2026.

- **Mapa sem internet:** no MVP, só o viável. Sem rede, a rota aparece como traçado sobre fundo neutro; com rede, sobre o mapa normal. Baixar mapas offline por região fica para depois do MVP.
- **Backup automático do Android:** ligado só para o arquivo exportado mais recente, nunca para o banco aberto.
- **Conflitos no premium:** vale a alteração mais recente de cada registro (`alteradoEm`), e uma exclusão vence uma edição mais antiga.

## Fontes

- [Plano de produto](plano-de-produto.md)
- [Guia de arquitetura offline-first do Android](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Testar migrations do Room](https://developer.android.com/training/data-storage/room/migrating-db-versions#test)
- [Tipos de serviço em primeiro plano (location)](https://developer.android.com/develop/background-work/services/fgs/service-types#location)
- [Limites de execução em segundo plano (Android 8)](https://developer.android.com/about/versions/oreo/background)
- [Backup automático do Android](https://developer.android.com/identity/data/autobackup)
- framework-aprendido: erros 8.2, 8.3 e 8.4, e seção 11 (app mobile)
