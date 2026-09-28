import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificadorManifestoTest {

    private val namespace = "com.pedrohrsm.diariodebordo"
    private val permitidos = setOf("com.pedrohrsm.diariodebordo.MainActivity")

    private fun manifesto(aplicacao: String = "", corpo: String = "", permissoes: String = "") = """
        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="$namespace">
            $permissoes
            <application $aplicacao>
                <activity android:name=".MainActivity" android:exported="true" />
                <service android:name="androidx.work.impl.background.systemjob.SystemJobService"
                    android:exported="true" android:permission="android.permission.BIND_JOB_SERVICE" />
                <receiver android:name="com.exemplo.Interno" android:exported="false" />
                $corpo
            </application>
        </manifest>
    """.trimIndent()

    private fun verificar(xml: String) = VerificadorManifesto.verificar(xml, namespace, permitidos)

    @Test
    fun manifestoDoAppPassa() {
        assertEquals(emptyList<String>(), verificar(manifesto()))
    }

    @Test
    fun componenteExportadoPlantadoReprova() {
        val problemas = verificar(manifesto(corpo = """<service android:name=".Vazado" android:exported="true" />"""))
        val esperado = "service exportado sem permissão e fora da lista de permitidos: $namespace.Vazado"
        assertEquals(listOf(esperado), problemas)
    }

    @Test
    fun providerExportadoSoComPermissaoDeLeituraPassa() {
        val corpo = """<provider android:name=".Arquivos" android:exported="true" android:readPermission="x.LER" />"""
        assertEquals(emptyList<String>(), verificar(manifesto(corpo = corpo)))
    }

    @Test
    fun debuggableECleartextReprovam() {
        val aplicacao = """android:debuggable="true" android:usesCleartextTraffic="true""""
        val problemas = verificar(manifesto(aplicacao = aplicacao))
        assertEquals(2, problemas.size)
    }

    @Test
    fun localizacaoEmSegundoPlanoReprova() {
        val permissoes = """<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />"""
        assertTrue(verificar(manifesto(permissoes = permissoes)).single().contains("ACCESS_BACKGROUND_LOCATION"))
    }
}
