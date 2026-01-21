package org.btmonier.model

import kotlinx.serialization.Serializable

@Serializable
data class Link(
    val label: String,
    val url: String,
    val description: String? = null,
    val icon: String? = null
)

@Serializable
data class LinkSection(
    val title: String,
    val items: List<Link>
)

@Serializable
data class Links(
    val sections: List<LinkSection>
)

