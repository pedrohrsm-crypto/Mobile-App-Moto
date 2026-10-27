package com.pedrohrsm.diariodebordo.rota.ui

import android.Manifest
import android.os.Build
import com.pedrohrsm.diariodebordo.localizacao.PermissoesLocalizacao

/** O que o toque em "Gravar rota" decide depois de conferir (e, se preciso, pedir) as permissões. */
enum class DecisaoGravacao { GRAVAR, SEM_LOCALIZACAO, SEM_NOTIFICACAO }

/**
 * Gravar a rota pede localização e, no Android 13 ou mais, a notificação: é nela que ficam o aviso fixo de GPS
 * em uso e o "Parar e salvar" (1.7). Sem uma das duas, a gravação não começa. Funções puras: a tela passa
 * quem está concedido e a versão do Android, e o teste roda na JVM.
 */
object PermissoesGravacao {
    private const val ANDROID_13 = Build.VERSION_CODES.TIRAMISU

    @Suppress("InlinedApi") // só entra na lista do Android 13 em diante (ANDROID_13)
    private const val NOTIFICACAO = Manifest.permission.POST_NOTIFICATIONS

    /** O que ainda falta pedir. Lista vazia: pode gravar sem perguntar nada. */
    fun queFaltam(concedida: (String) -> Boolean, sdk: Int): List<String> = buildList {
        if (PermissoesLocalizacao.PEDIDO.none(concedida)) addAll(PermissoesLocalizacao.PEDIDO)
        if (sdk >= ANDROID_13 && !concedida(NOTIFICACAO)) add(NOTIFICACAO)
    }

    fun decidir(concedida: (String) -> Boolean, sdk: Int): DecisaoGravacao = when {
        PermissoesLocalizacao.PEDIDO.none(concedida) -> DecisaoGravacao.SEM_LOCALIZACAO
        sdk >= ANDROID_13 && !concedida(NOTIFICACAO) -> DecisaoGravacao.SEM_NOTIFICACAO
        else -> DecisaoGravacao.GRAVAR
    }
}
