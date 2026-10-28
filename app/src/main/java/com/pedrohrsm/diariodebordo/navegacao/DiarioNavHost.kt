package com.pedrohrsm.diariodebordo.navegacao

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavArgumentBuilder
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.theme.LocalEscalaDiario
import com.pedrohrsm.diariodebordo.diario.ui.DiarioRoute
import com.pedrohrsm.diariodebordo.diario.ui.RegistroFormRoute
import com.pedrohrsm.diariodebordo.diario.ui.RegistroFormViewModel
import com.pedrohrsm.diariodebordo.garagem.ui.AcoesMoto
import com.pedrohrsm.diariodebordo.garagem.ui.GaragemRoute
import com.pedrohrsm.diariodebordo.garagem.ui.MotoDetalheRoute
import com.pedrohrsm.diariodebordo.garagem.ui.MotoDetalheViewModel
import com.pedrohrsm.diariodebordo.garagem.ui.MotoFormRoute
import com.pedrohrsm.diariodebordo.plano.lembretes.NotificadorLembretes
import com.pedrohrsm.diariodebordo.plano.ui.AcoesPlano
import com.pedrohrsm.diariodebordo.plano.ui.ItemPlanoFormRoute
import com.pedrohrsm.diariodebordo.plano.ui.ItemPlanoFormViewModel
import com.pedrohrsm.diariodebordo.plano.ui.PlanoRoute
import com.pedrohrsm.diariodebordo.rota.ui.RotaDetalheRoute
import com.pedrohrsm.diariodebordo.rota.ui.RotaDetalheViewModel
import com.pedrohrsm.diariodebordo.rota.ui.RotasRoute

/**
 * Lista única dos destinos da barra inferior: a barra e o grafo leem daqui, e o rótulo é o
 * mesmo título da tela (docs/PLANEJAMENTO.md, UX/UI). Números entra com a frente dele.
 */
enum class Destino(val rota: String, @StringRes val rotulo: Int, @DrawableRes val icone: Int) {
    GARAGEM(Rotas.GARAGEM, R.string.garagem_titulo, R.drawable.ic_garagem),
    DIARIO(Rotas.DIARIO, R.string.diario_titulo, R.drawable.ic_diario),
    PLANO(Rotas.PLANO, R.string.plano_titulo, R.drawable.ic_plano),
    ROTAS(Rotas.ROTAS, R.string.rotas_titulo, R.drawable.ic_rotas),
}

/** Todas as rotas do app. Rotas com argumento têm a função que monta o caminho. */
object Rotas {
    const val GARAGEM = "garagem"
    const val MOTO_NOVA = "motos/nova"
    const val MOTO = "moto/{${MotoDetalheViewModel.ARG_MOTO_ID}}"
    const val MOTO_EDITAR = "moto/{${MotoDetalheViewModel.ARG_MOTO_ID}}/editar"

    const val DIARIO = "diario"
    const val REGISTRO_NOVO = "registro/novo?${RegistroFormViewModel.ARG_TIPO}={${RegistroFormViewModel.ARG_TIPO}}"
    const val REGISTRO = "registro/editar/{${RegistroFormViewModel.ARG_REGISTRO_ID}}"

    const val ROTAS = "rotas"
    const val ROTA = "rota/{${RotaDetalheViewModel.ARG_ROTA_ID}}"

    const val PLANO = "plano"
    const val ITEM_NOVO = "plano/item/novo"
    const val ITEM = "plano/item/{${ItemPlanoFormViewModel.ARG_ITEM_ID}}"
    const val PLANO_DA_MOTO = "moto/{${MotoDetalheViewModel.ARG_MOTO_ID}}/plano?$ARG_DESTAQUE={$ARG_DESTAQUE}"

    fun moto(motoId: String) = "moto/$motoId"
    fun motoEditar(motoId: String) = "moto/$motoId/editar"
    fun registroNovo(tipo: String? = null) =
        "registro/novo" + (tipo?.let { "?${RegistroFormViewModel.ARG_TIPO}=$it" } ?: "")
    fun registro(registroId: String) = "registro/editar/$registroId"
    fun rota(rotaId: String) = "rota/$rotaId"
    fun item(itemId: String) = "plano/item/$itemId"

    /** Plano da moto aberto por um lembrete, com o item avisado em destaque. */
    fun planoDaMoto(motoId: String, destaque: String?) =
        "moto/$motoId/plano" + (destaque?.let { "?$ARG_DESTAQUE=${Uri.encode(it)}" } ?: "")
}

private const val ARG_DESTAQUE = "destaque"

/** Destino pedido de fora do app (notificação de lembrete). */
data class Abertura(val motoId: String, val itemId: String?)

/** Lê o destino que o toque na notificação de lembrete mandou (extras de [NotificadorLembretes]). */
fun Intent.comoAbertura(): Abertura? {
    val motoId = getStringExtra(NotificadorLembretes.EXTRA_MOTO_ID) ?: return null
    return Abertura(motoId, getStringExtra(NotificadorLembretes.EXTRA_ITEM_ID))
}

@Composable
fun DiarioNavHost(
    abertura: Abertura? = null,
    onAberturaTratada: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    LaunchedEffect(abertura) {
        if (abertura != null) {
            // A moto é o hub: o voltar a partir do plano leva à moto (docs/PLANEJAMENTO.md, UX/UI).
            navController.navigate(Rotas.moto(abertura.motoId)) { popUpTo(Rotas.GARAGEM) }
            navController.navigate(Rotas.planoDaMoto(abertura.motoId, abertura.itemId))
            onAberturaTratada()
        }
    }
    val entrada by navController.currentBackStackEntryAsState()
    val rotaAtual = entrada?.destination?.route
    Scaffold(
        // Cada tela desconta as próprias barras (TelaDiario); aqui só entra a barra inferior.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (Destino.entries.any { it.rota == rotaAtual }) {
                BarraDestinos(rotaAtual, onEscolher = { navController.irParaAba(it) })
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Rotas.GARAGEM,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            grafoGaragem(navController)
            grafoDiario(navController)
            grafoPlano(navController)
            grafoRotas(navController)
        }
    }
}

/**
 * Barra inferior com os destinos da lista única. Com 4 destinos em 360 dp, cada item tem 90 dp: com a fonte
 * grande (200%, ou 140% no modo oficina) o rótulo quebrava no meio ("Garag/em"), e cortado ("Gara…") ainda
 * não cabia. Com a fonte grande a barra fica só com os ícones, nomeados para o TalkBack; o título da tela
 * aberta já diz o destino. Com a fonte normal, o rótulo fica numa linha só.
 */
@Composable
fun BarraDestinos(rotaAtual: String?, onEscolher: (Destino) -> Unit) {
    val fonteGrande = LocalDensity.current.fontScale * LocalEscalaDiario.current.fator >= FONTE_GRANDE
    NavigationBar {
        Destino.entries.forEach { destino ->
            val nome = stringResource(destino.rotulo)
            NavigationBarItem(
                selected = destino.rota == rotaAtual,
                onClick = { onEscolher(destino) },
                colors = coresBarraDestinos(),
                icon = {
                    Icon(painterResource(destino.icone), contentDescription = nome.takeIf { fonteGrande })
                },
                label = if (fonteGrande) {
                    null
                } else {
                    { Text(nome, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                },
            )
        }
    }
}

/**
 * Cores da barra. O padrão do Material tem dois defeitos de contraste: o rótulo selecionado (`secondary` sobre
 * `surfaceContainer`) dava 3,75:1 no Estrada (erro #55), e o indicador (`secondaryContainer`) fica de 1,02 a 1,34:1 sobre
 * a barra; com a fonte grande a barra só tem ícones e o selecionado era igual ao não selecionado no modo oficina,
 * então nada mostrava o destino aberto. Indicador em `primary` (de 5,54 a 11,21:1 sobre a barra), ícone selecionado em
 * `onPrimary` sobre ele e rótulo selecionado em `primary`. O `ContrasteComponentesTest` lê esta função.
 */
@Composable
internal fun coresBarraDestinos(): NavigationBarItemColors {
    val cores = MaterialTheme.colorScheme
    return NavigationBarItemDefaults.colors(
        selectedIconColor = cores.onPrimary,
        selectedTextColor = cores.primary,
        indicatorColor = cores.primary,
    )
}

/** Fonte do sistema × escala do modo oficina a partir da qual o rótulo de todos os destinos não cabe. */
private const val FONTE_GRANDE = 1.8f

fun NavController.irParaAba(destino: Destino) {
    navigate(destino.rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavGraphBuilder.grafoGaragem(nav: NavController) {
    composable(Rotas.GARAGEM) {
        GaragemRoute(
            onAbrirMoto = { nav.navigate(Rotas.moto(it)) },
            onNovaMoto = { nav.navigate(Rotas.MOTO_NOVA) },
        )
    }
    composable(Rotas.MOTO_NOVA) {
        MotoFormRoute(onSalvo = { id -> nav.navigate(Rotas.moto(id)) { popUpTo(Rotas.GARAGEM) } })
    }
    composable(Rotas.MOTO_EDITAR) {
        MotoFormRoute(onSalvo = { nav.popBackStack() })
    }
    composable(Rotas.MOTO) { entrada ->
        val motoId = checkNotNull(entrada.arguments?.getString(MotoDetalheViewModel.ARG_MOTO_ID))
        MotoDetalheRoute(
            acoes = AcoesMoto(
                onEditar = { nav.navigate(Rotas.motoEditar(motoId)) },
                onDiario = { nav.irParaAba(Destino.DIARIO) },
            ),
        )
    }
}

private fun NavGraphBuilder.grafoDiario(nav: NavController) {
    composable(Rotas.DIARIO) {
        DiarioRoute(
            onNovoRegistro = { nav.navigate(Rotas.registroNovo()) },
            onAbrirRegistro = { nav.navigate(Rotas.registro(it)) },
            onCadastrarMoto = { nav.navigate(Rotas.MOTO_NOVA) },
        )
    }
    composable(Rotas.REGISTRO_NOVO, arguments = listOf(navArgument(RegistroFormViewModel.ARG_TIPO) { opcional() })) {
        RegistroFormRoute(onConcluido = { nav.popBackStack() })
    }
    composable(Rotas.REGISTRO) {
        RegistroFormRoute(onConcluido = { nav.popBackStack() })
    }
}

private fun NavGraphBuilder.grafoRotas(nav: NavController) {
    composable(Rotas.ROTAS) {
        RotasRoute(
            onAbrirRota = { nav.navigate(Rotas.rota(it)) },
            onCadastrarMoto = { nav.navigate(Rotas.MOTO_NOVA) },
        )
    }
    composable(Rotas.ROTA) {
        RotaDetalheRoute(onExcluida = { nav.popBackStack() })
    }
}

private fun NavArgumentBuilder.opcional() {
    type = NavType.StringType
    nullable = true
    defaultValue = null
}

private fun NavGraphBuilder.grafoPlano(nav: NavController) {
    val acoes = AcoesPlano(
        onNovoItem = { nav.navigate(Rotas.ITEM_NOVO) },
        onEditarItem = { nav.navigate(Rotas.item(it)) },
        onCadastrarMoto = { nav.navigate(Rotas.MOTO_NOVA) },
    )
    composable(Rotas.PLANO) { PlanoRoute(acoes) }
    composable(Rotas.PLANO_DA_MOTO, arguments = listOf(navArgument(ARG_DESTAQUE) { opcional() })) { entrada ->
        PlanoRoute(acoes, destaque = entrada.arguments?.getString(ARG_DESTAQUE))
    }
    composable(Rotas.ITEM_NOVO) { ItemPlanoFormRoute(onConcluido = { nav.popBackStack() }) }
    composable(Rotas.ITEM) { ItemPlanoFormRoute(onConcluido = { nav.popBackStack() }) }
}
