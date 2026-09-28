import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Regras de segurança do manifesto final (já mesclado com o das bibliotecas).
 * Função pura, testada em buildSrc/src/test; a tarefa do Gradle só lê o arquivo e chama aqui.
 */
object VerificadorManifesto {
    private const val ANDROID = "http://schemas.android.com/apk/res/android"

    private val tiposDeComponente = listOf("activity", "activity-alias", "service", "receiver", "provider")

    /** Permissões que o app não deve pedir (docs/offline-first.md e política da Play). */
    val permissoesProibidas = setOf(
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.SCHEDULE_EXACT_ALARM",
        "android.permission.USE_EXACT_ALARM",
        "android.permission.QUERY_ALL_PACKAGES",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
    )

    /** Devolve a lista de problemas; vazia quando o manifesto está dentro das regras. */
    fun verificar(xml: String, namespace: String, exportadosPermitidos: Set<String>): List<String> {
        val fabrica = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val documento = fabrica.newDocumentBuilder().parse(xml.byteInputStream())
        val problemas = mutableListOf<String>()

        for (tipo in tiposDeComponente) {
            val nos = documento.getElementsByTagName(tipo)
            for (i in 0 until nos.length) {
                val componente = nos.item(i) as Element
                if (componente.android("exported") != "true") continue
                val nome = nomeCompleto(componente.android("name"), namespace)
                val protegido = listOf("permission", "readPermission", "writePermission")
                    .any { componente.android(it).isNotEmpty() }
                if (!protegido && nome !in exportadosPermitidos) {
                    problemas += "$tipo exportado sem permissão e fora da lista de permitidos: $nome"
                }
            }
        }

        val aplicacao = documento.getElementsByTagName("application").item(0) as? Element
        if (aplicacao?.android("debuggable") == "true") problemas += "application com debuggable=true"
        if (aplicacao?.android("usesCleartextTraffic") == "true") problemas += "application com usesCleartextTraffic=true"

        val permissoes = documento.getElementsByTagName("uses-permission")
        for (i in 0 until permissoes.length) {
            val nome = (permissoes.item(i) as Element).android("name")
            if (nome in permissoesProibidas) problemas += "permissão proibida: $nome"
        }
        return problemas
    }

    private fun Element.android(atributo: String): String = getAttributeNS(ANDROID, atributo)

    private fun nomeCompleto(nome: String, namespace: String): String =
        if (nome.startsWith(".")) namespace + nome else nome
}
