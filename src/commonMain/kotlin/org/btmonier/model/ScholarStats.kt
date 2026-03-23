package org.btmonier.model

import kotlinx.serialization.Serializable

@Serializable
data class ScholarStats(
    val lastUpdated: String,
    /** Start year of Google Scholar’s rolling “since” column (typically current calendar year − 5). */
    val sinceYear: Int,
    val citations: StatPair,
    val hIndex: StatPair,
    val i10Index: StatPair,
    val citationsByYear: List<YearCount>
)

@Serializable
data class StatPair(
    val all: Int,
    val since: Int
)

@Serializable
data class YearCount(
    val year: Int,
    val count: Int
)

