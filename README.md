# Personal Website

My personal profile and CV generator built using [Kotlin Mutliplatform](https://kotlinlang.org/docs/multiplatform.html)

* **JS Target**: Static website built with [`kotlinx-html`](https://kotlinlang.org/docs/typesafe-html-dsl.html)
* **JVM Target**: CV PDF generator CLI using [`OpenPDF`](https://github.com/LibrePDF/OpenPDF)


## Prerequisites

* JDK 21+
* Gradle 8.x
* pixi

## Actions

### Generate site

```bash
# Run the development server (with hot reload)
./gradlew jsBrowserDevelopmentRun --continuous

# Build production bundle
./gradlew jsBrowserProductionWebpack
```

### Generate CV

```bash
# Generate CV with default output (btmonier_cv.pdf)
./gradlew jvmRun

# Generate CV with custom output path
./gradlew jvmRun --args="-o output/my_cv.pdf"
```

### Update Scholar metrics

Since Google _loves_ to block automated requests and provides no publically available REST API to return these crucial metrics,
An annoying intermediate step will have to be manually saving a local copy of the HTML from the browser and scraping the 
information from the saved file. I currently search for a given file ID (includes my name and the "Google Scholar" keywords) 
in either `Downloads` or `Desktop` of my local machine. Yes, this is incredibly hacky and not robust at all... Once that
has been done, I run the following commands:

```bash
# Initialize if on fresh machine
# pixi install

# Parse saved HTML and write `scholar.json`
pixi run metrics
```


