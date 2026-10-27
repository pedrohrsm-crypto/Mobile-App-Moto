import com.android.build.api.artifact.SingleArtifact
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.pedrohrsm.diariodebordo"
    // compileSdk 37 porque androidx.core 1.19 exige; targetSdk 36 é a exigência atual da Play.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pedrohrsm.diariodebordo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Recursos premium ficam atrás desta flag (plano de produto, seção Stack técnica).
        buildConfigField("boolean", "PREMIUM_HABILITADO", "false")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Robolectric abre o Room de verdade na JVM (teste de abertura do banco, decisão #12) e compõe as telas
        // dos testes de Compose e das capturas (precisa dos recursos do app: strings, temas).
        unitTests.isIncludeAndroidResources = true
        // Robolectric 4.17 lê jdk.internal.access, que o JDK 21 fecha por padrão.
        unitTests.all { it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED") }
    }

    lint {
        abortOnError = true
        checkDependencies = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// 0.12 do plano de ação: o manifesto final do release passa por VerificadorManifesto (buildSrc).
// Rodar com ./gradlew verificarManifestoRelease. Novo componente exportado entra na lista com motivo.
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val sufixo = variant.name.replaceFirstChar { it.uppercase() }
        tasks.register<VerificarManifestoTask>("verificarManifesto$sufixo") {
            manifesto.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            namespace.set(variant.namespace)
            exportadosPermitidos.set(
                setOf(
                    // Tela de entrada: precisa ser exportada para o launcher abrir o app.
                    "com.pedrohrsm.diariodebordo.MainActivity",
                ),
            )
        }
    }
}

room {
    // Schemas exportados e versionados: base para escrever as Migrations.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    // hiltViewModel() saiu do hilt-navigation-compose (obsoleto lá) para este artefato.
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Diário paginado (PLANEJAMENTO §4): a consulta do Room vira PagingSource e a lista lê por páginas.
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    implementation(libs.androidx.work.runtime.ktx)
    // Orientação da foto do diário lida pelo EXIF (a versão do AndroidX corrige leituras da do sistema).
    implementation(libs.androidx.exifinterface)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.paging.testing)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
