plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.appxa.wordatlas"
    compileSdk = 35
    defaultConfig { applicationId = "com.appxa.wordatlas"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    sourceSets["main"].assets.srcDir("../../dist")
}
dependencies { implementation("androidx.webkit:webkit:1.12.1") }
