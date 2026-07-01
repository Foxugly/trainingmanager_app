import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.openapiGenerator)
}

kotlin {
    android {
        namespace = "com.foxugly.trainingmanager_app.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
        androidResources {
            enable = true
        }
        // Enable JVM-host unit tests so commonTest runs on the desktop (incl.
        // Windows, no Mac/Xcode needed) via :composeApp:testAndroidHostTest.
        // isReturnDefaultValues: android.util.Log (used by AppLogger) is not on
        // the JVM test classpath — return defaults instead of throwing "not mocked".
        withHostTestBuilder {}.configure {
            isReturnDefaultValues = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.core)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.navigation.compose)
            implementation(libs.reorderable)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.security.crypto)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
            // AppLogger.android forwards handled errors to Sentry (safe no-op
            // until Sentry is initialized in the app's Application).
            implementation(libs.sentry.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

dependencies {
    add("androidRuntimeClasspath", libs.compose.uiTooling)
}

// ---------------------------------------------------------------------------
// OpenAPI models-only codegen (feasibility proof — feat/codegen-retry).
// Emits ONLY @Serializable kotlinx models into build/generated/openapi, in a
// SEPARATE package (…api.generated) that coexists with the hand-written DTOs.
// No Ktor runtime / apis / infrastructure are generated (globalProperties).
// ---------------------------------------------------------------------------
val openApiOutDir = layout.buildDirectory.dir("generated/openapi")

openApiGenerate {
    generatorName.set("kotlin")
    library.set("multiplatform") // selects kotlinx.serialization @Serializable models
    inputSpec.set(layout.projectDirectory.dir("../openapi").file("Training_Manager_API.yaml").asFile.path)
    outputDir.set(openApiOutDir.get().asFile.path)
    packageName.set("com.foxugly.trainingmanager_app.api.generated")
    // Models only — do not emit apis, infrastructure, or supporting/runtime files.
    globalProperties.set(
        mapOf(
            "models" to "",
            "modelDocs" to "false",
            "modelTests" to "false",
            "apis" to "false",
            "apiDocs" to "false",
            "apiTests" to "false",
            "supportingFiles" to "false",
        ),
    )
    configOptions.set(
        mapOf(
            // NOTE: do NOT set serializationLibrary here. library=multiplatform
            // already implies kotlinx_serialization; setting it again makes the
            // generator emit a duplicate `@Serializable@Serializable` on every
            // model (a Kotlin compile error). Letting it default fixes that.
            //
            // dateLibrary=string avoids kotlin.time.Instant (no KMP serializer on
            // this stack) — date/time fields become plain String.
            "dateLibrary" to "string",
            "enumPropertyNaming" to "UPPERCASE",
            "sourceFolder" to "src/commonMain/kotlin",
        ),
    )
    // Type remaps for KMP commonMain compatibility:
    //  - "decimal" (string/format:decimal) defaults to java.math.BigDecimal,
    //    which is JVM-only. The backend serialises these as JSON strings with a
    //    numeric pattern, so kotlin.String round-trips losslessly.
    //  - "AnyType" (free-form `{}` / untyped object & array items) defaults to a
    //    bare kotlin.Any, which is not @Serializable. Map to JsonElement, which
    //    IS @Serializable and represents arbitrary JSON.
    typeMappings.set(
        mapOf(
            "decimal" to "kotlin.String",
            "AnyType" to "kotlinx.serialization.json.JsonElement",
        ),
    )
    importMappings.set(
        mapOf(
            "kotlinx.serialization.json.JsonElement" to "kotlinx.serialization.json.JsonElement",
        ),
    )
}

// The schema declares a `NullEnum` (`enum: [null]`, drf-spectacular's marker for
// nullable enum oneOf branches). openapi-generator 7.21 emits it as a syntactically
// invalid empty Kotlin enum, but NOTHING references it (the nullable enum fields
// resolve to the real enum). Delete the orphan after generation so it can compile.
val pruneOpenApiOrphans = tasks.register<Delete>("pruneOpenApiOrphans") {
    delete(openApiOutDir.map { it.file("src/commonMain/kotlin/com/foxugly/trainingmanager_app/api/generated/models/NullEnum.kt") })
}
tasks.named("openApiGenerate") { finalizedBy(pruneOpenApiOrphans) }

// Register generated models as a commonMain source dir and ensure generation
// runs before any Kotlin compilation. dependsOn on the task name keeps this
// configuration-cache compatible (no Project access at execution time).
kotlin.sourceSets.named("commonMain") {
    kotlin.srcDir(openApiOutDir.map { it.dir("src/commonMain/kotlin") })
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(tasks.named("openApiGenerate"))
    dependsOn(pruneOpenApiOrphans)
}
