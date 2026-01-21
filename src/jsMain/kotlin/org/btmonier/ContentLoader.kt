package org.btmonier

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import org.btmonier.model.*
import org.w3c.fetch.Response
import kotlin.js.Promise

object ContentLoader {
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }

    private suspend fun fetchText(path: String): String {
        val response: Response = window.fetch(path).await()
        if (!response.ok) {
            throw RuntimeException("Failed to fetch $path: ${response.status}")
        }
        return response.text().await()
    }

    suspend fun loadSiteConfig(): SiteConfig {
        val text = fetchText("content/site.json")
        return json.decodeFromString<SiteConfig>(text)
    }

    suspend fun loadAbout(): About {
        val text = fetchText("content/about.json")
        return json.decodeFromString<About>(text)
    }

    suspend fun loadPublications(): List<Publication> {
        val text = fetchText("content/publications.json")
        return json.decodeFromString<Publications>(text).items
    }

    suspend fun loadSoftware(): List<Software> {
        val text = fetchText("content/software.json")
        return json.decodeFromString<SoftwareList>(text).items
    }

    suspend fun loadTeaching(): List<Teaching> {
        val text = fetchText("content/teaching.json")
        return json.decodeFromString<TeachingList>(text).items
    }

    suspend fun loadLinks(): List<LinkSection> {
        val text = fetchText("content/links.json")
        return json.decodeFromString<Links>(text).sections
    }

    suspend fun loadPresentations(): List<Presentation> {
        val text = fetchText("content/presentations.json")
        return json.decodeFromString<Presentations>(text).items
    }

    suspend fun loadEducation(): EducationData {
        val text = fetchText("content/education.json")
        return json.decodeFromString<EducationData>(text)
    }

    suspend fun loadScholarStats(): ScholarStats {
        val text = fetchText("content/scholar.json")
        return json.decodeFromString<ScholarStats>(text)
    }

    suspend fun loadSkills(): SkillsData {
        val text = fetchText("content/skills.json")
        return json.decodeFromString<SkillsData>(text)
    }

    suspend fun loadNotFoundImages(): NotFoundImages {
        val text = fetchText("content/404-images.json")
        return json.decodeFromString<NotFoundImages>(text)
    }
}

