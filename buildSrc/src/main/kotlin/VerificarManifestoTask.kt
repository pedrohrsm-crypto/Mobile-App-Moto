import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Reprova o build quando o manifesto final do release quebra alguma regra de [VerificadorManifesto]. */
abstract class VerificarManifestoTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val manifesto: RegularFileProperty

    @get:Input
    abstract val namespace: Property<String>

    @get:Input
    abstract val exportadosPermitidos: SetProperty<String>

    @TaskAction
    fun verificar() {
        val problemas = VerificadorManifesto.verificar(
            manifesto.get().asFile.readText(),
            namespace.get(),
            exportadosPermitidos.get(),
        )
        if (problemas.isNotEmpty()) {
            throw GradleException("Manifesto fora das regras:\n" + problemas.joinToString("\n") { "  - $it" })
        }
        logger.lifecycle("Manifesto dentro das regras.")
    }
}
