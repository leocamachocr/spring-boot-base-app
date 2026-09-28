plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    checkstyle
}

group = "dev.leocamacho"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.liquibase:liquibase-core")
    implementation("io.jsonwebtoken:jjwt:0.13.0")

    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.springframework.boot:spring-boot-h2console")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.tngtech.archunit:archunit:1.5.1")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.1")
    testImplementation("io.rest-assured:rest-assured:6.0.1")
    testImplementation("io.rest-assured:json-path:6.0.1")
    testImplementation("io.cucumber:cucumber-java:7.34.9")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.34.9")
    testImplementation("io.cucumber:cucumber-picocontainer:7.34.9")
    testImplementation("org.junit.platform:junit-platform-suite")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform {
        excludeEngines("junit-platform-suite", "cucumber")
    }
}

// Black-box E2E tests against an already running app; not part of `build`.
// Target with -Dhost=... -Dport=... (defaults: http://localhost:8080).
tasks.register<Test>("e2eTest") {
    description = "Runs Cucumber E2E tests against a running instance of the app."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeEngines("junit-platform-suite")
    }
    listOf("host", "port").forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
    outputs.upToDateWhen { false }
    shouldRunAfter(tasks.test)
}
