plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.appxa.wordatlas"
    compileSdk = 35
    defaultConfig { applicationId = "com.appxa.wordatlas"; minSdk = 26; targetSdk = 35; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"; versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1); versionName = "1.0.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}" }
    signingConfigs {
        create("production") {
            val keyPath = System.getenv("ANDROID_KEYSTORE_PATH")
            if (keyPath != null) {
                storeFile = file(keyPath)
                storePassword = System.getenv("ANDROID_KEY_PASSWORD")
                keyAlias = "wordatlas"
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("production")
            isMinifyEnabled = false
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    sourceSets["main"].assets.srcDir("../../dist")
}
dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
