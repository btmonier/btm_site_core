package org.btmonier.components

import kotlinx.html.*
import kotlinx.html.dom.create
import kotlinx.browser.document
import org.btmonier.LanguageIconPaths
import org.btmonier.model.Software
import org.w3c.dom.HTMLElement

/**
 * MDI (Pictogrammers) classes for languages without a shared SVG in [LanguageIconPaths].
 * See: https://pictogrammers.com/library/mdi/
 */
private fun getLanguageIconMdiFallback(language: String): String {
    return when (language.lowercase()) {
        "go" -> "mdi mdi-language-go"
        "swift" -> "mdi mdi-language-swift"
        "js" -> "mdi mdi-language-javascript"
        else -> "mdi mdi-code-tags"
    }
}

fun createSoftwarePage(software: List<Software>): HTMLElement {
    // Extract unique languages and tags for filters
    val languages = software.flatMap { it.languages }.distinct().sorted()
    val allTags = software.flatMap { it.tags }.distinct().sorted()
    
    return document.create.div {
        div("page-header") {
            h1("page-title") { +"Software" }
            p("page-subtitle") { +"Open-source projects and tools to which I contribute, maintain, or have developed" }
        }
        
        // Filter Section
        div("filter-section") {
            id = "software-filters"
            
            // Language Filter
            div("filter-group") {
                h3("filter-group-title") {
                    span("material-icons") { +"code" }
                    +"Language"
                }
                div("filter-chips") {
                    // "All" chip
                    button(classes = "filter-chip active") {
                        attributes["data-filter-type"] = "language"
                        attributes["data-filter-value"] = "all"
                        +"All"
                    }
                    languages.forEach { lang ->
                        button(classes = "filter-chip") {
                            attributes["data-filter-type"] = "language"
                            attributes["data-filter-value"] = lang
                            +lang
                        }
                    }
                }
            }
            
            // Tags Filter (Dropdown)
            div("filter-group") {
                h3("filter-group-title") {
                    span("material-icons") { +"label" }
                    +"Topics"
                }
                div("filter-dropdown-container") {
                    select("filter-dropdown") {
                        id = "topic-filter"
                        option {
                            value = "all"
                            +"All Topics"
                        }
                        allTags.forEach { tag ->
                            option {
                                value = tag
                                +tag
                            }
                        }
                    }
                    span("material-icons filter-dropdown-icon") { +"expand_more" }
                }
            }
            
            // Active filters display
            div("active-filters") {
                id = "active-filters"
                span("active-filters-label") { +"Showing: " }
                span("active-filters-count") {
                    id = "filter-count"
                    +"${software.size} projects"
                }
                button(classes = "clear-filters-btn") {
                    id = "clear-filters"
                    style = "display: none;"
                    span("material-icons") { +"close" }
                    +"Clear filters"
                }
            }
        }
        
        // Software Grid
        div("software-grid") {
            id = "software-grid"
            software.forEach { sw ->
                div("software-card md-card md-card-elevated") {
                    attributes["data-languages"] = sw.languages.joinToString(",")
                    attributes["data-tags"] = sw.tags.joinToString(",")
                    
                    div("software-header") {
                        h3("software-name") { +sw.name }
                        if (sw.languages.isNotEmpty()) {
                            div("software-langs") {
                                sw.languages.forEach { lang ->
                                    span("software-lang") {
                                        title = lang
                                        val svgPath = LanguageIconPaths.webPath(lang)
                                        if (svgPath != null) {
                                            img(
                                                src = svgPath,
                                                alt = lang,
                                                classes = "software-lang-img",
                                            )
                                        } else {
                                            i(classes = getLanguageIconMdiFallback(lang)) {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                    p("software-description") { +sw.description }
                    
                    if (sw.tags.isNotEmpty()) {
                        div("software-tags") {
                            sw.tags.forEach { tag ->
                                span("md-chip md-chip-suggestion software-tag-chip") {
                                    attributes["data-tag"] = tag
                                    +tag
                                }
                            }
                        }
                    }
                    
                    div("software-actions") {
                        sw.repoUrl?.let { url ->
                            a(href = url, target = "_blank", classes = "md-button md-button-tonal") {
                                span("material-icons") { +"code" }
                                +"Code"
                            }
                        }
                        sw.docsUrl?.let { url ->
                            a(href = url, target = "_blank", classes = "md-button md-button-outlined") {
                                span("material-icons") { +"description" }
                                +"Docs"
                            }
                        }
                    }
                }
            }
        }
        
        // No results message
        div("no-results") {
            id = "no-results"
            style = "display: none;"
            span("material-icons") { +"search_off" }
            p { +"No projects match the selected filters" }
            button(classes = "md-button md-button-tonal") {
                id = "no-results-clear"
                +"Clear filters"
            }
        }
    }
}
