plugins {
    kotlin("jvm") version "2.1.10"
    application
}

group = "org.example"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    
    // Add kotlinx-coroutines dependency for concurrent processing
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(23)
}

application {
    mainClass.set("MainKt")
}

tasks.register<JavaExec>("runBenchmark") {
    group = "application"
    description = "Run performance comparison benchmark"
    
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("benchmarks.PerformanceComparison")
    
    args = listOf("${projectDir}/Exposed", "${projectDir}/benchmark-results.txt")
}
