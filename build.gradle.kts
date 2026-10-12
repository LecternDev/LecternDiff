import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "tlm.dev.lectern.diff.Main"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jackson.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal"))
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        systemProperty("lectern.diff.runtimeClasspath", sourceSets.main.get().runtimeClasspath.asPath)
    }
    inputs.files(fileTree("src/main/java"), fileTree("src/test/java"))
        .withPropertyName("sourceRuleInputs")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("../LecternWorkbench/generated") { include("*/reports/registries.json", "*/reports/blocks.json", "*/reports/packets.json", "*/reports/commands.json") })
        .withPropertyName("workbenchReports")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("../LecternWorkbench/normalized") { include("*/reports/registries.json", "*/reports/blocks.json", "*/reports/packets.json", "*/reports/commands.json") })
        .withPropertyName("workbenchNormalizedReports")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    testLogging {
        events("failed", "skipped")
        exceptionFormat = TestExceptionFormat.FULL
    }
}
