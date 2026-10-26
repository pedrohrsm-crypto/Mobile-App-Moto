package com.pedrohrsm.diariodebordo.navegacao

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pedrohrsm.diariodebordo.garagem.ui.GaragemRoute

object Rotas {
    const val GARAGEM = "garagem"
}

@Composable
fun DiarioNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Rotas.GARAGEM) {
        composable(Rotas.GARAGEM) { GaragemRoute() }
    }
}
