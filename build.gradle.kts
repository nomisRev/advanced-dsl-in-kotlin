plugins {
  id("org.jetbrains.kotlin.jvm") version "2.5.0-Beta1"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(project(":excel-dsl"))
  implementation("org.apache.poi:poi-ooxml:5.5.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
}

kotlin {
  jvmToolchain(21)
  compilerOptions.freeCompilerArgs.addAll(
    "-Xcollection-literals",
    "-Xcontext-parameters",
    "-Xcompanion-blocks-and-extensions",
    "-Xcontext-sensitive-resolution",
  )
}

val snippetsCheck by tasks.registering(Exec::class) {
  commandLine("npx", "slidev-kotlin-snippets", "--check")
}

val extrasSnippetsCheck by tasks.registering(Exec::class) {
  commandLine("npx", "slidev-kotlin-snippets", "extras.md", "--check")
}

val recapSnippetsCheck by tasks.registering(Exec::class) {
  commandLine("npx", "slidev-kotlin-snippets", "lesson-0.md", "--check")
}

tasks.check { dependsOn(snippetsCheck, extrasSnippetsCheck, recapSnippetsCheck) }
