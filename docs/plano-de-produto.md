# Diário de Bordo — Plano de produto

25/09/2026 · Pedro

## Resumo e premissas

O Diário de Bordo vira um app Android gratuito para motociclistas: publicado ainda na ativa, sem nenhuma cobrança, e monetizado com um plano premium só depois da sua saída do Exército, por meio de uma empresa com sua esposa.

- **Público:** motociclistas de qualquer marca e modelo. Carros ficam para uma fase futura.
- **Tecnologia:** Kotlin nativo, com Jetpack Compose.
- **Para o usuário:** download grátis, sem anúncios. O premium é opcional.
- **Receita:** só começa após a saída do Exército.
- **Cronograma:** contado em meses a partir do início (M1, M2…), já que a data de saída ainda não está definida. Considera 8 a 10 h por semana de dedicação.

## Enquadramento legal e societário

Na ativa, o app existe só como projeto pessoal e gratuito. A empresa com sua esposa e a cobrança entram depois da saída.

O [art. 29 do Estatuto dos Militares](https://modeloinicial.com.br/lei/L-6880-1980/estatuto-militares/art-29) proíbe o militar da ativa de comerciar ou de administrar uma sociedade, com exceção de ser quotista ou acionista. Para oficiais, o comércio é [tratado como crime militar](https://www.jusbrasil.com.br/artigos/o-exercicio-de-comercio-por-oficial-das-forcas-armadas-a-luz-do-conceito-de-empresa-trazido-pelo-codigo-civil-e-a-habitualidade-do-agente/1743572497), com pena de suspensão do posto ou reforma. Este plano não é parecer jurídico.

| Fase | Pode fazer | Não fazer | Estrutura |
| --- | --- | --- | --- |
| Na ativa | Desenvolver, publicar grátis, coletar feedback | Assinatura, compra no app, anúncios, doações, patrocínio | Conta pessoal na Play, em seu nome |
| Após a saída | Cobrar pelo premium | — | LTDA com você e sua esposa como sócios, conta de organização na Play |

**Passagem de uma fase para a outra:**

1. Abrir a LTDA e, com um contador, definir o regime tributário (provavelmente Simples Nacional). A receita do Google vem do exterior, e o contador orienta sobre câmbio e tributos.
2. Solicitar o número D-U-N-S da empresa (é gratuito), exigido para contas de organização.
3. Criar a conta de organização na Play Console. Uma conta pessoal não pode ser convertida em organização, mas o app pode ser transferido entre contas sem perder usuários nem avaliações.
4. Ativar o perfil de pagamentos da empresa e, só então, liberar o premium.

Antes de publicar, vale uma consulta rápida à assessoria jurídica para confirmar que um app gratuito, sem nenhuma receita, não gera problema.

## Produto

O gratuito precisa resolver sozinho o problema de lembrar e registrar a manutenção. O premium vende conveniência e segurança dos dados, nunca o básico.

| Recurso | Grátis | Premium |
| --- | --- | --- |
| Motos cadastradas | 1 | Ilimitadas |
| Diário (manutenção, abastecimento, ocorrência, rolê, nota) | Sim | Sim |
| Plano de manutenção com lembretes por km e por tempo | Sim | Sim |
| Ocorrência que gera pendências de inspeção | Sim | Sim |
| 3 temas e modo oficina | Sim | Sim |
| Fotos nos registros | Até 3 por registro, no aparelho | Ilimitadas, com cópia na nuvem |
| Backup e sincronização entre aparelhos | Exportar e importar arquivo | Automático na nuvem |
| Relatório em PDF do histórico, útil na revenda | — | Sim |
| Estatísticas | Mês atual | Qualquer período, com comparativos |

**Planos de manutenção por modelo:** o app parte de planos-base por categoria (custom, street, trail, scooter), e o usuário ajusta os intervalos pelo manual da própria moto. Especificações como volume de óleo e torque entram como dado do usuário, com aviso para conferir no manual. Nenhum texto é copiado de manual de fabricante, por causa do direito autoral.

**Fora do MVP:** carros, recursos sociais ou de comunidade, leitura de dados por OBD e marketplace de peças.

## Stack técnica

O app funciona 100% offline desde o primeiro dia. A nuvem só entra no premium, então o MVP roda sem nenhum custo de servidor.

| Camada | Escolha | Observação |
| --- | --- | --- |
| Interface | Jetpack Compose + Material 3 | Os 3 temas do Figma viram `ColorScheme`; o modo oficina é uma escala de tipografia e de espaçamento |
| Arquitetura | MVVM, Hilt, Coroutines e Flow | Um módulo por funcionalidade: diário, plano, números, garagem |
| Dados locais | Room | Fonte da verdade no aparelho |
| Lembretes | WorkManager + notificações | Os lembretes por km são recalculados a cada atualização do hodômetro |
| Fotos | Armazenamento interno com compressão | Limite no plano grátis |
| Backup (premium) | Supabase (Postgres, Storage, Auth) via supabase-kt | Plano grátis do Supabase no início |
| Pagamentos (premium) | Google Play Billing Library | Assinatura e compra vitalícia |
| Relatório PDF | `PdfDocument` do Android | Sem biblioteca paga |
| Estabilidade | Firebase Crashlytics | Gratuito |
| Build | targetSdk 36, minSdk 26, GitHub Actions gerando o AAB | A Play exige API 36 para [apps novos e atualizações desde 31/08/2026](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en), e essa exigência sobe todo ano |

Desde o início, os recursos premium ficam atrás de uma flag. Assim eles podem ser construídos e testados na fase gratuita e liberados depois, sem precisar reescrever nada.

## Roadmap

O app gratuito chega à loja por volta do M4. Tudo o que vem depois dele até a saída do Exército é construir base de usuários e deixar o premium pronto.

| Fase | Quando | Entregas | Critério para avançar |
| --- | --- | --- | --- |
| 0. Fundação | M1, 2 semanas | Repositório, arquitetura, temas em Compose, conta pessoal na Play | Projeto compila e o tema troca em tempo real |
| 1. MVP | M1–M3 | Moto e hodômetro, diário com 4 tipos, plano com lembretes, modo oficina | Você usa na M800 por 4 semanas sem planilha paralela |
| 2. Teste fechado | M4 | 12+ testadores por 14 dias, correções | Produção liberada e 99% de sessões sem falha |
| 3. Lançamento grátis | M4–M5 | Ficha da loja, divulgação em grupos de motociclistas | 500 instalações e 20% de retenção em 30 dias |
| 4. Evolução na ativa | M5 até a saída | Planos por categoria, estatísticas, PDF e backup atrás de flag | Lista de pedidos dos usuários priorizada |
| 5. Empresa | Saída + 1 a 2 meses | LTDA, D-U-N-S, conta de organização, transferência do app | App transferido e perfil de pagamentos ativo |
| 6. Premium | Após a fase 5 | Assinatura e compra vitalícia, desconto de fundador para quem veio da fase grátis | 2% a 3% dos usuários ativos pagando |

## Publicação na Play Store

Publicar custa US$ 25 uma única vez. O gargalo é o teste fechado obrigatório: conte de 3 a 4 semanas entre a conta aprovada e o app no ar.

- **Conta:** taxa única de US$ 25, sem renovação anual ([fonte](https://primetestlab.com/blog/google-play-developer-fee)). Exige verificação de identidade e um aparelho Android real.
- **Teste fechado:** contas pessoais criadas após 13/11/2023 precisam de pelo menos 12 testadores inscritos por 14 dias seguidos antes de pedir acesso à produção ([fonte](https://primetestlab.com/blog/google-play-changed-20-to-12-testers)). Recrute de 15 a 20 pessoas no seu moto clube, para ter folga caso alguém desista.
- **API alvo:** targetSdk 36 hoje. Reserve uma janela todo mês de agosto para a atualização anual exigida pelo Google.
- **Obrigatórios:** política de privacidade publicada (adequada à LGPD) e o formulário de Segurança dos dados preenchido. O app guarda hodômetro, fotos e, opcionalmente, localização do posto.
- **Ficha da loja:** ícone, 6 a 8 capturas mostrando os temas e o modo oficina, e uma descrição com os termos que as pessoas buscam ("manutenção moto", "revisão moto", "consumo moto", "troca de óleo").

## Monetização e preço

O modelo é freemium sem anúncios. Com 5.000 usuários ativos e 3% de conversão no plano anual, a receita fica em torno de R$ 530 por mês, antes de impostos e custos. É uma renda complementar que cresce com a base de usuários.

**Preços sugeridos (hipóteses para testar):**

- Mensal: R$ 7,90
- Anual: R$ 49,90, o plano a destacar
- Vitalício: R$ 99,90

**Taxa do Google:** em assinaturas, fica em cerca de 15% (10% de serviço mais 5% de cobrança). A nova estrutura de taxas chega ao Brasil [até setembro de 2027](https://pricepush.app/blog/google-play-subscription-fees-2026-real-math). Com isso, o anual rende cerca de R$ 42,40 líquidos por assinante.

| Usuários ativos | Conversão | Assinantes anuais | Receita líquida por mês |
| --- | --- | --- | --- |
| 2.000 | 3% | 60 | R$ 212 |
| 5.000 | 3% | 150 | R$ 530 |
| 20.000 | 3% | 600 | R$ 2.120 |

Os valores são ilustrativos. Descontam só a taxa do Google e ainda não incluem impostos (o Simples Nacional começa em torno de 6%) nem custos de servidor.

## Custos estimados

A fase gratuita custa praticamente só os US$ 25 da conta. O custo fixo relevante começa com a empresa, e o maior item é o contador.

| Item | Custo aproximado | Quando |
| --- | --- | --- |
| Conta Google Play | US$ 25, uma vez | Fase 0 |
| Domínio para o site e a política de privacidade | cerca de R$ 40 por ano | Fase 3 |
| Supabase | Grátis no início; plano pago a partir de US$ 25 por mês quando crescer | Fase 6 |
| Abertura da LTDA | Varia por estado; peça orçamento ao contador | Fase 5 |
| Contador | cerca de R$ 200 a R$ 500 por mês | Fase 5 em diante |
| Número D-U-N-S | Gratuito | Fase 5 |
| Registro da marca no INPI | Taxas conforme a tabela do INPI | Antes do lançamento |

Os valores marcados como "cerca de" são estimativas de mercado. Confirme cada um antes de decidir.

## Métricas e metas

A métrica que decide se vale investir no premium é a retenção em 30 dias. Se ninguém continua usando o app, não há para quem vender.

| Métrica | Meta | Quando medir |
| --- | --- | --- |
| Sessões sem falha | ≥ 99% | Teste fechado em diante |
| Instalações | 500 | 3 meses após o lançamento |
| Retenção em 30 dias | ≥ 20% | A partir do lançamento |
| Registros por usuário ativo por mês | ≥ 4 | A partir do lançamento |
| Nota na loja | ≥ 4,5 | Contínuo |
| Conversão para o premium | 2% a 3% | 6 meses após liberar |

As metas são pontos de partida. Revise-as com os dados reais do primeiro mês.

## Riscos e mitigação

O maior risco não é técnico: é o tempo disponível ao lado da carreira, da faculdade e do concurso. Por isso o escopo do MVP é deliberadamente enxuto.

| Risco | Mitigação |
| --- | --- |
| Atividade enquadrada como comércio enquanto na ativa | Nenhuma receita até a saída; consulta prévia à assessoria jurídica |
| Pouco tempo disponível | MVP enxuto, 8 a 10 h por semana, uma fase por vez |
| Nicho pequeno | Planos-base para as motos mais vendidas no Brasil primeiro; carros como expansão futura |
| Concorrentes na loja | Pesquisar os 5 principais apps antes do M3; o diferencial é foco em moto, ocorrência que gera pendência e modo oficina |
| Especificação errada informada pelo usuário | Rótulo "dado do usuário" e aviso para conferir no manual |
| Mudanças de política do Google | Janela fixa em agosto para a atualização anual de API e revisão das políticas |
| Dados pessoais (LGPD) | Coletar o mínimo; localização opcional; política de privacidade clara |

## Próximos passos

Nas próximas duas semanas, o foco é validar o nome e o terreno e deixar o projeto de pé.

- [ ] Consultar a assessoria jurídica sobre publicar um app gratuito, sem receita
- [ ] Pesquisar os 5 principais apps de manutenção de moto na Play e anotar o que falta neles
- [ ] Definir o nome do app e checar a disponibilidade no INPI e na Play
- [ ] Criar o repositório Kotlin + Compose com os tokens dos 3 temas
- [ ] Criar a conta pessoal na Play Console (US$ 25)
- [ ] Listar de 15 a 20 testadores no moto clube
- [ ] Iniciar a fase 1: moto, hodômetro e diário

## Fontes

- [Estatuto dos Militares, art. 29](https://modeloinicial.com.br/lei/L-6880-1980/estatuto-militares/art-29)
- [Comércio por oficial das Forças Armadas (Jusbrasil)](https://www.jusbrasil.com.br/artigos/o-exercicio-de-comercio-por-oficial-das-forcas-armadas-a-luz-do-conceito-de-empresa-trazido-pelo-codigo-civil-e-a-habitualidade-do-agente/1743572497)
- [Taxa da conta Google Play](https://primetestlab.com/blog/google-play-developer-fee)
- [Regra dos 12 testadores](https://primetestlab.com/blog/google-play-changed-20-to-12-testers)
- [Requisitos de API alvo da Google Play](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
- [Taxas de assinatura na Google Play em 2026](https://pricepush.app/blog/google-play-subscription-fees-2026-real-math)
