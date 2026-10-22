import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
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

// 版本号来自 tag：v<major>.<minor>[.<patch>]，"-" 之后的内容只进 versionName。
// versionCode 用固定权重编码，保证 v1.100 < v2.0；旧编码（1、10、110…）都小于第一条新编码。
val tagName = providers.environmentVariable("TAG_NAME").orElse("v1.0").get().trim()
val cleanVersion = tagName.removePrefix("v").removePrefix("V")
val versionParts = cleanVersion.substringBefore('-').split('.').map { it.toIntOrNull() }
require(versionParts.size in 2..3 && versionParts.all { it != null }) {
    "TAG_NAME 需形如 v1.2 或 v1.2.3，当前为：$tagName"
}
val versionMajor = versionParts[0]!!
val versionMinor = versionParts[1]!!
val versionPatch = versionParts.getOrNull(2) ?: 0
require(versionMajor in 0..2147 && versionMinor in 0..999 && versionPatch in 0..999) {
    "版本号超出可编码范围（minor/patch 最大 999）：$tagName"
}
val derivedVersionCode = versionMajor * 1_000_000 + versionMinor * 1_000 + versionPatch
// 逃生口：需要重发已被占用的 versionCode 时，用环境变量显式指定。
val overrideVersionCode = providers.environmentVariable("VERSION_CODE").getOrNull()?.toIntOrNull()

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
        versionName = cleanVersion
        versionCode = overrideVersionCode ?: derivedVersionCode

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

// Room schema 导出到 app/schemas，后续写 Migration 时用来对比。
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
