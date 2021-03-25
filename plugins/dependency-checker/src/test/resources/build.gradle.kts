group = "org.example"
version = "0.1-SNAPSHOT"

plugins {
    java
}

allprojects {
    dependencies {
        compile(kotlin(module = "stdlib-jdk8", version = "1.3.61"))
    }

    repositories {
        mavenCentral()
    }
}