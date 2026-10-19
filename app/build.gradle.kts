import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// 签名材料两个来源：CI 用 GitHub Secrets 注入的环境变量，本地用不进仓库的 keystore.properties。
// 两处都缺时保持不签名（照旧产出 app-release-unsigned.apk），本地 assembleRelease 不会被卡住。
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(envName: String, propName: String): String? =
    (System.getenv(envName) ?: keystoreProps.getProperty(propName))?.takeIf { it.isNotBlank() }

val signingStoreFile = signingValue("RELEASE_STORE_FILE", "storeFile")
    ?.let { rootProject.file(it) }
    ?.takeIf { it.isFile }
val signingStorePassword = signingValue("RELEASE_STORE_PASSWORD", "storePassword")
val signingKeyAlias = signingValue("RELEASE_KEY_ALIAS", "keyAlias")
val signingKeyPassword = signingValue("RELEASE_KEY_PASSWORD", "keyPassword")
val signingReady = listOf<Any?>(signingStoreFile, signingStorePassword, signingKeyAlias, signingKeyPassword)
    .all { it != null }

android {
    namespace = "com.worktime.app"
    compileSdk = 37

    signingConfigs {
        create("release") {
            if (signingReady) {
                storeFile = signingStoreFile
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.worktime.app"
        minSdk = 31
        targetSdk = 36
        //versionCode = 1
        //versionName = "1.0"
        val tagName = System.getenv("TAG_NAME") ?: "v1.0"
        val cleanVersion = tagName.removePrefix("v")

        versionName = cleanVersion
        versionCode = cleanVersion.replace(".", "").toIntOrNull() ?: 1

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            if (signingReady) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
