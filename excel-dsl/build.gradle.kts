plugins {
  id("org.jetbrains.kotlin.jvm")
}

repositories {
  mavenCentral()
}

dependencies {
  api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
  implementation("org.apache.poi:poi-ooxml:5.5.1")

  testImplementation(kotlin("test"))
  testImplementation(kotlin("compiler-embeddable"))
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

tasks.test {
  useJUnitPlatform()
  systemProperty("dsl.compile.classpath", sourceSets.main.get().runtimeClasspath.asPath)
}
