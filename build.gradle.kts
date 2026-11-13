plugins {
    id("com.android.application") version "8.8.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

// O projeto fica no OneDrive, que bloqueia arquivos durante a sincronização.
// As saídas da compilação vão para uma pasta local, fora dele.
allprojects {
    layout.buildDirectory.set(file("${System.getenv("LOCALAPPDATA")}/Lectio-build/${project.name}"))
}
