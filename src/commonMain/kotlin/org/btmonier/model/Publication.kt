package org.btmonier.model

import kotlinx.serialization.Serializable

@Serializable
data class Publication(
    val title: String,
    val authors: List<String>,
    val journal: String,
    val year: Int,
    val volume: String? = null,
    val pages: String? = null,
    val doi: String? = null,
    val url: String? = null,
    val abstract: String? = null,
    val type: String? = null
)

@Serializable
data class Publications(
    val items: List<Publication>
)

