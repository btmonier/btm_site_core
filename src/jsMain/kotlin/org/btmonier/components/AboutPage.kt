package org.btmonier.components

import kotlinx.html.*
import kotlinx.html.dom.create
import kotlinx.browser.document
import org.btmonier.model.About
import org.btmonier.model.SiteConfig
import org.w3c.dom.HTMLElement

// Regex to match markdown-style links: [text](url)
private val linkPattern = Regex("""\[([^\]]+)\]\(([^)]+)\)""")

/**
 * Renders text with markdown-style links [text](url) as HTML anchors
 */
private fun FlowContent.renderTextWithLinks(text: String) {
    var lastIndex = 0
    linkPattern.findAll(text).forEach { match ->
        // Add text before the link
        if (match.range.first > lastIndex) {
            +text.substring(lastIndex, match.range.first)
        }
        // Add the link
        val linkText = match.groupValues[1]
        val linkUrl = match.groupValues[2]
        a(href = linkUrl, target = "_blank") { +linkText }
        lastIndex = match.range.last + 1
    }
    // Add remaining text after last link
    if (lastIndex < text.length) {
        +text.substring(lastIndex)
    }
}

/**
 * Renders bio text with paragraph breaks (\n\n) and markdown-style links
 */
private fun DIV.renderBio(text: String) {
    // Split on double newlines for paragraphs
    val paragraphs = text.split("\\n\\n")
    paragraphs.forEach { paragraph ->
        if (paragraph.isNotBlank()) {
            p("about-bio") {
                renderTextWithLinks(paragraph.trim())
            }
        }
    }
}

fun createAboutPage(config: SiteConfig, about: About): HTMLElement {
    return document.create.div {
        // Hero Section
        div("about-hero") {
            div("about-avatar") {
                img(src = "images/self_01.jpg", alt = config.name, classes = "about-avatar-img")
            }
            div("about-info") {
                h1("about-name") { +config.name }
                p("about-title") { +config.subtitle }
                renderBio(about.bio)
                
                // Quick Links
                div("about-quick-links links-grid") {
                    a(href = "https://github.com/btmonier", target = "_blank", classes = "link-card md-card md-card-elevated") {
                        div("link-icon-container") {
                            i(classes = "fa-brands fa-github") {}
                        }
                        div("link-content") {
                            p("link-label") { +"GitHub" }
                            p("link-description") { +"Source code and projects" }
                        }
                        span("material-icons link-arrow") { +"arrow_forward" }
                    }
                    a(href = "https://scholar.google.com/citations?user=buYGhlYAAAAJ&hl=en", target = "_blank", classes = "link-card md-card md-card-elevated") {
                        div("link-icon-container") {
                            i(classes = "fa-brands fa-google-scholar") {}
                        }
                        div("link-content") {
                            p("link-label") { +"Google Scholar" }
                            p("link-description") { +"Publications and citations" }
                        }
                        span("material-icons link-arrow") { +"arrow_forward" }
                    }
                    a(href = "https://github.com/btmonier/btm_site_core/releases/download/cv-latest/btmonier_cv.pdf", target = "_blank", classes = "link-card md-card md-card-elevated") {
                        div("link-icon-container") {
                            i(classes = "fa-solid fa-file-pdf") {}
                        }
                        div("link-content") {
                            p("link-label") { +"Curriculum Vitae" }
                            p("link-description") { +"Download PDF" }
                        }
                        span("material-icons link-arrow") { +"arrow_forward" }
                    }
                }
            }
        }
    }
}
