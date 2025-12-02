plugins {
    java
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.example.angella"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.liquibase:liquibase-core")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.webjars:bootstrap:5.3.2")
    implementation("org.webjars:webjars-locator:0.50")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.3.0")
    implementation("org.webjars:font-awesome:6.4.0")
    implementation("org.webjars:jquery:3.6.0")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6:3.1.2.RELEASE")
    implementation("org.webjars:select2:4.0.13")
    compileOnly("org.projectlombok:lombok")
    implementation("org.webjars:sockjs-client:1.5.1")
    implementation("org.webjars:stomp-websocket:2.3.4")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.projectlombok:lombok")
    compileOnly("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("com.itextpdf:layout:7.2.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
    testImplementation("org.testcontainers:postgresql:1.19.7")

}

tasks.withType<Test> {
    useJUnitPlatform()
}

