import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.desktop)
}

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }
    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(libs.coroutines.swing)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "cg.epilote.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName    = "E-PILOTE CONGO"
            packageVersion = "1.0.0"
            description    = "Plateforme de gestion des écoles — Congo Brazzaville"
            vendor         = "Ministère de l'Éducation Nationale"
            windows {
                menuGroup      = "E-PILOTE"
                upgradeUuid    = "3f8a2b10-4e7d-11ef-9a2c-0800200c9a66"
                shortcut       = true
                dirChooser     = true
            }
        }
    }
}
