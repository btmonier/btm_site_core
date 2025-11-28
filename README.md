# Personal Website

My personal profile and CV generator built using Kotlin Mutliplatform

* **JS Target**: Static website built with [`kotlinx-html`](https://kotlinlang.org/docs/typesafe-html-dsl.html)
* **JVM Target**: CV PDF generator CLI using [OpenPDF](https://github.com/LibrePDF/OpenPDF)


## Prerequisites

* JDK 21+
* Gradle 8.x

## Generate Site

```bash
# Run the development server (with hot reload)
./gradlew jsBrowserDevelopmentRun --continuous

# Build production bundle
./gradlew jsBrowserProductionWebpack
```

## Generate CV

```bash
# Generate CV with default output (btmonier_cv.pdf)
./gradlew jvmRun

# Generate CV with custom output path
./gradlew jvmRun --args="-o output/my_cv.pdf"
```


