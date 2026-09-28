package com.pedrohrsm.diariodebordo.navegacao

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.pedrohrsm.diariodebordo.diario.ui.DiarioRoute
import com.pedrohrsm.diariodebordo.diario.ui.RegistroFormRoute
import com.pedrohrsm.diariodebordo.diario.ui.RegistroFormViewModel
import com.pedrohrsm.diariodebordo.garagem.ui.AcoesMoto
import com.pedrohrsm.diariodebordo.garagem.ui.GaragemRoute
import com.pedrohrsm.diariodebordo.garagem.ui.MotoDetalheRoute
import com.pedrohrsm.diariodebordo.garagem.ui.MotoFormRoute

/**
 * Lista única dos destinos da barra inferior: a barra e o grafo leem daqui, e o rótulo é o
 * mesmo título da tela (docs/PLANEJAMENTO.md, UX/UI).
 */
enum class Destino(val rota: String, @StringRes val rotulo: Int, @DrawableRes val icone: Int) {
    GARAGEM(Rotas.GARAGEM, R.string.garagem_titulo, R.drawable.ic_garagem),
    DIARIO(Rotas.DIARIO, R.string.diario_titulo, R.drawable.ic_diario),
}

/** Todas as rotas do app. Rotas com argumento têm a função que monta o caminho. */
object Rotas {
    const val GARAGEM = "garagem"
    const val MOTO_NOVA = "motos/nova"
    const val MOTO = "moto/{motoId}"
    const val MOTO_EDITAR = "moto/{motoId}/editar"

    const val DIARIO = "diario"
    const val REGISTRO_NOVO = "registro/novo?tipo={tipo}&itemId={itemId}"
    const val REGISTRO = "registro/editar/{registroId}"

    fun moto(motoId: String) = "moto/$motoId"
    fun registroNovo(tipo: String? = null, itemId: String? = null) =
        "registro/novo" + listOfNotNull(tipo?.let { "tipo=$it" }, itemId?.let { "itemId=$it" })
            .joinToString("&").let { if (it.isEmpty()) "" else "?$it" }
    fun registro(registroId: String) = "registro/editar/$registroId"
    fun motoEditar(motoId: String) = "moto/$motoId/editar"
}

@Composable
fun DiarioNavHost(navController: NavHostController = rememberNavController()) {
    val entrada by navController.currentBackStackEntryAsState()
    val rotaAtual = entrada?.destination?.route
    Scaffold(
        // Cada tela desconta as próprias barras (TelaDiario); aqui só entra a barra inferior.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (Destino.entries.any { it.rota == rotaAtual }) {
                NavigationBar {
                    Destino.entries.forEach { destino ->
                        NavigationBarItem(
                            selected = destino.rota == rotaAtual,
                            onClick = { navController.irParaAba(destino) },
                            icon = { Icon(painterResource(destino.icone), contentDescription = null) },
                            label = { Text(stringResource(destino.rotulo)) },
                        )
                    }
                }
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
        }
    }
}

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
            onAjustes = {},
        )
    }
    composable(Rotas.MOTO_NOVA) {
        MotoFormRoute(onSalvo = { id -> nav.navigate(Rotas.moto(id)) { popUpTo(Rotas.GARAGEM) } })
    }
    composable(Rotas.MOTO_EDITAR) {
        MotoFormRoute(onSalvo = { nav.popBackStack() })
    }
    composable(Rotas.MOTO) { entrada ->
        val motoId = checkNotNull(entrada.arguments?.getString("motoId"))
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
    composable(
        Rotas.REGISTRO_NOVO,
        arguments = listOf(
            navArgument(RegistroFormViewModel.ARG_TIPO) { opcional() },
            navArgument(RegistroFormViewModel.ARG_ITEM_ID) { opcional() },
        ),
    ) {
        RegistroFormRoute(onConcluido = { nav.popBackStack() })
    }
    composable(Rotas.REGISTRO) {
        RegistroFormRoute(onConcluido = { nav.popBackStack() })
    }
}

private fun NavArgumentBuilder.opcional() {
    type = NavType.StringType
    nullable = true
    defaultValue = null
}
