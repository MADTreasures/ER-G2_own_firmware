package ch.madtreasures.g2watch.glasses

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The app never writes firmware. Faceclaw's core, copied unchanged, still contains its flashing
 * flows; this guard fails as soon as app code refers to any of them.
 */
class NoFlashingTest {
    private val flashing = listOf(
        "OtaFlashFlow",
        "FlashPromptFlow",
        "FirmwareImage",
        "FaceclawFirmwareFlasherListener",
        "FaceclawFlashPromptListener",
    )

    @Test
    fun `app code does not use Faceclaw's flashing flows`() {
        // Unit tests run in the module directory.
        val sources = File("src/main").walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }.toList()
        assertTrue("no sources found in ${File("src/main").absolutePath}", sources.size > 10)
        for (file in sources) {
            val text = file.readText()
            for (name in flashing) assertFalse("${file.path} uses $name", text.contains(name))
        }
    }
}
