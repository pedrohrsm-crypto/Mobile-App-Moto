// Plugins declarados uma vez aqui e aplicados nos módulos. Versões em gradle/libs.versions.toml.
// AGP 9 já traz o Kotlin embutido: não aplicar org.jetbrains.kotlin.android.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}
