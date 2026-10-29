plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
android {
    namespace = "com.example.wellbeing"
    compileSdk = 36
    defaultConfig {
        applicationId = providers.gradleProperty("wellbeing.applicationId").orElse("com.example.wellbeing.offline").get()
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "1.1.1-vitalday-rc2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    bundle { language { enableSplit = false } } // Both supported languages work offline after installation.
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    val signingNames = listOf("WELLBEING_KEYSTORE", "WELLBEING_STORE_PASSWORD", "WELLBEING_KEY_ALIAS", "WELLBEING_KEY_PASSWORD")
    val signingValues = signingNames.map { providers.environmentVariable(it).orNull }
    require(signingValues.all { it == null } || signingValues.all { !it.isNullOrBlank() }) { "Provide all four release signing environment variables, or none for an unsigned build." }
    signingConfigs {
        if (signingValues.all { !it.isNullOrBlank() }) create("production") {
            storeFile = file(signingValues[0]!!)
            storePassword = signingValues[1]
            keyAlias = signingValues[2]
            keyPassword = signingValues[3]
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("production")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    androidResources { localeFilters += listOf("en", "ta") }
    lint {
        abortOnError = true
        warningsAsErrors = true
        enable += setOf("MissingTranslation", "ExtraTranslation")
        // Remote version discovery must not make a pinned release build fail
        // merely because a newer tool or library was published. Keep these
        // upgrade notices visible; correctness and translation checks still fail.
        informational += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }
    testOptions { animationsDisabled = true }
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.room:room-testing:2.7.1")
    androidTestImplementation("androidx.work:work-testing:2.10.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
