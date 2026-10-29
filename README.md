# Diário de Bordo

App Android de diário de bordo e manutenção para motociclistas, em Kotlin nativo (Jetpack Compose + Room), 100% offline.

**Status:** em construção, como MVP de portfólio. Marco do MVP: 3-Jan-2027 ([cronograma](docs/CRONOGRAMA.md)).

Construído por frentes de IA (Claude Code) sob o [Framework Aprendido](https://github.com/pedrohrsm-crypto/framework-aprendido) v2.0: cada frente entrega um PR com os portões verdes, revisado e integrado pelo PM; o dono aceita.

## Documentos

- [Plano de produto](docs/plano-de-produto.md): escopo, stack, roadmap, publicação na Play Store, monetização, custos, métricas e riscos
- [Offline-first](docs/offline-first.md): dados, GPS sem rede, lembretes, backup e sincronização
- [Planejamento com as áreas](docs/PLANEJAMENTO.md) e [plano de ação](docs/plano-de-acao.md)
- [Design no Figma](https://www.figma.com/design/XLfpBLh0ndZhvYecq2uYbz): wireframe do fluxo e telas em alta fidelidade nos temas Oficina, Estrada e Cromo

## Instalar o APK de teste

Cada push na `main` gera um APK de release assinado, para o uso real antes da Play Store.

1. No GitHub, abra **Actions** → **CI**, escolha o run verde do commit desejado e baixe, em **Artifacts**, o `diario-de-bordo-release-<sha7>` (os 7 primeiros caracteres do commit; é preciso estar logado no GitHub).
2. Descompacte o `.zip` e passe o `app-release.apk` para o celular (Android 8 ou mais novo).
3. Abra o arquivo no celular e permita a instalação de apps de fonte desconhecida quando o Android pedir.

Para atualizar, instale o APK de um run mais novo por cima: os dados ficam.

**Aviso:** a versão da Play Store vai ser assinada por outra chave (Play App Signing) e não instala por cima deste APK; vai pedir para desinstalar, o que apaga os dados do aparelho. Antes de trocar, use **Exportar backup** nos ajustes, guarde o `.zip` fora do aparelho e, depois de instalar a versão da Play, use **Importar backup**.
