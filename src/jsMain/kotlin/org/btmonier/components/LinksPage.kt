package org.btmonier.components

import kotlinx.html.*
import kotlinx.html.dom.create
import kotlinx.browser.document
import org.btmonier.model.Link
import org.btmonier.model.LinkSection
import org.w3c.dom.HTMLElement

fun createLinksPage(sections: List<LinkSection>): HTMLElement {
    return document.create.div {
        div("page-header") {
            h1("page-title") { +"Links" }
            p("page-subtitle") { +"Find me elsewhere on the web" }
        }
        
        sections.forEach { section ->
            div("links-section") {
                h2("section-title") { +section.title }
                div("links-grid") {
                    section.items.forEach { link ->
                        a(href = link.url, target = "_blank", classes = "link-card md-card md-card-elevated") {
                            div("link-icon-container") {
                                i(classes = link.icon ?: "fa-solid fa-link") {}
                            }
                            div("link-content") {
                                p("link-label") { +link.label }
                                link.description?.let { desc ->
                                    p("link-description") { +desc }
                                }
                            }
                            span("material-icons link-arrow") { +"arrow_forward" }
                        }
                    }
                }
            }
        }
    }
}

