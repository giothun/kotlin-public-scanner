package cli

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CliParserTest {
    private val parser = CliParser()

    @Test
    fun `parse minimal arguments`() {
        val args = arrayOf("src")
        val result: CliArgs = parser.parse(args)

        assertEquals("src", result.sourceDir)
        assertEquals(null, result.excludePattern)
        assertEquals(null, result.includePattern)
        assertEquals(false, result.verbose)
    }

    @Test
    fun `parse all options`() {
        val args = arrayOf("src", "-e", "excludePattern", "-i", "includePattern", "-v")
        val result: CliArgs = parser.parse(args)

        assertEquals("src", result.sourceDir)
        assertEquals("excludePattern", result.excludePattern)
        assertEquals("includePattern", result.includePattern)
        assertEquals(true, result.verbose)
    }

    @Test
    fun `fails when missing required argument`() {
        val args = arrayOf<String>()
        assertFailsWith<Exception> {
            parser.parse(args)
        }
    }
}
