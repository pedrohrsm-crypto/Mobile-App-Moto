package com.pedrohrsm.diariodebordo.navegacao

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pedrohrsm.diariodebordo.core.designsystem.componentes.AjusteTemaScreen
import com.pedrohrsm.diariodebordo.core.designsystem.theme.AjusteTema
import com.pedrohrsm.diariodebordo.garagem.ui.GaragemRoute

object Rotas {
    const val GARAGEM = "garagem"
    const val AJUSTE_TEMA = "ajuste-tema"
}

@Composable
fun DiarioNavHost(ajuste: AjusteTema, aoAjustarTema: (AjusteTema) -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Rotas.GARAGEM) {
        composable(Rotas.GARAGEM) { GaragemRoute() }
        composable(Rotas.AJUSTE_TEMA) { AjusteTemaScreen(ajuste, aoAjustarTema) }
    }
}
