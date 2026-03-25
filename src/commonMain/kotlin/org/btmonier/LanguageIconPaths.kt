package org.btmonier

/**
 * Shared SVG paths for programming language icons under [commonMain/resources/icons].
 *
 * Web: use [webPath] for `img src` (relative to the site root).
 * JVM: use [classpathPath] for `ClassLoader.getResourceAsStream` (leading `/`).
 */
object LanguageIconPaths {
    private val byLanguageName: Map<String, String> = mapOf(
        "R" to "icons/language-r.svg",
        "Python" to "icons/language-python.svg",
        "Kotlin" to "icons/language-kotlin.svg",
        "Java" to "icons/language-java.svg",
        "JavaScript" to "icons/language-javascript.svg",
        "TypeScript" to "icons/language-typescript.svg",
        "C++" to "icons/language-cpp.svg",
        "HTML" to "icons/language-html5.svg",
        "Rust" to "icons/language-rust.svg",
        "SQL" to "icons/database.svg",
        "Shiny" to "icons/language-shiny.svg",
        "CSS" to "icons/language-css3.svg",
        "Perl" to "icons/language-perl.svg",
    )

    /** Lowercase keys so lookups match JSON even if casing or surrounding spaces differ. */
    private val byNormalizedName: Map<String, String> =
        byLanguageName.entries.associate { (name, path) ->
            name.trim().lowercase() to path
        }

    fun webPath(language: String): String? =
        byNormalizedName[language.trim().lowercase()]

    fun classpathPath(language: String): String? =
        webPath(language)?.let { "/$it" }
}
