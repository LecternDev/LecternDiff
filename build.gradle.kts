import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal"))
}

tasks.test {
    useJUnitPlatform()
    inputs.files(fileTree("src/main/java"), fileTree("src/test/java"), "ARCHITECTURE_RULES.md")
        .withPropertyName("sourceRuleInputs")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    testLogging {
        events("failed", "skipped")
        exceptionFormat = TestExceptionFormat.FULL
    }
}
