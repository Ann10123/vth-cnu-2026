dependencies {
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")
    annotationProcessor("io.micronaut.validation:micronaut-validation-processor")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("io.micronaut.flyway:micronaut-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.micronaut.sql:micronaut-jdbi")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut.security:micronaut-security-annotations:5.0.0")
    implementation("io.projectreactor:reactor-core:3.6.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    runtimeOnly("io.micronaut.sql:micronaut-jdbc-hikari")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("ch.qos.logback:logback-classic")

    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

application {
    mainClass.set("ua.cdu.edu.fotius.fspm.valorant.roster.Application")
}
