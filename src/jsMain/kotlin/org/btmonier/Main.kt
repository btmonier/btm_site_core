package org.btmonier

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import org.btmonier.components.*
import org.btmonier.model.*
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSelectElement
import kotlin.js.json

private val scope = MainScope()

private var currentPage = "about"
private var siteConfig: SiteConfig? = null
private var aboutData: About? = null
private var educationData: EducationData? = null
private var publicationsData: List<Publication>? = null
private var presentationsData: List<Presentation>? = null
private var softwareData: List<Software>? = null
private var teachingData: List<Teaching>? = null
private var linksData: List<Link>? = null
private var scholarStats: ScholarStats? = null
private var skillsData: SkillsData? = null
private var notFoundImages: NotFoundImages? = null

fun main() {
    window.onload = {
        initializeApp()
    }
}

private fun initializeApp() {
    showLoading()
    
    scope.launch {
        try {
            // Load all content
            siteConfig = ContentLoader.loadSiteConfig()
            aboutData = ContentLoader.loadAbout()
            educationData = ContentLoader.loadEducation()
            publicationsData = ContentLoader.loadPublications()
            presentationsData = ContentLoader.loadPresentations()
            softwareData = ContentLoader.loadSoftware()
            teachingData = ContentLoader.loadTeaching()
            linksData = ContentLoader.loadLinks()
            scholarStats = ContentLoader.loadScholarStats()
            skillsData = ContentLoader.loadSkills()
            notFoundImages = ContentLoader.loadNotFoundImages()
            
            // Get initial page from pathname
            val path = window.location.pathname.removePrefix("/").removeSuffix("/")
            val requestedPage = when {
                path.isEmpty() -> "about" // Root path "/" shows about page
                path.contains("-year-") -> "about" // Year anchors are handled via hash
                else -> path
            }
            currentPage = if (isValidPage(requestedPage)) requestedPage else "404"
            
            // Render the application
            renderApp()
            
            // Setup navigation listeners for browser back/forward
            window.onpopstate = { 
                val path = window.location.pathname.removePrefix("/").removeSuffix("/")
                val newPage = when {
                    path.isEmpty() -> "about" // Root path "/" shows about page
                    path.contains("-year-") -> currentPage // Year anchors are handled via hash, don't change page
                    else -> path
                }
                if (newPage != currentPage) {
                    currentPage = if (isValidPage(newPage)) newPage else "404"
                    renderPage()
                    updateActiveNavItems()
                }
            }
            
        } catch (e: Exception) {
            showError("Failed to load content: ${e.message}")
            console.error("Error loading content:", e)
        }
    }
}

private fun showLoading() {
    document.body?.innerHTML = ""
    document.body?.append {
        div("loading") {
            div("loading-spinner") {}
            p("loading-text") { +"Loading..." }
        }
    }
}

private fun showError(message: String) {
    document.body?.innerHTML = ""
    document.body?.append {
        div {
            id = "error-container"
            style = """
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                min-height: 100vh;
                padding: 24px;
                text-align: center;
            """.trimIndent()
            span("material-icons") {
                style = "font-size: 64px; color: var(--md-sys-color-error); margin-bottom: 16px;"
                +"error_outline"
            }
            h2 { 
                style = "margin-bottom: 8px;"
                +"Something went wrong" 
            }
            p { 
                style = "color: var(--md-sys-color-on-surface-variant);"
                +message 
            }
            button(classes = "md-button md-button-filled") {
                id = "retry-button"
                style = "margin-top: 24px;"
                +"Retry"
            }
        }
    }
    
    document.getElementById("retry-button")?.addEventListener("click", {
        window.location.reload()
    })
}

private fun renderApp() {
    document.body?.innerHTML = ""
    document.title = "${getPageTitle(currentPage)} - ${siteConfig?.name ?: "Personal Site"}"
    
    val appLayout = document.create.div("app-layout") {
        id = "app-layout"
    }
    document.body?.appendChild(appLayout)
    
    // Create Horizontal Navigation Bar (desktop)
    appLayout.appendChild(createNavBar())
    
    // Create Main Content Area
    val mainContent = document.create.div("main-content") {
        id = "main-content"
    }
    appLayout.appendChild(mainContent)
    
    // Create Page Container
    val pageContainer = document.create.div("page-container") {
        id = "page-container"
    }
    mainContent.appendChild(pageContainer)
    
    // Render current page
    renderPage()
    
    // Create Bottom Navigation (mobile)
    appLayout.appendChild(createBottomNav())
    
    // Setup nav click handlers
    setupNavigation()
}

private fun createNavBar(): HTMLElement {
    val config = siteConfig!!
    
    return document.create.nav("nav-bar") {
        id = "nav-bar"
        
        // Brand/Logo
        div("nav-bar-brand") {
            div("nav-bar-logo") {
                id = "nav-logo"
                title = config.name
                img(src = "images/avatar.svg", alt = config.name, classes = "nav-logo-img") {}
            }
            span("nav-bar-title") { +config.name }
        }
        
        // Navigation Items
        div("nav-bar-items") {
            navItems.forEach { (id, label, icon) ->
                a(href = "/$id", classes = "nav-bar-item") {
                    attributes["data-page"] = id
                    if (id == currentPage) classes = classes + " active"
                    span("material-icons") { +icon }
                    span("nav-bar-item-label") { +label }
                }
            }
            
            // Dropdown for Other Resources
            div("nav-bar-dropdown") {
                id = "nav-dropdown"
                val isDropdownActive = dropdownItems.any { it.first == currentPage }
                div("nav-bar-dropdown-trigger") {
                    if (isDropdownActive) classes = classes + " active"
                    span("material-icons") { +"more_horiz" }
                    span("nav-bar-item-label") { +"Other" }
                    span("material-icons dropdown-arrow") { +"expand_more" }
                }
                div("nav-bar-dropdown-menu") {
                    dropdownItems.forEach { (id, label, icon) ->
                        a(href = "/$id", classes = "nav-bar-dropdown-item") {
                            attributes["data-page"] = id
                            if (id == currentPage) classes = classes + " active"
                            span("material-icons") { +icon }
                            span("nav-bar-dropdown-item-label") { +label }
                        }
                    }
                }
            }
        }
        
        // Mobile Hamburger Menu Button
        button(classes = "nav-hamburger") {
            id = "nav-hamburger"
            span("material-icons") { +"menu" }
        }
        
        // Mobile Menu Dropdown
        div("nav-mobile-menu") {
            id = "nav-mobile-menu"
            val allNavItems = navItems + dropdownItems
            allNavItems.forEach { (id, label, icon) ->
                a(href = "/$id", classes = "nav-mobile-menu-item") {
                    attributes["data-page"] = id
                    if (id == currentPage) classes = classes + " active"
                    span("material-icons") { +icon }
                    span("nav-mobile-menu-item-label") { +label }
                }
            }
        }
    }
}

private fun createBottomNav(): HTMLElement {
    val allNavItems = navItems + dropdownItems
    return document.create.nav("bottom-nav") {
        id = "bottom-nav"
        div("bottom-nav-items") {
            allNavItems.forEach { (id, label, icon) ->
                a(href = "/$id", classes = "bottom-nav-item") {
                    attributes["data-page"] = id
                    if (id == currentPage) classes = classes + " active"
                    div("bottom-nav-item-icon") {
                        span("material-icons") { +icon }
                    }
                    span("bottom-nav-item-label") { +label }
                }
            }
        }
    }
}

private fun renderPage() {
    val container = document.getElementById("page-container") ?: return
    container.innerHTML = ""
    
    // Update document title
    document.title = "${getPageTitle(currentPage)} - ${siteConfig?.name ?: "Personal Site"}"
    
    val pageContent: HTMLElement = when (currentPage) {
        "about" -> createAboutPage(siteConfig!!, aboutData!!)
        "publications" -> createPublicationsPage(publicationsData!!, scholarStats!!)
        "presentations" -> createPresentationsPage(presentationsData!!)
        "software" -> createSoftwarePage(softwareData!!)
        "teaching" -> createTeachingPage(teachingData!!)
        "experience" -> createExperiencePage(educationData!!, skillsData!!)
        "links" -> createLinksPage(linksData!!)
        "404" -> createNotFoundPage(notFoundImages ?: NotFoundImages(emptyList()))
        else -> createNotFoundPage(notFoundImages ?: NotFoundImages(emptyList()))
    }
    
    container.appendChild(pageContent)
    
    // Setup year navigation after page is rendered
    setupYearNavigation()
    
    // Setup section navigation for experience page
    if (currentPage == "experience") {
        setupSectionNavigation()
    }
    
    // Setup software filters if on software page
    if (currentPage == "software") {
        setupSoftwareFilters()
    }
    
    // Setup publications filters if on publications page
    if (currentPage == "publications") {
        setupPublicationsFilters()
        initCitationsChart()
    }
}

private fun initCitationsChart() {
    val stats = scholarStats ?: return
    val canvas = document.getElementById("citations-chart") ?: return
    
    // Convert data to JS arrays
    val years = stats.citationsByYear.map { it.year }.toTypedArray()
    val counts = stats.citationsByYear.map { it.count }.toTypedArray()
    
    // Build config object using json helper
    val config = json(
        "type" to "bar",
        "data" to json(
            "labels" to years,
            "datasets" to arrayOf(
                json(
                    "label" to "Citations",
                    "data" to counts,
                    "backgroundColor" to "rgba(0, 106, 106, 0.7)",
                    "borderColor" to "rgb(0, 106, 106)",
                    "borderWidth" to 1,
                    "borderRadius" to 4
                )
            )
        ),
        "options" to json(
            "responsive" to true,
            "maintainAspectRatio" to false,
            "plugins" to json(
                "legend" to json("display" to false),
                "tooltip" to json(
                    "backgroundColor" to "rgba(30, 30, 30, 0.9)",
                    "padding" to 12,
                    "cornerRadius" to 8
                )
            ),
            "scales" to json(
                "y" to json(
                    "beginAtZero" to true,
                    "grid" to json("color" to "rgba(0, 0, 0, 0.08)")
                ),
                "x" to json(
                    "grid" to json("display" to false)
                )
            )
        )
    )
    
    // Store in window for JS access, then create chart
    window.asDynamic().__chartCanvas = canvas
    window.asDynamic().__chartConfig = config
    js("new Chart(window.__chartCanvas, window.__chartConfig)")
}

private fun setupNavigation() {
    // Setup click handlers for all nav items
    document.querySelectorAll(".nav-bar-item, .bottom-nav-item, .nav-bar-dropdown-item, .nav-mobile-menu-item").asList().forEach { element ->
        element.addEventListener("click", { e ->
            e.preventDefault()
            val page = element.getAttribute("data-page") ?: "about"
            navigateToPage(page)
            // Close dropdown after selection
            document.getElementById("nav-dropdown")?.classList?.remove("open")
        })
    }
    
    // Logo click navigates to about
    document.getElementById("nav-logo")?.addEventListener("click", {
        navigateToPage("about")
    })
    
    // Dropdown toggle
    document.querySelector(".nav-bar-dropdown-trigger")?.addEventListener("click", { e ->
        e.stopPropagation()
        document.getElementById("nav-dropdown")?.classList?.toggle("open")
    })
    
    // Close dropdown when clicking outside
    document.addEventListener("click", { e ->
        val dropdown = document.getElementById("nav-dropdown")
        val target = e.target as? Element
        if (dropdown != null && target != null && !dropdown.contains(target)) {
            dropdown.classList.remove("open")
        }
    })
    
    // Mobile hamburger menu toggle
    document.getElementById("nav-hamburger")?.addEventListener("click", { e ->
        e.stopPropagation()
        document.getElementById("nav-mobile-menu")?.classList?.toggle("open")
    })
    
    // Close mobile menu when clicking on a menu item
    document.querySelectorAll(".nav-mobile-menu-item").asList().forEach { element ->
        element.addEventListener("click", {
            document.getElementById("nav-mobile-menu")?.classList?.remove("open")
        })
    }
    
    // Close mobile menu when clicking outside
    document.addEventListener("click", { e ->
        val mobileMenu = document.getElementById("nav-mobile-menu")
        val hamburger = document.getElementById("nav-hamburger")
        val target = e.target as? Element
        if (mobileMenu != null && hamburger != null && target != null && 
            !mobileMenu.contains(target) && !hamburger.contains(target)) {
            mobileMenu.classList.remove("open")
        }
    })
}

private fun setupYearNavigation() {
    // Setup click handlers for year navigation items
    document.querySelectorAll(".year-nav-item").asList().forEach { element ->
        element.addEventListener("click", { e ->
            e.preventDefault()
            val href = element.getAttribute("href") ?: return@addEventListener
            val targetId = href.removePrefix("#")
            
            // Find the target section
            val targetSection = document.getElementById(targetId)
            targetSection?.let { section ->
                // Calculate scroll position accounting for fixed header
                val headerHeight = 64 // top nav height
                val elementPosition = section.asDynamic().getBoundingClientRect().top as Double
                val offsetPosition = elementPosition + window.pageYOffset - headerHeight - 16
                
                // Smooth scroll to section
                window.scrollTo(
                    js("({top: offsetPosition, behavior: 'smooth'})").unsafeCast<dynamic>()
                )
                
                // Update active state in year nav
                updateActiveYearNavItem(element)
            }
        })
    }
    
    // Setup scroll spy for year sections
    setupYearScrollSpy()
}

private fun updateActiveYearNavItem(activeElement: Element) {
    // Remove active from all year nav items
    document.querySelectorAll(".year-nav-item").asList().forEach { el ->
        el.classList.remove("active")
    }
    // Add active to clicked item
    activeElement.classList.add("active")
}

private fun setupYearScrollSpy() {
    var ticking = false
    
    window.addEventListener("scroll", {
        if (!ticking) {
            window.requestAnimationFrame {
                updateActiveYearFromScroll()
                ticking = false
            }
            ticking = true
        }
    })
    
    // Initial check
    updateActiveYearFromScroll()
}

private fun updateActiveYearFromScroll() {
    val headerHeight = 64 + 32 // nav height + some padding
    var activeYear: String? = null
    
    // Find which year section is currently in view
    document.querySelectorAll(".year-section").asList().forEach { section ->
        val rect = section.asDynamic().getBoundingClientRect()
        val top = rect.top as Double
        
        if (top <= headerHeight + 100) {
            activeYear = section.getAttribute("data-year")
        }
    }
    
    // Update active state
    if (activeYear != null) {
        document.querySelectorAll(".year-nav-item").asList().forEach { navItem ->
            val itemYear = navItem.getAttribute("data-year")
            if (itemYear == activeYear) {
                navItem.classList.add("active")
            } else {
                navItem.classList.remove("active")
            }
        }
    }
}

private fun setupSectionNavigation() {
    // Setup click handlers for section navigation items
    document.querySelectorAll(".section-nav-item").asList().forEach { element ->
        element.addEventListener("click", { e ->
            e.preventDefault()
            val href = element.getAttribute("href") ?: return@addEventListener
            val targetId = href.removePrefix("#")
            
            // Find the target section
            val targetSection = document.getElementById(targetId)
            targetSection?.let { section ->
                // Calculate scroll position accounting for fixed header
                val headerHeight = 64 // top nav height
                val elementPosition = section.asDynamic().getBoundingClientRect().top as Double
                val offsetPosition = elementPosition + window.pageYOffset - headerHeight - 16
                
                // Smooth scroll to section
                window.scrollTo(
                    js("({top: offsetPosition, behavior: 'smooth'})").unsafeCast<dynamic>()
                )
                
                // Update active state in section nav
                updateActiveSectionNavItem(element)
            }
        })
    }
    
    // Setup scroll spy for experience sections
    setupSectionScrollSpy()
}

private fun updateActiveSectionNavItem(activeElement: Element) {
    // Remove active from all section nav items
    document.querySelectorAll(".section-nav-item").asList().forEach { el ->
        el.classList.remove("active")
    }
    // Add active to clicked item
    activeElement.classList.add("active")
}

private fun setupSectionScrollSpy() {
    var ticking = false
    
    window.addEventListener("scroll", {
        if (!ticking && currentPage == "experience") {
            window.requestAnimationFrame {
                updateActiveSectionFromScroll()
                ticking = false
            }
            ticking = true
        }
    })
    
    // Initial check
    updateActiveSectionFromScroll()
}

private fun updateActiveSectionFromScroll() {
    val headerHeight = 64 + 32 // nav height + some padding
    var activeSection: String? = null
    
    // Find which experience section is currently in view
    document.querySelectorAll(".experience-section").asList().forEach { section ->
        val rect = section.asDynamic().getBoundingClientRect()
        val top = rect.top as Double
        
        if (top <= headerHeight + 100) {
            activeSection = section.getAttribute("data-section")
        }
    }
    
    // Update active state
    if (activeSection != null) {
        document.querySelectorAll(".section-nav-item").asList().forEach { navItem ->
            val itemSection = navItem.getAttribute("data-section")
            if (itemSection == activeSection) {
                navItem.classList.add("active")
            } else {
                navItem.classList.remove("active")
            }
        }
    }
}

private fun navigateToPage(page: String) {
    if (page == currentPage) return
    
    val targetPage = if (isValidPage(page)) page else "404"
    currentPage = targetPage
    window.history.pushState(null, "", "/$targetPage")
    renderPage()
    updateActiveNavItems()
    
    // Scroll to top
    window.scrollTo(0.0, 0.0)
}

private fun updateActiveNavItems() {
    document.querySelectorAll(".nav-bar-item, .bottom-nav-item, .nav-bar-dropdown-item, .nav-mobile-menu-item").asList().forEach { element ->
        val page = element.getAttribute("data-page")
        if (page == currentPage) {
            element.classList.add("active")
        } else {
            element.classList.remove("active")
        }
    }
    
    // Update dropdown trigger active state
    val isDropdownActive = dropdownItems.any { it.first == currentPage }
    val dropdownTrigger = document.querySelector(".nav-bar-dropdown-trigger")
    if (isDropdownActive) {
        dropdownTrigger?.classList?.add("active")
    } else {
        dropdownTrigger?.classList?.remove("active")
    }
}

private fun getPageTitle(page: String): String {
    return when (page) {
        "about" -> "About"
        "publications" -> "Publications"
        "presentations" -> "Presentations"
        "software" -> "Software"
        "teaching" -> "Teaching"
        "experience" -> "Experience"
        "links" -> "Links"
        "404" -> "Page Not Found"
        else -> "Page Not Found"
    }
}

private fun isValidPage(page: String): Boolean {
    val validPages = setOf("about", "publications", "presentations", "software", "teaching", "experience", "links")
    return validPages.contains(page)
}

// Software filtering state
private var activeLanguageFilter = "all"
private var activeTagFilter = "all"

// Publications filtering state
private var activeTypeFilter = "all"

private fun setupSoftwareFilters() {
    val filterChips = document.querySelectorAll(".filter-chip").asList()
    val tagChips = document.querySelectorAll(".software-tag-chip").asList()
    val topicDropdown = document.getElementById("topic-filter") as? HTMLSelectElement
    val clearFiltersBtn = document.getElementById("clear-filters")
    val noResultsClearBtn = document.getElementById("no-results-clear")
    
    // Reset filter state
    activeLanguageFilter = "all"
    activeTagFilter = "all"
    
    // Language filter chip click handlers
    filterChips.forEach { chip ->
        chip.addEventListener("click", {
            val filterType = chip.getAttribute("data-filter-type") ?: return@addEventListener
            val filterValue = chip.getAttribute("data-filter-value") ?: return@addEventListener
            
            // Update active state for this filter group
            document.querySelectorAll(".filter-chip[data-filter-type='$filterType']").asList().forEach { c ->
                c.classList.remove("active")
            }
            chip.classList.add("active")
            
            // Update filter state (only language now uses chips)
            if (filterType == "language") {
                activeLanguageFilter = filterValue
            }
            
            applyFilters()
        })
    }
    
    // Topic dropdown change handler
    topicDropdown?.addEventListener("change", {
        activeTagFilter = topicDropdown.value
        applyFilters()
    })
    
    // Tag chip click handlers (on cards) - sets dropdown value
    tagChips.forEach { tagChip ->
        tagChip.addEventListener("click", { e ->
            e.stopPropagation()
            val tag = tagChip.getAttribute("data-tag") ?: return@addEventListener
            
            // Set the dropdown value
            topicDropdown?.value = tag
            activeTagFilter = tag
            applyFilters()
        })
    }
    
    // Clear filters button
    clearFiltersBtn?.addEventListener("click", { clearAllFilters() })
    noResultsClearBtn?.addEventListener("click", { clearAllFilters() })
}

private fun clearAllFilters() {
    activeLanguageFilter = "all"
    activeTagFilter = "all"
    
    // Reset all filter chips
    document.querySelectorAll(".filter-chip").asList().forEach { chip ->
        chip.classList.remove("active")
        if (chip.getAttribute("data-filter-value") == "all") {
            chip.classList.add("active")
        }
    }
    
    // Reset topic dropdown
    val topicDropdown = document.getElementById("topic-filter") as? HTMLSelectElement
    topicDropdown?.value = "all"
    
    applyFilters()
}

private fun applyFilters() {
    val softwareCards = document.querySelectorAll(".software-card").asList()
    val clearFiltersBtn = document.getElementById("clear-filters")
    val noResults = document.getElementById("no-results")
    val softwareGrid = document.getElementById("software-grid")
    val filterCount = document.getElementById("filter-count")
    
    var visibleCount = 0
    
    softwareCards.forEach { card ->
        val cardLanguages = card.getAttribute("data-languages")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        val cardTags = card.getAttribute("data-tags")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        
        val matchesLanguage = activeLanguageFilter == "all" || cardLanguages.contains(activeLanguageFilter)
        val matchesTag = activeTagFilter == "all" || cardTags.contains(activeTagFilter)
        
        if (matchesLanguage && matchesTag) {
            card.classList.remove("hidden")
            visibleCount++
        } else {
            card.classList.add("hidden")
        }
    }
    
    // Update count
    filterCount?.textContent = "$visibleCount project${if (visibleCount != 1) "s" else ""}"
    
    // Show/hide clear button
    val hasActiveFilters = activeLanguageFilter != "all" || activeTagFilter != "all"
    (clearFiltersBtn as? HTMLElement)?.style?.display = if (hasActiveFilters) "inline-flex" else "none"
    
    // Show/hide no results message
    (noResults as? HTMLElement)?.style?.display = if (visibleCount == 0) "flex" else "none"
    (softwareGrid as? HTMLElement)?.style?.display = if (visibleCount == 0) "none" else "grid"
}

private fun setupPublicationsFilters() {
    val filterChips = document.querySelectorAll("#publications-filters .filter-chip").asList()
    val typeChips = document.querySelectorAll(".publication-type-chip").asList()
    val clearFiltersBtn = document.getElementById("clear-filters")
    val noResultsClearBtn = document.getElementById("no-results-clear")
    
    // Reset filter state
    activeTypeFilter = "all"
    
    // Type filter chip click handlers
    filterChips.forEach { chip ->
        chip.addEventListener("click", {
            val filterType = chip.getAttribute("data-filter-type") ?: return@addEventListener
            val filterValue = chip.getAttribute("data-filter-value") ?: return@addEventListener
            
            // Update active state for this filter group
            document.querySelectorAll("#publications-filters .filter-chip[data-filter-type='$filterType']").asList().forEach { c ->
                c.classList.remove("active")
            }
            chip.classList.add("active")
            
            // Update filter state
            if (filterType == "type") {
                activeTypeFilter = filterValue
            }
            
            applyPublicationFilters()
        })
    }
    
    // Type chip click handlers (on cards) - sets filter
    typeChips.forEach { typeChip ->
        typeChip.addEventListener("click", { e ->
            e.stopPropagation()
            val type = typeChip.getAttribute("data-type") ?: return@addEventListener
            
            // Set the filter
            activeTypeFilter = type
            
            // Update filter chip active state
            document.querySelectorAll("#publications-filters .filter-chip").asList().forEach { chip ->
                chip.classList.remove("active")
                if (chip.getAttribute("data-filter-value") == type) {
                    chip.classList.add("active")
                }
            }
            
            applyPublicationFilters()
        })
    }
    
    // Clear filters button
    clearFiltersBtn?.addEventListener("click", { clearAllPublicationFilters() })
    noResultsClearBtn?.addEventListener("click", { clearAllPublicationFilters() })
}

private fun clearAllPublicationFilters() {
    activeTypeFilter = "all"
    
    // Reset all filter chips
    document.querySelectorAll("#publications-filters .filter-chip").asList().forEach { chip ->
        chip.classList.remove("active")
        if (chip.getAttribute("data-filter-value") == "all") {
            chip.classList.add("active")
        }
    }
    
    applyPublicationFilters()
}

private fun applyPublicationFilters() {
    val publicationCards = document.querySelectorAll(".publication-card").asList()
    val clearFiltersBtn = document.getElementById("clear-filters")
    val noResults = document.getElementById("no-results")
    val publicationsList = document.querySelector(".publications-list")
    val filterCount = document.getElementById("filter-count")
    
    var visibleCount = 0
    
    publicationCards.forEach { card ->
        val cardType = card.getAttribute("data-type") ?: ""
        
        val matchesType = activeTypeFilter == "all" || cardType == activeTypeFilter
        
        if (matchesType) {
            card.classList.remove("hidden")
            visibleCount++
        } else {
            card.classList.add("hidden")
        }
    }
    
    // Also hide year sections if all publications in that year are hidden
    document.querySelectorAll(".year-section").asList().forEach { yearSection ->
        val cardsInSection = yearSection.querySelectorAll(".publication-card").asList()
        val visibleInSection = cardsInSection.count { !it.classList.contains("hidden") }
        
        if (visibleInSection == 0) {
            yearSection.classList.add("hidden")
        } else {
            yearSection.classList.remove("hidden")
        }
    }
    
    // Update count
    filterCount?.textContent = "$visibleCount publication${if (visibleCount != 1) "s" else ""}"
    
    // Show/hide clear button
    val hasActiveFilters = activeTypeFilter != "all"
    (clearFiltersBtn as? HTMLElement)?.style?.display = if (hasActiveFilters) "inline-flex" else "none"
    
    // Show/hide no results message
    (noResults as? HTMLElement)?.style?.display = if (visibleCount == 0) "flex" else "none"
    (publicationsList as? HTMLElement)?.style?.display = if (visibleCount == 0) "none" else "block"
}

// Primary navigation items
private val navItems = listOf(
    Triple("about", "About", "person"),
    Triple("publications", "Publications", "article"),
    Triple("presentations", "Presentations", "mic"),
    Triple("teaching", "Teaching", "school"),
    Triple("software", "Software", "code")
)

// Dropdown items for "Other Resources"
private val dropdownItems = listOf(
    Triple("experience", "Experience", "work_history"),
    Triple("links", "Links", "link")
)

// Extension function to convert NodeList to List
private fun org.w3c.dom.NodeList.asList(): List<Element> {
    val list = mutableListOf<Element>()
    for (i in 0 until this.length) {
        this.item(i)?.let { list.add(it as Element) }
    }
    return list
}
