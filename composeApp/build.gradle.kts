plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kmpAndroidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidLibrary {
        namespace = "com.kapp.marslens"
        compileSdk = 36
        minSdk = 26
        // androidResources.enable = true
        withJava()
    }
    
    jvm("desktop")
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    sourceSets {
        val desktopMain by getting
        
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.material.icons.extended)
            implementation(libs.components.resources)
            
            // Lifecycle
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            
            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            
            // DateTime
            implementation(libs.kotlinx.datetime)
            
            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Okio
            implementation(libs.okio)
        }
        
        androidMain.dependencies {
            implementation(libs.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ui.tooling)
        }
        
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.zstd.jni)
            implementation(libs.bcprov)
            implementation(libs.ui.tooling.preview)
        }
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.skiko") {
            useVersion("0.9.4.2")
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.kapp.marslens.MainKt"
        // jvmArgs += listOf("-Djava.library.path=${project.projectDir}/libs")
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "MarsLens"
            packageVersion = "1.0.0"
        }
    }
}

sqldelight {
    databases {
        create("PunchDatabase") {
            packageName.set("com.kapp.marslens.data.db")
        }
    }
}

compose.resources {
    packageOfResClass = "com.kapp.marslens"
}
