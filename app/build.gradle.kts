import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.google.dagger.hilt)
}

// Release signing. The values come from one properties file outside the repository (see "Release
// signing" in README.md). Passwords must never be logged or put into an error message.
fun fileFromPath(path: String, relativeTo: File): File {
    val expanded =
        if (path.startsWith("~/")) System.getProperty("user.home") + path.removePrefix("~") else path
    return relativeTo.resolve(expanded)
}

val signingPropertiesFile = fileFromPath(
    findProperty("CINESTREAM_SIGNING_PROPERTIES") as String?
        ?: "~/Documents/AgentStuff/cinestream-release-signing.properties",
    rootDir
)
val signingValues = Properties()
val signingFileReadable = runCatching {
    if (signingPropertiesFile.isFile) signingPropertiesFile.reader().use { signingValues.load(it) }
}.isSuccess
val missingSigningKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .filter { signingValues.getProperty(it).isNullOrBlank() }
val releaseKeystore = signingValues.getProperty("storeFile")?.trim()
    ?.let { fileFromPath(it, signingPropertiesFile.parentFile) }

/** Why an existing signing file cannot be used. Null when it can, or when there is none. */
val signingProblem: String? = when {
    !signingPropertiesFile.exists() -> null
    !signingPropertiesFile.isFile || !signingFileReadable -> "cannot be read as a properties file"
    missingSigningKeys.isNotEmpty() -> "has no value for ${missingSigningKeys.joinToString()}"
    releaseKeystore?.isFile != true -> "names a keystore that does not exist: $releaseKeystore"
    else -> null
}
val canSignRelease = signingPropertiesFile.isFile && signingProblem == null

android {
    namespace = "com.ammar.cinestream"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.ammar.cinestream"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = signingValues.getProperty("storePassword")
                keyAlias = signingValues.getProperty("keyAlias").trim()
                keyPassword = signingValues.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    testOptions {
        // Robolectric Compose tests need the merged manifest and resources.
        unitTests.isIncludeAndroidResources = true
    }
}

// Reported only when a release variant is going to be built, so debug builds never depend on it.
gradle.taskGraph.whenReady {
    if (allTasks.any { it.path == "${project.path}:preReleaseBuild" }) {
        if (signingProblem != null) {
            throw GradleException("Release signing: $signingPropertiesFile $signingProblem")
        }
        if (!canSignRelease) {
            logger.lifecycle(
                "Release signing: $signingPropertiesFile not found, so the release build is unsigned."
            )
        }
    }
}

dependencies {
    //Project modules
    implementation(project(":core:designsystems"))
    implementation(project(":core:network"))
    implementation(project(":core:sdui"))
    implementation(project(":feature:home"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:listMovie"))
    implementation(project(":feature:search"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}
