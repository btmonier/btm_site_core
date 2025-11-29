package org.btmonier.cli

import com.lowagie.text.*
import com.lowagie.text.pdf.*
import com.lowagie.text.pdf.draw.LineSeparator
import kotlinx.serialization.json.Json
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import org.btmonier.model.*
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class CvPdfGenerator(private val dataDir: String) {

    private val json = Json { ignoreUnknownKeys = true }

    // Colors
    private val maroonColor = Color(128, 0, 32) // Dark red/maroon
    private val grayBg = Color(230, 230, 230) // Light gray background
    private val accentLineColor = Color(128, 0, 32) // Maroon accent lines
    private val tableBorderColor = Color(200, 200, 200)

    // Load Roboto Serif from resources
    private val robotoSerifBase: BaseFont = try {
        val fontStream = javaClass.getResourceAsStream("/fonts/roboto_serif.ttf")
            ?: throw Exception("Font not found in resources")
        val tempFile = File.createTempFile("roboto_serif", ".ttf")
        tempFile.deleteOnExit()
        tempFile.outputStream().use { out -> fontStream.copyTo(out) }
        BaseFont.createFont(tempFile.absolutePath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
    } catch (e: Exception) {
        println("Warning: Could not load Roboto Serif, falling back to Times Roman: ${e.message}")
        BaseFont.createFont(BaseFont.TIMES_ROMAN, BaseFont.CP1252, BaseFont.NOT_EMBEDDED)
    }

    // SVG icon paths
    object Icons {
        const val LOCATION = "/icons/location.svg"
        const val GLOBE = "/icons/globe.svg"
        const val PHONE = "/icons/phone.svg"
        const val EMAIL = "/icons/envelope.svg"
        const val ORCID = "/icons/orcid.svg"
        const val SCHOLAR = "/icons/google-scholar.svg" // Add google-scholar.svg to resources
    }

    // Convert SVG to PNG Image for PDF embedding
    private fun loadSvgIcon(resourcePath: String, size: Float = 12f, color: Color? = null): Image? {
        return try {
            val svgStream = javaClass.getResourceAsStream(resourcePath)
                ?: throw Exception("SVG not found: $resourcePath")
            
            // Read SVG content and optionally recolor
            var svgContent = svgStream.bufferedReader().readText()
            if (color != null) {
                val hexColor = String.format("#%02x%02x%02x", color.red, color.green, color.blue)
                // Replace fill colors in SVG
                svgContent = svgContent.replace(Regex("fill=\"[^\"]*\""), "fill=\"$hexColor\"")
                // Add fill if not present
                if (!svgContent.contains("fill=")) {
                    svgContent = svgContent.replace("<path", "<path fill=\"$hexColor\"")
                    svgContent = svgContent.replace("<circle", "<circle fill=\"$hexColor\"")
                    svgContent = svgContent.replace("<rect", "<rect fill=\"$hexColor\"")
                }
            }
            
            // Transcode SVG to PNG
            val transcoder = PNGTranscoder()
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, size * 4) // Higher res for quality
            transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, size * 4)
            
            val input = TranscoderInput(svgContent.byteInputStream())
            val outputStream = ByteArrayOutputStream()
            val output = TranscoderOutput(outputStream)
            
            transcoder.transcode(input, output)
            
            val pngBytes = outputStream.toByteArray()
            val image = Image.getInstance(pngBytes)
            image.scaleAbsolute(size, size)
            image
        } catch (e: Exception) {
            println("Warning: Could not load SVG icon $resourcePath: ${e.message}")
            null
        }
    }

    // Fonts - using Roboto Serif with programmatic bold/italic
    private val titleFont = Font(robotoSerifBase, 14f, Font.BOLD, maroonColor)
    private val subtitleFont = Font(robotoSerifBase, 9f, Font.BOLD, maroonColor)
    private val subtitleNormalFont = Font(robotoSerifBase, 9f, Font.NORMAL, maroonColor)
    private val contactFont = Font(robotoSerifBase, 8f, Font.NORMAL, maroonColor)
    private val contactBoldFont = Font(robotoSerifBase, 8f, Font.BOLD, maroonColor)
    private val sectionFont = Font(robotoSerifBase, 11f, Font.BOLD)
    private val subsectionFont = Font(robotoSerifBase, 10f, Font.BOLD)
    private val bodyFont = Font(robotoSerifBase, 9f, Font.NORMAL)
    private val bodyBoldFont = Font(robotoSerifBase, 9f, Font.BOLD)
    private val bodyItalicFont = Font(robotoSerifBase, 9f, Font.ITALIC)
    private val smallFont = Font(robotoSerifBase, 8f, Font.NORMAL)
    private val tableHeaderFont = Font(robotoSerifBase, 9f, Font.BOLD, Color.WHITE)
    private val tableBodyFont = Font(robotoSerifBase, 9f, Font.NORMAL)
    private val tableBoldFont = Font(robotoSerifBase, 9f, Font.BOLD)

    // Generate current date in "Month Year" format
    private fun getCurrentDate(): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        return LocalDate.now().format(formatter)
    }

    fun generate(outputPath: String) {
        // Load all data
        val siteConfig = loadJson<SiteConfig>("site.json")
        val cvConfig = loadJson<CvConfig>("cv.json")
        val education = loadJson<EducationData>("education.json")
        val publications = loadJson<Publications>("publications.json")
        val presentations = loadJson<Presentations>("presentations.json")
        val software = loadJson<SoftwareList>("software.json")
        val skills = loadJson<SkillsData>("skills.json")
        val scholarStats = loadJson<ScholarStats>("scholar.json")

        // Auto-generate CV date at compile/run time
        val cvDate = getCurrentDate()

        // Create document with reduced margins
        val document = Document(PageSize.LETTER, 54f, 54f, 60f, 54f)
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        val writer = PdfWriter.getInstance(document, FileOutputStream(outputFile))
        
        // Add header/footer event handler
        writer.pageEvent = HeaderFooterPageEvent(
            headerText = "Curriculum Vitae – ${siteConfig.name}",
            dateText = cvDate,
            baseFont = robotoSerifBase,
            accentColor = maroonColor
        )
        
        document.open()

        // Create PDF outline (bookmarks) for navigation
        val root = PdfOutline(writer.directContent.rootOutline, PdfAction(""), "Contents", false)
        
        // Add content with bookmarks
        addTitleSection(document, siteConfig, cvConfig, cvDate)
        
        addBookmarkedSection(writer, root, "Education")
        addEducation(document, education.degrees)
        
        addBookmarkedSection(writer, root, "Research Experience")
        addResearchExperience(document, education.experience)
        
        addBookmarkedSection(writer, root, "Skills")
        addSkills(document, skills)
        
        addBookmarkedSection(writer, root, "Software")
        addSoftware(document, software.items)
        
        addBookmarkedSection(writer, root, "Publications")
        addPublications(document, publications.items, scholarStats)
        
        addBookmarkedSection(writer, root, "Presentations")
        addPresentations(document, presentations.items)

        document.close()
    }

    private inline fun <reified T> loadJson(filename: String): T {
        val file = File(dataDir, filename)
        return json.decodeFromString(file.readText())
    }

    // Add a PDF bookmark/outline entry for a section
    private fun addBookmarkedSection(writer: PdfWriter, parent: PdfOutline, title: String) {
        val dest = PdfDestination(PdfDestination.FITH)
        writer.directContent.localDestination(title, dest)
        PdfOutline(parent, PdfAction.gotoLocalPage(title, false), title)
    }

    private fun addTitleSection(document: Document, siteConfig: SiteConfig, cvConfig: CvConfig, cvDate: String) {
        // Create main table with gray background
        val mainTable = PdfPTable(2)
        mainTable.widthPercentage = 100f
        mainTable.setWidths(floatArrayOf(55f, 45f))

        // Left side: Name and CV title
        val leftCell = PdfPCell()
        leftCell.border = Rectangle.NO_BORDER
        leftCell.backgroundColor = grayBg
        leftCell.paddingLeft = 20f
        leftCell.paddingTop = 15f
        leftCell.paddingBottom = 15f
        leftCell.verticalAlignment = Element.ALIGN_MIDDLE

        val namePhrase = Phrase()
        namePhrase.add(Chunk(siteConfig.name, titleFont))
        leftCell.addElement(Paragraph(namePhrase))

        val cvPhrase = Phrase()
        cvPhrase.add(Chunk("Curriculum Vitae", subtitleFont))
        val cvPara = Paragraph(cvPhrase)
        cvPara.spacingBefore = 8f
        leftCell.addElement(cvPara)

        val datePara = Paragraph(cvDate, subtitleNormalFont)
        leftCell.addElement(datePara)

        mainTable.addCell(leftCell)

        // Right side: Contact info with icons
        val rightCell = PdfPCell()
        rightCell.border = Rectangle.NO_BORDER
        rightCell.backgroundColor = grayBg
        rightCell.paddingRight = 20f
        rightCell.paddingTop = 15f
        rightCell.paddingBottom = 15f
        rightCell.verticalAlignment = Element.ALIGN_MIDDLE
        rightCell.horizontalAlignment = Element.ALIGN_RIGHT

        // Create contact table with SVG icons
        val contactTable = PdfPTable(2)
        contactTable.widthPercentage = 100f
        contactTable.setWidths(floatArrayOf(10f, 90f))
        contactTable.horizontalAlignment = Element.ALIGN_RIGHT

        // Using SVG icons with clickable links
        // Split institution into multiple lines (e.g., "Institute, University" -> separate lines)
        val institutionParts = cvConfig.institution.split(", ")
        val locationText = if (institutionParts.size >= 2) {
            "${institutionParts[0]}\n${institutionParts[1]}\n${cvConfig.address}"
        } else {
            "${cvConfig.institution}\n${cvConfig.address}"
        }
        addContactRowWithIcon(contactTable, Icons.LOCATION, locationText)
        
        // Website - clickable link
        addContactRowWithIcon(contactTable, Icons.GLOBE, cvConfig.website, "https://www.btmonier.org")
        
        // Phone
        addContactRowWithIcon(contactTable, Icons.PHONE, cvConfig.phone)
        
        // Email - clickable mailto link
        addContactRowWithIcon(contactTable, Icons.EMAIL, siteConfig.email, "mailto:${siteConfig.email}")
        
        // ORCID - clickable link to profile
        val orcid = siteConfig.socialLinks.find { it.platform == "ORCID" }
        if (orcid != null) {
            val orcidId = orcid.url.substringAfterLast("/")
            addContactRowWithIcon(contactTable, Icons.ORCID, orcidId, orcid.url)
        }

        rightCell.addElement(contactTable)
        mainTable.addCell(rightCell)

        // Add accent lines (top and bottom maroon lines)
        val topLine = PdfPTable(1)
        topLine.widthPercentage = 100f
        val topLineCell = PdfPCell()
        topLineCell.border = Rectangle.NO_BORDER
        topLineCell.borderWidthTop = 3f
        topLineCell.borderColorTop = accentLineColor
        topLineCell.fixedHeight = 3f
        topLine.addCell(topLineCell)
        document.add(topLine)

        document.add(mainTable)

        val bottomLine = PdfPTable(1)
        bottomLine.widthPercentage = 100f
        val bottomLineCell = PdfPCell()
        bottomLineCell.border = Rectangle.NO_BORDER
        bottomLineCell.borderWidthBottom = 3f
        bottomLineCell.borderColorBottom = accentLineColor
        bottomLineCell.fixedHeight = 3f
        bottomLine.addCell(bottomLineCell)
        document.add(bottomLine)

        document.add(Paragraph("\n"))
    }

    private fun addContactRowWithIcon(table: PdfPTable, iconPath: String, text: String, url: String? = null) {
        val iconCell = PdfPCell()
        iconCell.border = Rectangle.NO_BORDER
        iconCell.horizontalAlignment = Element.ALIGN_RIGHT
        iconCell.paddingRight = 5f
        iconCell.paddingBottom = 3f
        iconCell.verticalAlignment = Element.ALIGN_TOP
        
        // Load SVG icon
        val icon = loadSvgIcon(iconPath, 10f, maroonColor)
        if (icon != null) {
            iconCell.addElement(icon)
        }
        table.addCell(iconCell)

        // Create text with optional hyperlink
        val textCell = PdfPCell()
        textCell.border = Rectangle.NO_BORDER
        textCell.paddingBottom = 3f
        
        if (url != null) {
            // Create clickable link with blue underline styling
            val linkFont = Font(contactFont)
            linkFont.color = Color(0, 0, 180) // Blue color for links
            linkFont.style = Font.UNDERLINE
            
            val chunk = Chunk(text, linkFont)
            chunk.setAnchor(url)
            textCell.phrase = Phrase(chunk)
        } else {
            textCell.phrase = Phrase(text, contactFont)
        }
        table.addCell(textCell)
    }

    private fun addSectionHeader(document: Document, title: String) {
        document.add(Paragraph("\n"))

        val header = Paragraph(title, sectionFont)
        document.add(header)

        val line = LineSeparator()
        line.lineColor = Color.BLACK
        document.add(Chunk(line))

        document.add(Paragraph("\n"))
    }

    private fun addEducation(document: Document, degrees: List<Degree>) {
        addSectionHeader(document, "Education")

        for (degree in degrees.sortedByDescending { it.year }) {
            // Main line: Year | Degree Field
            val degreeLine = Paragraph()
            degreeLine.add(Chunk("${degree.year}", bodyBoldFont))
            degreeLine.add(Chunk("  |  ", bodyFont))
            degreeLine.add(Chunk("${degree.degree}, ${degree.field}", bodyBoldFont))
            document.add(degreeLine)

            // Institution and location
            val instLine = Paragraph()
            instLine.indentationLeft = 30f
            instLine.add(Chunk(degree.institution, bodyItalicFont))
            instLine.add(Chunk("  |  ", bodyFont))
            instLine.add(Chunk(degree.location, bodyFont))
            document.add(instLine)

            // Department (if applicable)
            if (degree.department != null) {
                val deptLine = Paragraph(degree.department, bodyFont)
                deptLine.indentationLeft = 30f
                document.add(deptLine)
            }

            // Thesis (if applicable)
            if (degree.thesis != null) {
                val thesisLine = Paragraph()
                thesisLine.indentationLeft = 30f
                thesisLine.add(Chunk("Thesis: ", bodyItalicFont))
                thesisLine.add(Chunk("\"${degree.thesis}\"", bodyFont))
                document.add(thesisLine)
            }

            document.add(Paragraph("\n"))
        }
    }

    private fun addResearchExperience(document: Document, experiences: List<Experience>) {
        addSectionHeader(document, "Research Experience")

        for (exp in experiences) {
            val headerLine = Paragraph()
            headerLine.add(Chunk("${exp.institution} | ${exp.position} | ${exp.period}", bodyBoldFont))
            document.add(headerLine)

            if (exp.advisor != null) {
                val advisorLine = Paragraph("Advisor: ${exp.advisor}", bodyItalicFont)
                advisorLine.indentationLeft = 15f
                document.add(advisorLine)
            }

            val deliveredLine = Paragraph("Delivered:", bodyFont)
            deliveredLine.indentationLeft = 15f
            document.add(deliveredLine)

            val list = com.lowagie.text.List(com.lowagie.text.List.UNORDERED)
            list.setListSymbol("• ")
            list.indentationLeft = 30f

            for (highlight in exp.highlights) {
                val item = ListItem(highlight, bodyFont)
                item.spacingAfter = 2f
                list.add(item)
            }
            document.add(list)

            document.add(Paragraph("\n"))
        }
    }

    private fun addSkills(document: Document, skills: SkillsData) {
        addSectionHeader(document, "Skills")

        for (category in skills.categories) {
            val categoryLine = Paragraph()
            categoryLine.add(Chunk("${category.name}: ", bodyBoldFont))
            categoryLine.add(Chunk(category.items.joinToString(", "), bodyFont))
            categoryLine.spacingAfter = 3f
            document.add(categoryLine)
        }
    }

    // Language to SVG icon path mapping
    private val languageIcons = mapOf(
        "R" to "/icons/language-r.svg",
        "Python" to "/icons/language-python.svg",
        "Kotlin" to "/icons/language-kotlin.svg",
        "Java" to "/icons/language-java.svg",
        "JavaScript" to "/icons/language-javascript.svg",
        "TypeScript" to "/icons/language-typescript.svg",
        "C++" to "/icons/language-cpp.svg",
        "HTML" to "/icons/language-html5.svg",
        "Rust" to "/icons/language-rust.svg",
        "SQL" to "/icons/database.svg",
        "Shiny" to "/icons/language-shiny.svg",
        "CSS" to "/icons/language-css3.svg",
        "Perl" to "/icons/language-perl.svg"
    )

    private fun addSoftware(document: Document, software: List<Software>) {
        addSectionHeader(document, "Software")

        // Add language icon legend
        addLanguageLegend(document, software)

        for (sw in software) {
            // Calculate dynamic widths based on content
            val titleWidth = robotoSerifBase.getWidthPoint(sw.name, 10f) // bodyBoldFont size
            val iconSize = 10f
            val iconSpacing = 4f // spacing between icons
            val validIconCount = sw.languages.count { languageIcons.containsKey(it) }
            val iconsWidth = if (validIconCount > 0) {
                (validIconCount * iconSize) + ((validIconCount - 1) * iconSpacing)
            } else 0f
            
            // Calculate available width for the table (page width minus margins)
            val pageWidth = PageSize.LETTER.width - 72f - 72f // 72pt margins on each side
            val leaderWidth = pageWidth - titleWidth - iconsWidth - 10f // 10f for minimal padding
            
            // Ensure minimum leader width
            val adjustedLeaderWidth = maxOf(leaderWidth, 50f)
            
            // Create table with dynamic column widths
            val entryTable = PdfPTable(3)
            entryTable.widthPercentage = 100f
            entryTable.setTotalWidth(floatArrayOf(titleWidth + 5f, adjustedLeaderWidth, iconsWidth + 5f))
            entryTable.isLockedWidth = true

            // Left cell: Software name
            val nameCell = PdfPCell()
            nameCell.border = Rectangle.NO_BORDER
            nameCell.phrase = Phrase(sw.name, bodyBoldFont)
            nameCell.verticalAlignment = Element.ALIGN_BOTTOM
            nameCell.paddingBottom = 3f
            nameCell.paddingRight = 0f
            nameCell.paddingLeft = 0f
            entryTable.addCell(nameCell)

            // Middle cell: Subtle dotted leader line (dynamic width)
            val leaderCell = PdfPCell()
            leaderCell.border = Rectangle.NO_BORDER
            leaderCell.verticalAlignment = Element.ALIGN_BOTTOM
            leaderCell.paddingBottom = 5f
            leaderCell.paddingLeft = 0f
            leaderCell.paddingRight = 0f
            // Create a dotted line using a custom cell event
            leaderCell.cellEvent = object : PdfPCellEvent {
                override fun cellLayout(cell: PdfPCell, position: Rectangle, canvases: Array<PdfContentByte>) {
                    val cb = canvases[PdfPTable.LINECANVAS]
                    cb.saveState()
                    cb.setColorStroke(Color(180, 180, 180)) // Light gray
                    cb.setLineDash(1f, 2f, 0f) // Dotted pattern
                    cb.setLineWidth(0.5f)
                    val y = position.bottom + 4f
                    val padding = 4f // Small equal padding on both sides
                    cb.moveTo(position.left + padding, y)
                    cb.lineTo(position.right - padding, y)
                    cb.stroke()
                    cb.restoreState()
                }
            }
            entryTable.addCell(leaderCell)

            // Right cell: Language icons
            val iconsCell = PdfPCell()
            iconsCell.border = Rectangle.NO_BORDER
            iconsCell.horizontalAlignment = Element.ALIGN_RIGHT
            iconsCell.verticalAlignment = Element.ALIGN_BOTTOM
            iconsCell.paddingBottom = 2f
            iconsCell.paddingLeft = 0f
            iconsCell.paddingRight = 0f
            
            val iconsParagraph = Paragraph()
            iconsParagraph.alignment = Element.ALIGN_RIGHT
            for (lang in sw.languages) {
                val iconPath = languageIcons[lang]
                if (iconPath != null) {
                    val icon = loadSvgIcon(iconPath, iconSize)
                    if (icon != null) {
                        val chunk = Chunk(icon, 0f, 0f)
                        iconsParagraph.add(chunk)
                        iconsParagraph.add(Chunk(" ")) // minimal spacing between icons
                    }
                }
            }
            iconsCell.addElement(iconsParagraph)
            entryTable.addCell(iconsCell)

            document.add(entryTable)

            val descLine = Paragraph(sw.description, bodyFont)
            descLine.indentationLeft = 15f
            document.add(descLine)

            val urls = mutableListOf<String>()
            sw.repoUrl?.let { urls.add(it) }
            sw.docsUrl?.let { urls.add(it) }
            if (urls.isNotEmpty()) {
                val urlLine = Paragraph(urls.joinToString(" | "), smallFont)
                urlLine.indentationLeft = 15f
                document.add(urlLine)
            }

            document.add(Paragraph("\n"))
        }
    }

    private fun addLanguageLegend(document: Document, software: List<Software>) {
        // Collect all unique languages used
        val usedLanguages = software.flatMap { it.languages }.distinct().sorted()
        
        if (usedLanguages.isEmpty()) return

        val legendParagraph = Paragraph()
        legendParagraph.add(Chunk("Languages: ", bodyBoldFont))
        
        usedLanguages.forEachIndexed { index, lang ->
            val iconPath = languageIcons[lang]
            if (iconPath != null) {
                val icon = loadSvgIcon(iconPath, 9f)
                if (icon != null) {
                    legendParagraph.add(Chunk(icon, 0f, -1f))
                    legendParagraph.add(Chunk(" $lang", smallFont))
                    if (index < usedLanguages.size - 1) {
                        legendParagraph.add(Chunk("   ", smallFont)) // separator
                    }
                }
            } else {
                // No icon available, just show text
                legendParagraph.add(Chunk(lang, smallFont))
                if (index < usedLanguages.size - 1) {
                    legendParagraph.add(Chunk(", ", smallFont))
                }
            }
        }
        
        document.add(legendParagraph)
        document.add(Paragraph("\n"))
    }

    private fun addPublications(document: Document, publications: List<Publication>, scholarStats: ScholarStats) {
        addSectionHeader(document, "Publications")

        // Add citation metrics table
        addCitationMetricsTable(document, scholarStats)

        val inPrep = publications.filter {
            it.journal.contains("In Preparation", ignoreCase = true) ||
                    it.journal.contains("In preparation", ignoreCase = true)
        }
        val published = publications.filter {
            !it.journal.contains("In Preparation", ignoreCase = true) &&
                    !it.journal.contains("In preparation", ignoreCase = true)
        }

        if (inPrep.isNotEmpty()) {
            val subHeader = Paragraph("In preparation", subsectionFont)
            document.add(subHeader)
            document.add(Paragraph("\n"))
            addNumberedPublications(document, inPrep, 1)
        }

        if (published.isNotEmpty()) {
            val subHeader = Paragraph("Research Articles", subsectionFont)
            document.add(subHeader)
            document.add(Paragraph("\n"))
            addNumberedPublications(document, published.sortedByDescending { it.year }, inPrep.size + 1)
        }
    }

    private fun addCitationMetricsTable(document: Document, stats: ScholarStats) {
        // Citation overview header with clickable Google Scholar link
        val scholarUrl = "https://scholar.google.com/citations?user=buYGhlYAAAAJ&hl=en"
        
        val overviewHeader = Paragraph()
        overviewHeader.add(Chunk("Citation Metrics (", subsectionFont))
        
        // Create clickable "Google Scholar" link
        val linkFont = Font(subsectionFont)
        linkFont.color = Color(0, 0, 180) // Blue for links
        linkFont.style = Font.UNDERLINE
        val scholarChunk = Chunk("Google Scholar", linkFont)
        scholarChunk.setAnchor(scholarUrl)
        overviewHeader.add(scholarChunk)
        
        overviewHeader.add(Chunk(")", subsectionFont))
        overviewHeader.spacingAfter = 8f
        document.add(overviewHeader)

        // Create metrics table
        val table = PdfPTable(3)
        table.widthPercentage = 60f
        table.horizontalAlignment = Element.ALIGN_LEFT
        table.setWidths(floatArrayOf(40f, 30f, 30f))

        // Header row
        val headerBg = Color(80, 80, 80)
        
        val metricHeader = PdfPCell(Phrase("Metric", tableHeaderFont))
        metricHeader.backgroundColor = headerBg
        metricHeader.horizontalAlignment = Element.ALIGN_CENTER
        metricHeader.paddingTop = 5f
        metricHeader.paddingBottom = 5f
        table.addCell(metricHeader)

        val allHeader = PdfPCell(Phrase("All", tableHeaderFont))
        allHeader.backgroundColor = headerBg
        allHeader.horizontalAlignment = Element.ALIGN_CENTER
        allHeader.paddingTop = 5f
        allHeader.paddingBottom = 5f
        table.addCell(allHeader)

        val since2020Header = PdfPCell(Phrase("Since 2020", tableHeaderFont))
        since2020Header.backgroundColor = headerBg
        since2020Header.horizontalAlignment = Element.ALIGN_CENTER
        since2020Header.paddingTop = 5f
        since2020Header.paddingBottom = 5f
        table.addCell(since2020Header)

        // Data rows
        addMetricRow(table, "Citations", stats.citations.all.toString(), stats.citations.since2020.toString())
        addMetricRow(table, "h-index", stats.hIndex.all.toString(), stats.hIndex.since2020.toString())
        addMetricRow(table, "i10-index", stats.i10Index.all.toString(), stats.i10Index.since2020.toString())

        document.add(table)
        
        // Last updated note
        val lastUpdated = Paragraph("Last updated: ${formatDate(stats.lastUpdated)}", smallFont)
        lastUpdated.spacingBefore = 4f
        lastUpdated.spacingAfter = 12f
        document.add(lastUpdated)
    }

    private fun addMetricRow(table: PdfPTable, metric: String, all: String, since2020: String) {
        val metricCell = PdfPCell(Phrase(metric, tableBoldFont))
        metricCell.horizontalAlignment = Element.ALIGN_LEFT
        metricCell.paddingTop = 4f
        metricCell.paddingBottom = 4f
        metricCell.paddingLeft = 5f
        metricCell.borderColor = tableBorderColor
        table.addCell(metricCell)

        val allCell = PdfPCell(Phrase(all, tableBodyFont))
        allCell.horizontalAlignment = Element.ALIGN_CENTER
        allCell.paddingTop = 4f
        allCell.paddingBottom = 4f
        allCell.borderColor = tableBorderColor
        table.addCell(allCell)

        val since2020Cell = PdfPCell(Phrase(since2020, tableBodyFont))
        since2020Cell.horizontalAlignment = Element.ALIGN_CENTER
        since2020Cell.paddingTop = 4f
        since2020Cell.paddingBottom = 4f
        since2020Cell.borderColor = tableBorderColor
        table.addCell(since2020Cell)
    }

    private fun formatDate(dateStr: String): String {
        // Format YYYYMMDD to readable date
        return try {
            val year = dateStr.substring(0, 4)
            val month = dateStr.substring(4, 6).toInt()
            val day = dateStr.substring(6, 8)
            val monthNames = listOf("", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December")
            "${monthNames[month]} $day, $year"
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun addNumberedPublications(document: Document, pubs: List<Publication>, startNum: Int) {
        var num = startNum
        for (pub in pubs) {
            val citation = formatCitation(pub)
            val para = Paragraph()
            para.add(Chunk("$num. ", bodyFont))
            para.add(citation)
            para.indentationLeft = 15f
            para.firstLineIndent = -15f
            document.add(para)
            document.add(Paragraph("\n"))
            num++
        }
    }

    private fun formatCitation(pub: Publication): Phrase {
        val phrase = Phrase()

        val authorStr = pub.authors.joinToString(", ") { author ->
            val parts = author.split(" ")
            if (parts.size >= 2) {
                val last = parts.last()
                val initials = parts.dropLast(1).map { "${it.first()}." }.joinToString(" ")
                "$last, $initials"
            } else {
                author
            }
        }
        phrase.add(Chunk(authorStr, bodyFont))
        phrase.add(Chunk(" (${pub.year}). ", bodyFont))
        phrase.add(Chunk(pub.title, bodyFont))
        phrase.add(Chunk(". ", bodyFont))
        phrase.add(Chunk(pub.journal, bodyItalicFont))

        if (pub.volume != null) {
            phrase.add(Chunk(", ${pub.volume}", bodyItalicFont))
            if (pub.pages != null) {
                phrase.add(Chunk("(${pub.pages})", bodyFont))
            }
        } else if (pub.pages != null) {
            phrase.add(Chunk(", ${pub.pages}", bodyFont))
        }

        phrase.add(Chunk(".", bodyFont))

        if (pub.doi != null) {
            phrase.add(Chunk(" https://doi.org/${pub.doi}", bodyFont))
        } else if (pub.url != null) {
            phrase.add(Chunk(" ${pub.url}", bodyFont))
        }

        return phrase
    }

    private fun addPresentations(document: Document, presentations: List<Presentation>) {
        addSectionHeader(document, "Presentations")

        val talks = presentations.filter { it.type == "talk" }.sortedByDescending { it.year }
        val posters = presentations.filter { it.type == "poster" }.sortedByDescending { it.year }

        if (talks.isNotEmpty()) {
            val subHeader = Paragraph("Talks", subsectionFont)
            document.add(subHeader)
            document.add(Paragraph("\n"))
            addNumberedPresentations(document, talks, 1)
        }

        if (posters.isNotEmpty()) {
            val subHeader = Paragraph("Posters", subsectionFont)
            document.add(subHeader)
            document.add(Paragraph("\n"))
            addNumberedPresentations(document, posters, 1)
        }
    }

    private fun addNumberedPresentations(document: Document, presos: List<Presentation>, startNum: Int) {
        var num = startNum
        for (pres in presos) {
            val citation = formatPresentationCitation(pres)
            val para = Paragraph()
            para.add(Chunk("$num. ", bodyFont))
            para.add(citation)
            para.indentationLeft = 15f
            para.firstLineIndent = -15f
            document.add(para)
            document.add(Paragraph("\n"))
            num++
        }
    }

    private fun formatPresentationCitation(pres: Presentation): Phrase {
        val phrase = Phrase()

        val authorStr = pres.authors.joinToString(", ") { author ->
            val parts = author.split(" ")
            if (parts.size >= 2) {
                val last = parts.last()
                val initials = parts.dropLast(1).map { "${it.first()}." }.joinToString(" ")
                "$last, $initials"
            } else {
                author
            }
        }
        phrase.add(Chunk(authorStr, bodyFont))
        phrase.add(Chunk(" (${pres.year}). ", bodyFont))
        phrase.add(Chunk(pres.title, bodyFont))
        phrase.add(Chunk(". ", bodyFont))
        phrase.add(Chunk(pres.venue, bodyItalicFont))
        phrase.add(Chunk(".", bodyFont))

        return phrase
    }
}

/**
 * Page event handler for adding headers and footers
 */
class HeaderFooterPageEvent(
    private val headerText: String,
    private val dateText: String,
    private val baseFont: BaseFont,
    private val accentColor: Color = Color.DARK_GRAY
) : PdfPageEventHelper() {

    override fun onEndPage(writer: PdfWriter, document: Document) {
        val cb = writer.directContent
        val pageSize = document.pageSize

        // Header - only on pages after the first
        if (writer.pageNumber > 1) {
            cb.beginText()
            cb.setFontAndSize(baseFont, 8f)
            cb.setColorFill(Color.DARK_GRAY)
            
            // Header left: title
            cb.showTextAligned(
                PdfContentByte.ALIGN_LEFT,
                headerText,
                document.leftMargin(),
                pageSize.height - 32f,
                0f
            )
            
            // Header right: date
            cb.showTextAligned(
                PdfContentByte.ALIGN_RIGHT,
                dateText,
                pageSize.width - document.rightMargin(),
                pageSize.height - 32f,
                0f
            )
            
            cb.endText()
            
            // Header line
            cb.setColorStroke(accentColor)
            cb.setLineWidth(0.5f)
            cb.moveTo(document.leftMargin(), pageSize.height - 38f)
            cb.lineTo(pageSize.width - document.rightMargin(), pageSize.height - 38f)
            cb.stroke()
        }

        // Footer - page number centered
        cb.beginText()
        cb.setFontAndSize(baseFont, 8f)
        cb.setColorFill(Color.DARK_GRAY)
        cb.showTextAligned(
            PdfContentByte.ALIGN_CENTER,
            "Page ${writer.pageNumber}",
            pageSize.width / 2,
            24f,
            0f
        )
        cb.endText()
    }
}
