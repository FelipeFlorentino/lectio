import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Chave própria do app (fora do git). Toda versão precisa da mesma chave para o celular aceitar a atualização.
val assinatura = Properties().apply {
    val arquivo = rootProject.file("assinatura/assinatura.properties")
    if (arquivo.exists()) arquivo.inputStream().use { load(it) }
}

android {
    namespace = "com.felipe.palavradehoje"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.felipe.lectio"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.1.1"
        // Repositório no GitHub onde as versões novas são publicadas.
        buildConfigField("String", "REPO_ATUALIZACAO", "\"${property("atualizacao.repo")}\"")
    }

    signingConfigs {
        if (!assinatura.isEmpty) create("release") {
            storeFile = file(assinatura.getProperty("storeFile"))
            storePassword = assinatura.getProperty("storePassword")
            keyAlias = assinatura.getProperty("keyAlias")
            keyPassword = assinatura.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("io.coil-kt:coil-compose:2.7.0")
}
