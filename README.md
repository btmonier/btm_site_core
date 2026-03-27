# Personal Website

My personal profile and CV generator built using [Kotlin Mutliplatform](https://kotlinlang.org/docs/multiplatform.html)

* **JS Target**: Static website built with [`kotlinx-html`](https://kotlinlang.org/docs/typesafe-html-dsl.html)
* **JVM Target**: CV PDF generator CLI using [`OpenPDF`](https://github.com/LibrePDF/OpenPDF)


## Prerequisites

* JDK 21+
* Gradle 8.x
* pixi

## Pixi Tasks

All tasks are managed through [pixi](https://pixi.sh). Run `pixi install` on a fresh machine to set up dependencies.

### Build and serve the website

```bash
pixi run website
```

### Generate CV

```bash
# Default output: btmonier_cv.pdf
pixi run cv

# Custom output path
pixi run cv output/my_cv.pdf
```

### Update Scholar metrics

Since Google _loves_ to block automated requests and provides no publically available REST API to return these crucial metrics,
an annoying intermediate step will have to be manually saving a local copy of the HTML from the browser and scraping the 
information from the saved file. I currently search for a given file ID (includes my name and the "Google Scholar" keywords) 
in either `Downloads` or `Desktop` of my local machine. Yes, this is incredibly hacky and not robust at all... Once that
has been done, I run:

```bash
pixi run metrics
```


