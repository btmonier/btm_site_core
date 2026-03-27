# Data Overview

This document provides a summary of each JSON data file used in the personal site. All files are located in `src/commonMain/resources/content/`.

---

## Table of Contents

1. [site.json](#sitejson)
2. [about.json](#aboutjson)
3. [publications.json](#publicationsjson)
4. [presentations.json](#presentationsjson)
5. [software.json](#softwarejson)
6. [teaching.json](#teachingjson)
7. [education.json](#educationjson)
8. [skills.json](#skillsjson)
9. [scholar.json](#scholarjson)
10. [links.json](#linksjson)

---

## site.json

**Purpose:** Global site configuration and social links displayed in the navigation and footer.

**Structure:**

| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Site owner's full name |
| `title` | String | Page title text |
| `subtitle` | String | Professional tagline |
| `email` | String | Contact email address |
| `socialLinks` | Array | List of social media links |

**Social Link Object:**

| Field | Type | Description |
|-------|------|-------------|
| `platform` | String | Name of the platform (e.g., "GitHub") |
| `url` | String | Full URL to profile |
| `icon` | String | Font Awesome icon class |

**Example Entry:**
```json
{
  "platform": "GitHub",
  "url": "https://github.com/btmonier",
  "icon": "fa-brands fa-github"
}
```

---

## about.json

**Purpose:** Content for the About page including biography and professional highlights.

**Structure:**

| Field | Type | Description |
|-------|------|-------------|
| `bio` | String | Biographical text with markdown-style links (`[text](url)`) and paragraph breaks (`\\n\\n`) |
| `profileImage` | String | Path to profile image (optional) |
| `highlights` | Array | List of professional focus areas |

**Notes:**
- The `bio` field supports inline markdown links in the format `[link text](url)`
- Use `\\n\\n` for paragraph breaks within the bio text

---

## publications.json

**Purpose:** Academic publications including journal articles, preprints, and theses.

**Structure:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `items` | Array | Yes | List of publication objects |

**Publication Object:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | String | Yes | Publication title |
| `authors` | Array | Yes | List of author names |
| `journal` | String | Yes | Journal name or publication venue |
| `year` | Integer | Yes | Publication year |
| `volume` | String | No | Journal volume |
| `pages` | String | No | Page numbers |
| `doi` | String | No | Digital Object Identifier |
| `url` | String | No | Direct link to publication |

**Notes:**
- Publications are grouped by year in the UI
- "Brandon Monier" is automatically bolded in the author list
- Items without a `url` or `doi` won't display a "View Paper" link

**Example Entry:**
```json
{
  "title": "rTASSEL: An R interface to TASSEL for analyzing genomic diversity",
  "authors": ["Brandon Monier", "Terry M. Casstevens", "Peter J. Bradbury", "Edward S. Buckler"],
  "journal": "Journal of Open Source Software",
  "year": 2022,
  "volume": "7",
  "pages": "4530",
  "doi": "10.21105/joss.04530",
  "url": "https://doi.org/10.21105/joss.04530"
}
```

---

## presentations.json

**Purpose:** Conference talks and poster presentations.

**Structure:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `items` | Array | Yes | List of presentation objects |

**Presentation Object:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | String | Yes | Presentation title |
| `authors` | Array | Yes | List of presenter/author names |
| `venue` | String | Yes | Conference name and location |
| `year` | Integer | Yes | Year of presentation |
| `type` | String | Yes | Either `"talk"` or `"poster"` |

**Notes:**
- Presentations are grouped by year and displayed in separate tabs for talks and posters
- "Brandon Monier" is automatically bolded in the author list

**Example Entry:**
```json
{
  "title": "Breeder Genomics Hub: Cloudifying reproducible breeding pipelines",
  "authors": ["Brandon Monier", "Lynn C. Johnson", "Francisco J. Agosto-Perez"],
  "venue": "Zeaevolution Meeting, Ithaca, New York",
  "year": 2023,
  "type": "talk"
}
```

---

## software.json

**Purpose:** Software projects, packages, and tools.

**Structure:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `items` | Array | Yes | List of software objects |

**Software Object:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | Yes | Project name |
| `description` | String | Yes | Brief description of the software |
| `languages` | Array | No | Programming languages used (e.g., `["R", "Kotlin"]`) |
| `repoUrl` | String | No | Link to source code repository |
| `docsUrl` | String | No | Link to documentation |
| `tags` | Array | No | Topical tags for filtering |

**Supported Languages (with icons):**
- R, Python, Kotlin, Java, JavaScript, Perl, HTML, CSS, Shiny

**Notes:**
- The `languages` field accepts an array to support multi-language projects
- Language icons are displayed using Material Design Icons (MDI)
- Tags are used for the topic filter dropdown

**Example Entry:**
```json
{
  "name": "rTASSEL",
  "description": "An R-based front-end for TASSEL interactivity",
  "languages": ["R", "Java"],
  "repoUrl": "https://github.com/maize-genetics/rTASSEL",
  "docsUrl": "https://maize-genetics.github.io/rTASSEL/",
  "tags": ["TASSEL", "GWAS", "R Package", "Genomics"]
}
```

---

## teaching.json

**Purpose:** Teaching experience, workshops, and outreach activities.

**Structure:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `items` | Array | Yes | List of teaching objects |

**Teaching Object:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | String | Yes | Course or workshop title |
| `institution` | String | Yes | Host institution or location |
| `period` | String | Yes | Year or date range |
| `description` | String | Yes | Description of teaching responsibilities |
| `url` | String | No | Link to course materials (can be `null`) |
| `participants` | Integer | No | Number of participants/students |

**Notes:**
- Teaching entries are grouped by year in the UI
- The `participants` field displays with a group icon when present

**Example Entry:**
```json
{
  "title": "National Association of Plant Breeders Conference Workshop",
  "institution": "Greenville, South Carolina",
  "period": "2023",
  "description": "Taught one in-person workshop covering rTASSEL and rPHG.",
  "url": null,
  "participants": 60
}
```

---

## education.json

**Purpose:** Academic degrees and research experience.

**Structure:**

| Field | Type | Description |
|-------|------|-------------|
| `degrees` | Array | List of academic degrees |
| `experience` | Array | List of research positions |

**Degree Object:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `degree` | String | Yes | Degree type (e.g., "Ph.D.", "M.S.", "B.S.") |
| `field` | String | Yes | Field of study |
| `institution` | String | Yes | University name |
| `department` | String | No | Department name |
| `location` | String | Yes | City and state |
| `year` | Integer | Yes | Graduation year |
| `thesis` | String | No | Thesis title (for graduate degrees) |
| `thesisUrl` | String | No | Link to thesis |

**Experience Object:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `position` | String | Yes | Job title |
| `institution` | String | Yes | Institution name |
| `location` | String | Yes | City and state |
| `period` | String | Yes | Employment period |
| `advisor` | String | No | Name of advisor/supervisor |
| `highlights` | Array | No | List of accomplishments |

**Example Degree:**
```json
{
  "degree": "Ph.D.",
  "field": "Biology",
  "institution": "South Dakota State University",
  "department": "Department of Biology and Microbiology",
  "location": "Brookings, South Dakota",
  "year": 2018,
  "thesis": "Microbial communities and their impact on bioenergy crops",
  "thesisUrl": "https://openprairie.sdstate.edu/etd/2659"
}
```

---

## skills.json

**Purpose:** Technical skills, competencies, and mentoring experience.

**Structure:**

| Field | Type | Description |
|-------|------|-------------|
| `categories` | Array | List of skill categories |
| `mentoring` | Object | Mentoring and leadership information |

**Skill Category Object:**

| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Category name |
| `icon` | String | Material icon name |
| `items` | Array | List of skills in this category |

**Mentoring Object:**

| Field | Type | Description |
|-------|------|-------------|
| `summary` | String | Summary statement |
| `certifications` | Array | List of leadership certifications |
| `mentees` | Array | List of mentee objects |

**Mentee Object:**

| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Mentee name |
| `level` | String | Academic level (e.g., "Undergraduate", "Graduate") |
| `duration` | String | Duration of mentorship |
| `institution` | String | Institution name |
| `year` | String | Year(s) of mentorship |

**Example Skill Category:**
```json
{
  "name": "Programming Languages",
  "icon": "code",
  "items": ["Bash", "C++", "Java", "JavaScript", "Kotlin", "Python", "R"]
}
```

---

## scholar.json

**Purpose:** Google Scholar citation metrics and statistics.

**Structure:**

| Field | Type | Description |
|-------|------|-------------|
| `lastUpdated` | String | Date of last update in `YYYYMMDD` format |
| `citations` | Object | Citation counts |
| `hIndex` | Object | h-index values |
| `i10Index` | Object | i10-index values |
| `citationsByYear` | Array | Yearly citation data for chart |

**Metric Objects (citations, hIndex, i10Index):**

| Field | Type | Description |
|-------|------|-------------|
| `all` | Integer | All-time value |
| `since2020` | Integer | Value since 2020 |

**Citations By Year Object:**

| Field | Type | Description |
|-------|------|-------------|
| `year` | Integer | Year |
| `count` | Integer | Number of citations |

**Notes:**
- The `lastUpdated` field is displayed as a formatted timestamp on the Publications page
- The `citationsByYear` data is used to render a Chart.js bar graph

**Example:**
```json
{
  "lastUpdated": "20251127",
  "citations": { "all": 909, "since2020": 864 },
  "hIndex": { "all": 10, "since2020": 9 },
  "i10Index": { "all": 10, "since2020": 9 },
  "citationsByYear": [
    { "year": 2018, "count": 12 },
    { "year": 2019, "count": 26 }
  ]
}
```

---

## links.json

**Purpose:** External links displayed on the Links page.

**Structure:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `items` | Array | Yes | List of link objects |

**Link Object:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `label` | String | Yes | Display name |
| `url` | String | Yes | Full URL |
| `description` | String | Yes | Brief description |
| `icon` | String | Yes | Font Awesome icon class |

**Example Entry:**
```json
{
  "label": "GitHub",
  "url": "https://github.com/btmonier",
  "description": "Source code repositories and open-source projects",
  "icon": "fa-brands fa-github"
}
```

---

## Adding New Content

### To add a new publication:
1. Open `publications.json`
2. Add a new object to the `items` array
3. Ensure all required fields are populated

### To add a new software project:
1. Open `software.json`
2. Add a new object to the `items` array
3. Use appropriate tags from existing entries or add new ones

### To update citation metrics:
1. Preferred: save your Google Scholar profile as HTML to Desktop or Downloads (filename must contain `Brandon Monier` and `Google Scholar`), then run `pixi run metrics` (see `scripts/scrape_gs_metrics.R`).
2. Alternatively, edit `scholar.json` by hand: update metric values, set `lastUpdated` to `YYYYMMDD`, and extend `citationsByYear` if needed.

