#!/usr/bin/env Rscript
## Google Scholar Metrics Scraper
## Fetches citation metrics and outputs to JSON for the personal site

# ---- LOAD PACKAGES (suppress startup messages) ----
suppressPackageStartupMessages({
    library(scholar)
    library(jsonlite)
    library(dplyr)
    library(rvest)
})

# ---- LOGGING ----

# Log levels
LOG_LEVELS <- c(DEBUG = 1, INFO = 2, WARN = 3, ERROR = 4)
LOG_LEVEL  <- LOG_LEVELS["INFO"]  # Set minimum log level

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

#' Scrape "Since [year]" metrics directly from Google Scholar profile page
#' The scholar package doesn't provide these, so we scrape them ourselves
scrape_since_metrics <- function(scholar_id) {
    url <- paste0("https://scholar.google.com/citations?user=", scholar_id, "&hl=en")
    log_debug("Scraping URL: %s", url)
    
    tryCatch({
        page <- read_html(url)
        
        # The stats table has cells in order:
        # Row 1: "Citations", all-time value, since-year value
        # Row 2: "h-index", all-time value, since-year value  
        # Row 3: "i10-index", all-time value, since-year value
        stats_cells <- page |> 
            html_nodes("#gsc_rsb_st td.gsc_rsb_std") |>
            html_text()
        
        if (length(stats_cells) >= 6) {
            result <- list(
                cites_since = as.integer(stats_cells[2]),
                h_since     = as.integer(stats_cells[4]),
                i10_since   = as.integer(stats_cells[6])
            )
            log_debug("Parsed 'since' metrics: cites=%d, h=%d, i10=%d", 
                      result$cites_since, result$h_since, result$i10_since)
            return(result)
        } else {
            log_warn("Could not parse stats table - expected 6 cells, got %d", length(stats_cells))
            return(list(cites_since = NA, h_since = NA, i10_since = NA))
        }
    }, error = function(e) {
        log_error("Failed to scrape Google Scholar: %s", conditionMessage(e))
        return(list(cites_since = NA, h_since = NA, i10_since = NA))
    })
}

# ---- CONFIG ----
scholar_id <- "buYGhlYAAAAJ"
json_out   <- "src/jsMain/resources/content/scholar.json"

log_info("Starting Google Scholar metrics scrape")
log_info("Scholar ID: %s", scholar_id)
log_info("Output file: %s", json_out)

# ---- FETCH DATA FROM GOOGLE SCHOLAR ----

log_info("Fetching profile data...")
prof <- get_profile(scholar_id)
log_info("Profile loaded: %s (%s)", prof$name, prof$affiliation)

log_info("Fetching citation history...")
hist <- get_citation_history(scholar_id)
log_info("Citation history: %d years of data", nrow(hist))

# ---- DERIVE METRICS ----

# All-time metrics from scholar package
total_cites_all <- prof$total_cites
h_all           <- prof$h_index
i10_all         <- prof$i10_index

log_info("All-time metrics: citations=%d, h-index=%d, i10-index=%d", 
         total_cites_all, h_all, i10_all)

# "Since [year]" metrics by scraping Google Scholar directly
log_info("Scraping 'Since' metrics from Google Scholar...")
since_metrics <- scrape_since_metrics(scholar_id)

total_cites_since <- since_metrics$cites_since
h_since           <- since_metrics$h_since
i10_since         <- since_metrics$i10_since

if (any(is.na(c(total_cites_since, h_since, i10_since)))) {
    log_warn("Some 'since' metrics are NA - check Google Scholar accessibility")
} else {
    log_info("Since metrics: citations=%d, h-index=%d, i10-index=%d",
             total_cites_since, h_since, i10_since)
}

# ---- CITATIONS BY YEAR ----

citations_by_year <- hist |>
    arrange(year) |>
    transmute(
        year  = year,
        count = cites
    )

log_info("Citations by year: %d to %d", 
         min(citations_by_year$year), max(citations_by_year$year))

# ---- BUILD OUTPUT ----

last_updated <- format(Sys.Date(), "%Y%m%d")
log_info("Last updated timestamp: %s", last_updated)

# NOTE: The 'since2020' key uses Google Scholar's "last 5 years" metrics,
# which are based on citations RECEIVED in that period (not papers published).
metrics_list <- list(
    lastUpdated = last_updated,
    citations   = list(
        all       = total_cites_all,
        since2020 = total_cites_since
    ),
    hIndex = list(
        all       = h_all,
        since2020 = h_since
    ),
    i10Index = list(
        all       = i10_all,
        since2020 = i10_since
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

# Compact citationsByYear entries to single lines
# Matches: {\n      "year": YYYY,\n      "count": N\n    }
# Replaces with: {"year": YYYY, "count": N}
json_text <- gsub(
    '\\{\n\\s+"year":\\s*(\\d+),\n\\s+"count":\\s*(\\d+)\n\\s+\\}',
    '{ "year": \\1, "count": \\2 }',
    json_text
)

log_info("Writing JSON to: %s", json_out)
writeLines(json_text, json_out)

log_info("Done! Metrics saved successfully")
log_debug("JSON output:\n%s", json_text)
