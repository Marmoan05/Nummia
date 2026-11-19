import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

ksp {
    arg("room.schemaLocation", "$projectDir/build/roomSchemas")
}

val releaseSigningProperties = Properties().apply {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.isFile) {
        propertiesFile.inputStream().use { load(it) }
    }
}
val appVersionName = providers.gradleProperty("appVersionName").orElse("0.9").get()
val appVersionCode = providers.gradleProperty("appVersionCode").map(String::toInt).orElse(9).get()

android {
    namespace = "com.example.nummia"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.nummia"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

    }

    signingConfigs {
        if (!releaseSigningProperties.isEmpty) {
            create("release") {
                storeFile = rootProject.file(
                    releaseSigningProperties.getProperty("storeFile")
                        ?: error("Falta storeFile en keystore.properties.")
                )
                storePassword = releaseSigningProperties.getProperty("storePassword")
                    ?: error("Falta storePassword en keystore.properties.")
                keyAlias = releaseSigningProperties.getProperty("keyAlias")
                    ?: error("Falta keyAlias en keystore.properties.")
                keyPassword = releaseSigningProperties.getProperty("keyPassword")
                    ?: error("Falta keyPassword en keystore.properties.")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (!releaseSigningProperties.isEmpty) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material3)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
}