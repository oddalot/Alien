plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish") version "0.25.3"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.10-1.0.13")
    implementation("com.squareup:kotlinpoet:1.11.0")
    implementation("com.squareup:kotlinpoet-ksp:1.11.0")
    testImplementation(libs.junit.junit)
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.5.0")

}

group = "net.williamott"
version = "0.1"

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.S01)

    coordinates("net.williamott", "alien", "0.1")

    pom {
        name.set("Alien Dependency Injection")
        description.set("A simple dependency injection library for Kotlin.")
        inceptionYear.set("2023")
        url.set("https://github.com/oddalot/Alien")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("oddalot")
                name.set("William Ott")
                url.set("https://github.com/oddalot")
            }
        }
        scm {
            url.set("https://github.com/oddalot/Alien/")
            connection.set("scm:git:git://github.com/oddalot/Alien.git")
            developerConnection.set("scm:git:ssh://git@github.com/oddalot/Alien.git")
        }
    }
}