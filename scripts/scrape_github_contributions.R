#!/usr/bin/env Rscript
## Back up GitHub contribution counts via the GraphQL contribution calendar (heatmap).
## Requires a Personal Access Token with at least `read:user` scope.
## Usage:
##   Copy .env.example to .env and set GITHUB_TOKEN, or export GITHUB_TOKEN / GH_TOKEN.
##   Rscript scripts/scrape_github_contributions.R
##   Rscript scripts/scrape_github_contributions.R --login btmonier --output contributions.json

suppressPackageStartupMessages({
    library(jsonlite)
    library(httr)
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

# ---- CLI ----

parse_args <- function() {
    args <- commandArgs(trailingOnly = TRUE)
    out <- list(login = "btmonier", output = "contributions.json")
    i <- 1L
    while (i <= length(args)) {
        a <- args[[i]]
        if (a %in% c("--login", "-l") && i < length(args)) {
            out$login <- args[[i + 1L]]
            i <- i + 2L
        } else if (a %in% c("--output", "-o") && i < length(args)) {
            out$output <- args[[i + 1L]]
            i <- i + 2L
        } else if (a %in% c("--help", "-h")) {
            cat("Usage: Rscript scrape_github_contributions.R [options]\n")
            cat("  Credentials: GITHUB_TOKEN or GH_TOKEN (env), or a .env file in the project tree.\n")
            cat("  --login, -l   GitHub username (default: btmonier)\n")
            cat("  --output, -o  JSON output path (default: contributions.json)\n")
            quit(status = 0L)
        } else {
            log_warn("Unknown argument ignored: %s", a)
            i <- i + 1L
        }
    }
    out
}

# ---- GITHUB GRAPHQL ----

GITHUB_GRAPHQL <- "https://api.github.com/graphql"

#' Directory containing this script (when run via Rscript --file=...), else NULL.
script_dir <- function() {
    args <- commandArgs(trailingOnly = FALSE)
    f <- grep("^--file=", args, value = TRUE)
    if (length(f) == 0L) {
        return(NULL)
    }
    p <- normalizePath(sub("^--file=", "", f[[1L]]), winslash = "/", mustWork = FALSE)
    if (is.na(p)) {
        return(NULL)
    }
    dirname(p)
}

#' Find a .env file by walking up from `start` (at most `max_up` parents).
find_dotenv_from <- function(start, max_up = 16L) {
    if (is.null(start) || !nzchar(start)) {
        return(NA_character_)
    }
    dir <- normalizePath(start, winslash = "/", mustWork = FALSE)
    if (is.na(dir)) {
        return(NA_character_)
    }
    for (i in seq_len(max_up)) {
        p <- file.path(dir, ".env")
        if (file.exists(p)) {
            return(p)
        }
        parent <- dirname(dir)
        if (parent == dir) {
            break
        }
        dir <- parent
    }
    NA_character_
}

#' Load first .env found: cwd chain, then script-dir chain. Uses readRenviron (KEY=value, # comments).
load_dotenv <- function() {
    candidates <- c(
        find_dotenv_from(getwd()),
        find_dotenv_from(script_dir())
    )
    candidates <- candidates[!is.na(candidates)]
    candidates <- unique(candidates)
    if (length(candidates) == 0L) {
        log_debug("No .env file found (searched upward from getwd() and script directory)")
        return(invisible(FALSE))
    }
    p <- candidates[[1L]]
    log_info("Loading environment from %s", p)
    tryCatch(
        {
            readRenviron(p)
            invisible(TRUE)
        },
        error = function(e) {
            log_warn("Could not read %s: %s", p, conditionMessage(e))
            invisible(FALSE)
        }
    )
}

get_token <- function() {
    tok <- Sys.getenv(c("GITHUB_TOKEN", "GH_TOKEN"), unset = NA_character_)
    tok <- tok[!is.na(tok) & nzchar(tok)]
    if (length(tok) == 0L) {
        log_error("Set GITHUB_TOKEN or GH_TOKEN to a GitHub PAT (read:user).")
        quit(status = 1L)
    }
    tok[[1L]]
}

graphql <- function(token, query, variables = NULL) {
    body <- list(query = query)
    if (!is.null(variables)) {
        body$variables <- variables
    }
    resp <- POST(
        GITHUB_GRAPHQL,
        add_headers(
            Authorization = paste("Bearer", token),
            `User-Agent` = "btm-site-core-contribution-backup"
        ),
        body = body,
        encode = "json"
    )
    if (http_error(resp)) {
        log_error("HTTP %s: %s", status_code(resp), content(resp, "text", encoding = "UTF-8"))
        quit(status = 1L)
    }
    fromJSON(content(resp, "text", encoding = "UTF-8"), simplifyVector = FALSE)
}

query_created_at <- '
query($login: String!) {
  user(login: $login) {
    createdAt
  }
}
'

query_calendar <- '
query($login: String!, $from: DateTime!, $to: DateTime!) {
  user(login: $login) {
    contributionsCollection(from: $from, to: $to) {
      contributionCalendar {
        totalContributions
        weeks {
          contributionDays {
            date
            contributionCount
            contributionLevel
            weekday
          }
        }
      }
    }
  }
}
'

parse_created_year <- function(iso) {
    as.integer(substr(iso, 1L, 4L))
}

#' Flatten weeks -> list of named list(date =, count =).
#' The calendar is grouped by week; edge weeks include contributionDays whose `date`
#' falls outside the requested `from`/`to` window. Those days repeat across
#' adjacent yearly API calls; we must filter by inclusive [from_date, to_date] or
#' per-day counts double at year boundaries (and no longer match a single `gh api` query).
calendar_to_rows <- function(parsed, from_date, to_date) {
    errs <- parsed$errors
    if (!is.null(errs) && length(errs) > 0L) {
        msgs <- vapply(errs, function(e) e$message %||% "unknown", character(1L))
        log_error("GraphQL error: %s", paste(msgs, collapse = "; "))
        quit(status = 1L)
    }
    u <- parsed$data$user
    if (is.null(u)) {
        log_error("User not found or not visible with this token.")
        quit(status = 1L)
    }
    cc <- u$contributionsCollection$contributionCalendar
    if (is.null(cc)) {
        return(list())
    }
    weeks <- cc$weeks
    if (is.null(weeks)) {
        return(list())
    }
    rows <- list()
    for (w in weeks) {
        days <- w$contributionDays
        if (is.null(days)) next
        for (d in days) {
            dt <- d$date
            if (dt < from_date || dt > to_date) {
                next
            }
            rows[[length(rows) + 1L]] <- list(
                date  = dt,
                count = as.integer(d$contributionCount %||% 0L)
            )
        }
    }
    rows
}

`%||%` <- function(x, y) if (is.null(x)) y else x

#' One calendar year in UTC; GitQL allows at most 1 year between from and to.
fetch_year <- function(token, login, year) {
    from_iso <- sprintf("%d-01-01T00:00:00Z", year)
    to_iso   <- sprintf("%d-12-31T23:59:59Z", year)
    from_date <- sprintf("%d-01-01", year)
    to_date   <- sprintf("%d-12-31", year)
    log_debug("Fetching %s .. %s", from_iso, to_iso)
    parsed <- graphql(
        token,
        query_calendar,
        list(login = login, from = from_iso, to = to_iso)
    )
    calendar_to_rows(parsed, from_date, to_date)
}

main <- function() {
    opts <- parse_args()
    load_dotenv()
    token <- get_token()
    login <- opts$login
    out_path <- opts$output

    log_info("Resolving account creation for '%s'", login)
    ca <- graphql(token, query_created_at, list(login = login))
    if (!is.null(ca$errors) && length(ca$errors) > 0L) {
        msgs <- vapply(ca$errors, function(e) e$message %||% "unknown", character(1L))
        log_error("GraphQL error: %s", paste(msgs, collapse = "; "))
        quit(status = 1L)
    }
    if (is.null(ca$data$user)) {
        log_error("User not found: %s", login)
        quit(status = 1L)
    }
    created <- ca$data$user$createdAt
    start_year <- parse_created_year(created)
    end_year <- as.integer(format(Sys.Date(), "%Y"))

    log_info("Fetching contribution calendar from %d through %d", start_year, end_year)

    all_rows <- list()
    for (yr in seq.int(start_year, end_year)) {
        rows <- fetch_year(token, login, yr)
        all_rows <- c(all_rows, rows)
        n_days <- length(rows)
        tot <- if (n_days > 0L) {
            sum(vapply(rows, function(r) r$count, integer(1L)))
        } else {
            0L
        }
        log_info("Year %d: %d days in payload, %d contributions", yr, n_days, tot)
    }

    if (length(all_rows) == 0L) {
        log_warn("No contribution days returned; writing empty array.")
        out <- list()
    } else {
        dates <- vapply(all_rows, function(r) r$date, character(1L))
        counts <- vapply(all_rows, function(r) r$count, integer(1L))
        # Defensive: one date per day after range filtering; sum would duplicate if not.
        tot_by_day <- tapply(counts, dates, sum)
        udates <- sort(names(tot_by_day))
        out <- lapply(udates, function(d) {
            list(date = d, count = as.integer(tot_by_day[[d]]))
        })
    }

    json_text <- toJSON(out, pretty = TRUE, auto_unbox = TRUE)
    log_info("Writing %d records to %s", length(out), out_path)
    writeLines(json_text, out_path)
    log_info("Done.")
}

main()
