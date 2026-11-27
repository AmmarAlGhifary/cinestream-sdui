plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.dagger.hilt)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

// The backend address is set in gradle.properties and read only here. -PBASE_URL overrides it
// for every build type.
val baseUrlOverride = findProperty("BASE_URL") as String?
val debugBaseUrl = baseUrlOverride ?: property("DEBUG_BASE_URL") as String
val releaseBaseUrl = baseUrlOverride ?: property("RELEASE_BASE_URL") as String

android {
    namespace = "com.ammar.cinestream.core.network"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$debugBaseUrl\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "BASE_URL", "\"$releaseBaseUrl\"")
        }
    }
    testOptions {
        // android.util.Log is a no-op in plain JVM tests.
        unitTests.isReturnDefaultValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Checked only when a release variant is going to be built, so debug builds may use http.
gradle.taskGraph.whenReady {
    val buildsRelease = allTasks.any { it.path == "${project.path}:preReleaseBuild" }
    if (buildsRelease && !releaseBaseUrl.startsWith("https://")) {
        throw GradleException(
            "Release builds need an https BASE_URL, but it is \"$releaseBaseUrl\". " +
                "Pass -PBASE_URL=https://... or fix RELEASE_BASE_URL in gradle.properties."
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization.converter)
    implementation(libs.okhttp.logging)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}
