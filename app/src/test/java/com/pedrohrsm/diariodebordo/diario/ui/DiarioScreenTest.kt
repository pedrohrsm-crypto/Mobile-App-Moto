package com.pedrohrsm.diariodebordo.diario.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.pedrohrsm.diariodebordo.R
import com.pedrohrsm.diariodebordo.core.designsystem.theme.DiarioTheme
import com.pedrohrsm.diariodebordo.diario.data.Registro
import com.pedrohrsm.diariodebordo.garagem.data.Moto
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Os três estados da lista paginada: um erro do Room aparece como erro, nunca como diário vazio. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DiarioScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val moto = Moto(id = "1", apelido = "Bandida", marca = "Harley-Davidson", modelo = "883")
    private fun texto(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    private fun mostrar(carga: LoadState) {
        val estados =
            LoadStates(refresh = carga, prepend = LoadState.NotLoading(true), append = LoadState.NotLoading(true))
        rule.setContent {
            DiarioTheme {
                DiarioScreen(
                    DiarioEstado.Pronto(moto, filtro = null),
                    flowOf(PagingData.empty<Registro>(estados)).collectAsLazyPagingItems(),
                )
            }
        }
    }

    @Test
    fun falhaAoCarregarMostraErroENaoDiarioVazio() {
        mostrar(LoadState.Error(IllegalStateException("disco cheio")))
        rule.onNodeWithText(texto(R.string.erro_ao_carregar), substring = true).assertIsDisplayed()
        rule.onNodeWithText(texto(R.string.diario_vazio)).assertDoesNotExist()
    }

    @Test
    fun diarioSemRegistrosMostraOVazioComAAcao() {
        mostrar(LoadState.NotLoading(endOfPaginationReached = true))
        rule.onNodeWithText(texto(R.string.diario_vazio)).assertIsDisplayed()
        rule.onNodeWithText(texto(R.string.erro_ao_carregar), substring = true).assertDoesNotExist()
    }
}
