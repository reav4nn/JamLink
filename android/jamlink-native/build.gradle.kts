plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jamlink.nativelib"
    compileSdk = rootProject.ext["compileSdkVersion"] as Int

    defaultConfig {
        minSdk = rootProject.ext["minSdkVersion"] as Int

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17", "-fexceptions")
                arguments("-DANDROID_STL=c++_shared")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("../app/build/generated/source/codegen/java")
        }
    }
}

afterEvaluate {
    tasks.configureEach {
        if ((name.contains("compile") && (name.contains("Kotlin") || name.contains("Java"))) || name.contains("externalNativeBuild")) {
            dependsOn(":app:generateCodegenArtifactsFromSchema")
        }
    }
}

dependencies {
    implementation("com.facebook.react:react-android")
    // Oboe will be added in Phase 4
    testImplementation("junit:junit:4.13.2")
}
