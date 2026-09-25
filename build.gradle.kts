plugins {
  id("org.jetbrains.kotlin.jvm") version "2.4.20"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.apache.poi:poi-ooxml:5.5.1")
}

kotlin {
  jvmToolchain(21)
  compilerOptions.freeCompilerArgs.addAll(
    "-Xcollection-literals",
    "-Xcontext-parameters",
    "-Xcompanion-blocks-and-extensions",
  )
}

val snippetsCheck by tasks.registering(Exec::class) {
  commandLine("npx", "slidev-kotlin-snippets", "--check")
}

tasks.check { dependsOn(snippetsCheck) }
