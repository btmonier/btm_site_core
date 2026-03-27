package org.btmonier.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import java.io.File

class GenerateCv : CliktCommand(
    name = "generate-cv",
    help = "Generate a PDF curriculum vitae from JSON data files"
) {
    private val output: String by option(
        "-o", "--output",
        help = "Output PDF file path"
    ).default("btmonier_cv.pdf")

    private val dataDir: String by option(
        "-d", "--data",
        help = "Directory containing JSON data files"
    ).default("src/commonMain/resources/content")

    override fun run() {
        val dataDirFile = File(dataDir)
        if (!dataDirFile.exists() || !dataDirFile.isDirectory) {
            echo("Error: Data directory '$dataDir' does not exist", err = true)
            return
        }

        echo("Generating CV from data in: $dataDir")
        echo("Output file: $output")

        try {
            val generator = CvPdfGenerator(dataDir)
            generator.generate(output)
            echo("CV generated successfully: $output")
        } catch (e: Exception) {
            echo("Error generating CV: ${e.message}", err = true)
            e.printStackTrace()
        }
    }
}

fun main(args: Array<String>) = GenerateCv().main(args)

