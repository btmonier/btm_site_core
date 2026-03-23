#!/usr/bin/env Rscript
## Google Scholar metrics from a locally saved profile HTML (browser Save As)
## Writes JSON for the personal site

# ---- LOAD PACKAGES (suppress startup messages) ----
suppressPackageStartupMessages({
    library(jsonlite)
    library(dplyr)
    library(rvest)
})

# ---- LOGGING ----

LOG_LEVELS <- c(DEBUG = 1, INFO = 2, WARN = 3, ERROR = 4)
LOG_LEVEL  <- LOG_LEVELS["INFO"]

log_msg <- function(level, msg, ...) {
    if (LOG_LEVELS[level] >= LOG_LEVEL) {
        timestamp <- format(Sys.time(), "[%Y-%m-%d %H:%M:%S]")
        formatted_msg <- sprintf(msg, ...)
        cat(sprintf("%s [%s] %s\n", timestamp, level, formatted_msg))
    }
}

log_debug <- function(msg, ...) log_msg("DEBUG", msg, ...)
log_info  <- function(msg, ...) log_msg("INFO", msg, ...)
log_warn  <- function(msg, ...) log_msg("WARN", msg, ...)
log_error <- function(msg, ...) log_msg("ERROR", msg, ...)

# ---- HELPERS ----

#' Find newest matching saved Google Scholar HTML under Desktop / Downloads.
#' Filename must contain both name phrase and "Google Scholar" (case-insensitive).
find_saved_scholar_html <- function(
        search_dirs = path.expand(c("~/Desktop", "~/Downloads")),
        name_kw = "Brandon Monier",
        scholar_kw = "Google Scholar") {
    search_dirs <- search_dirs[dir.exists(search_dirs)]
    if (length(search_dirs) == 0) {
        log_error("Neither ~/Desktop nor ~/Downloads exists or is reachable")
        return(NA_character_)
    }

    html_files <- unlist(lapply(search_dirs, function(d) {
        list.files(d, pattern = "\\.html?$", full.names = TRUE, ignore.case = TRUE)
    }), use.names = FALSE)

    html_files <- html_files[file.exists(html_files)]
    html_files <- html_files[!file.info(html_files)$isdir]
    if (length(html_files) == 0) {
        log_error("No .html files in Desktop or Downloads")
        return(NA_character_)
    }

    name_kw_l <- tolower(name_kw)
    scholar_kw_l <- tolower(scholar_kw)
    hits <- html_files[vapply(html_files, function(f) {
        b <- tolower(basename(f))
        grepl(name_kw_l, b, fixed = TRUE) && grepl(scholar_kw_l, b, fixed = TRUE)
    }, logical(1))]

    if (length(hits) == 0) {
        log_error(
            "No saved Scholar HTML found. Save your profile (e.g. %s) under Desktop or Downloads.",
            paste0("_", name_kw, "_ - _", scholar_kw, "_.html")
        )
        return(NA_character_)
    }

    if (length(hits) > 1) {
        mt <- file.mtime(hits)
        chosen <- hits[which.max(mt)]
        log_info("Multiple matches (%d); using newest by mtime: %s", length(hits), chosen)
        return(chosen)
    }
    hits[[1]]
}

#' Parse sidebar stats: all-time and "since" citations, h-index, i10-index
parse_profile_stats <- function(page) {
    stats_cells <- page |>
        html_nodes("#gsc_rsb_st td.gsc_rsb_std") |>
        html_text()

    if (length(stats_cells) < 6) {
        log_warn("Expected 6 stat cells in #gsc_rsb_st, got %d", length(stats_cells))
        return(list(
            total_cites = NA_integer_, h_index = NA_integer_, i10_index = NA_integer_,
            cites_since = NA_integer_, h_since = NA_integer_, i10_since = NA_integer_
        ))
    }

    list(
        total_cites = as.integer(stats_cells[[1]]),
        cites_since = as.integer(stats_cells[[2]]),
        h_index     = as.integer(stats_cells[[3]]),
        h_since     = as.integer(stats_cells[[4]]),
        i10_index   = as.integer(stats_cells[[5]]),
        i10_since   = as.integer(stats_cells[[6]])
    )
}

#' Rolling-window start year from the "Since YYYY" table header (Scholar uses a 5-year window).
parse_since_year <- function(page) {
    th_texts <- page |>
        html_nodes("th.gsc_rsb_sth") |>
        html_text(trim = TRUE)
    since_label <- th_texts[grepl("^Since ", th_texts)]
    if (length(since_label) == 0) {
        y <- as.integer(format(Sys.Date(), "%Y")) - 5L
        log_warn("No 'Since YYYY' header found; using current year - 5 = %d", y)
        return(y)
    }
    m <- regmatches(since_label[[1]], regexec("^Since (\\d{4})$", since_label[[1]]))
    if (length(m[[1]]) >= 2) {
        return(as.integer(m[[1]][2]))
    }
    y <- as.integer(format(Sys.Date(), "%Y")) - 5L
    log_warn("Could not parse year from '%s'; using current year - 5 = %d", since_label[[1]], y)
    y
}

#' Citation counts by year from the bar chart (same logic as scholar::get_citation_history)
parse_citation_history <- function(page) {
    years <- page |>
        html_nodes(xpath = "//*/span[@class='gsc_g_t']") |>
        html_text() |>
        as.numeric()
    vals <- page |>
        html_nodes(xpath = "//*/span[@class='gsc_g_al']") |>
        html_text() |>
        as.numeric()

    if (length(years) == 0) {
        return(data.frame(year = integer(), cites = integer()))
    }

    if (length(years) > length(vals)) {
        style_tags <- page |>
            html_nodes(css = ".gsc_g_a") |>
            html_attr("style")
        zm <- regmatches(style_tags, regexec("z-index:([0-9]+)", style_tags))
        zindices <- vapply(zm, function(x) {
            if (length(x) >= 2) as.integer(x[[2]]) else NA_integer_
        }, integer(1))
        if (length(zindices) == length(vals) && all(!is.na(zindices))) {
            allvals <- integer(length(years))
            allvals[zindices] <- vals
            vals <- rev(allvals)
        } else {
            log_warn("Could not align citation bars with years; using partial data")
        }
    }

    data.frame(year = as.integer(years), cites = as.integer(vals))
}

# ---- CONFIG ----
json_out <- "src/jsMain/resources/content/scholar.json"

log_info("Starting Google Scholar metrics extraction from saved HTML")

html_path <- find_saved_scholar_html()
if (is.na(html_path) || !nzchar(html_path)) {
    quit(status = 1)
}

log_info("Using saved page: %s", html_path)

page <- tryCatch(
    read_html(html_path),
    error = function(e) {
        log_error("Failed to read HTML: %s", conditionMessage(e))
        NULL
    }
)
if (is.null(page)) {
    quit(status = 1)
}

# ---- PARSE ----

stats <- parse_profile_stats(page)
since_year <- parse_since_year(page)
log_info("Scholar 'since' window start year: %d", since_year)
log_info(
    "All-time metrics: citations=%s, h-index=%s, i10-index=%s",
    stats$total_cites, stats$h_index, stats$i10_index
)
log_info(
    "Since-window metrics: citations=%s, h-index=%s, i10-index=%s",
    stats$cites_since, stats$h_since, stats$i10_since
)

if (any(is.na(unlist(stats)))) {
    log_warn("Some metrics are NA — saved page may be incomplete or layout changed")
}

hist <- parse_citation_history(page)
log_info("Citation history: %d years of data", nrow(hist))

citations_by_year <- hist |>
    arrange(year) |>
    transmute(year = year, count = cites)

if (nrow(citations_by_year) > 0) {
    log_info(
        "Citations by year: %d to %d",
        min(citations_by_year$year), max(citations_by_year$year)
    )
} else {
    log_warn("No citations-by-year chart found in saved HTML")
}

# ---- BUILD OUTPUT ----

last_updated <- format(Sys.Date(), "%Y%m%d")
log_info("Last updated timestamp: %s", last_updated)

# sinceYear + "since" values mirror Scholar's rolling window column (header "Since YYYY")
metrics_list <- list(
    lastUpdated = last_updated,
    sinceYear   = since_year,
    citations   = list(
        all   = stats$total_cites,
        since = stats$cites_since
    ),
    hIndex = list(
        all   = stats$h_index,
        since = stats$h_since
    ),
    i10Index = list(
        all   = stats$i10_index,
        since = stats$i10_since
    ),
    citationsByYear = lapply(seq_len(nrow(citations_by_year)), function(i) {
        list(
            year  = citations_by_year$year[i],
            count = citations_by_year$count[i]
        )
    })
)

# ---- WRITE JSON ----

json_text <- toJSON(metrics_list, pretty = TRUE, auto_unbox = TRUE)

json_text <- gsub(
    '\\{\n\\s+"year":\\s*(\\d+),\n\\s+"count":\\s*(\\d+)\n\\s+\\}',
    '{ "year": \\1, "count": \\2 }',
    json_text
)

log_info("Writing JSON to: %s", json_out)
writeLines(json_text, json_out)

log_info("Done! Metrics saved successfully")
log_debug("JSON output:\n%s", json_text)
