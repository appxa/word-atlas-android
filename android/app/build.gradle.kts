plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.appxa.wordatlas"
    compileSdk = 35
    defaultConfig { applicationId = "com.appxa.wordatlas"; minSdk = 26; targetSdk = 35; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"; versionCode = 1; versionName = "1.0.0" }
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
