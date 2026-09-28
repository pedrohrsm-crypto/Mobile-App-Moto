// Lógica de build própria do projeto (hoje: verificação do manifesto do release).
// Os testes daqui rodam com ./gradlew -p buildSrc test.
plugins {
    `kotlin-dsl`
}

dependencies {
    testImplementation(libs.junit)
}
