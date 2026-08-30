plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "faang.school"
version = "1.0"

val javaVersion = 25
val springCloudVersion = "2025.1.3"
val testcontainersVersion = "2.0.5"
val mapstructVersion = "1.6.3"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

repositories {
    mavenCentral()
}

val mockitoAgent = configurations.create("mockitoAgent")

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
        mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
    }
}

dependencies {
    /**
     * Spring boot starters
     */
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.retry:spring-retry")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")


    // Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")

    /**
     * Database
     */
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")

    /**
     * Utils & Logging
     */
    implementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    implementation("com.fasterxml.jackson.core:jackson-databind")

    /**
     * Tests
     */
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")

    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    mockitoAgent("org.mockito:mockito-core") { isTransitive = false }
}

jacoco {
    toolVersion = "0.8.15"
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-Xshare:off", "-javaagent:${mockitoAgent.asPath}")
}

// Unit tests only — integration tests are excluded by tag and run via `integrationTest`.
tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    finalizedBy(tasks.named("jacocoTestReport"))
}

// Integration tests (Testcontainers) — run explicitly, not part of the unit gate.
tasks.register<Test>("integrationTest") {
    description = "Runs integration tests (tagged 'integration')."
    group = "verification"
    useJUnitPlatform {
        includeTags("integration")
    }
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco"))
    }
}

// Coverage gate for application logic only. Thresholds start at the measured unit-test
// baseline (2026-08-30) and ramp up non-decreasingly (DEVPLAN_UNITSTESTS-RULES.md §3).
// Documented exclusions (narrow, per rules): model.* (Lombok persistence entities with no
// custom behavior beyond AchievementProgress.increment), dto.* (record-only DTO),
// repository.* (Spring Data interfaces), client.UserServiceClient (Feign interface),
// client.FeignConfig (bean wiring), AchievementServiceApp (bootstrap).
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            element = "CLASS"
            includes = listOf(
                "faang.school.achievement.service.*",
                "faang.school.achievement.validator.*",
                "faang.school.achievement.controller.*",
                "faang.school.achievement.config.context.*",
                "faang.school.achievement.client.FeignUserInterceptor"
            )
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.75".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.bootJar {
    archiveFileName.set("service.jar")
}
