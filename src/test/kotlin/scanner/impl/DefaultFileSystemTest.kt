package scanner.impl

import scanner.FileSystem
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.io.File

class DefaultFileSystemTest {
    private val fileSystem: FileSystem = DefaultFileSystem()

    @Test
    fun `find Kotlin files in directory`(@TempDir tempDir: File) {
        val ktFile1 = File(tempDir, "A.kt").apply { writeText("fun A() {}") }
        val ktFile2 = File(tempDir, "B.kt").apply { writeText("class B {}") }
        File(tempDir, "C.txt").apply { writeText("not kotlin") }

        val files = fileSystem.findKotlinFiles(tempDir, excludePattern = null, includePattern = null)
        assertEquals(2, files.size)
        assert(files.contains(ktFile1))
        assert(files.contains(ktFile2))
    }

    @Test
    fun `read file content`(@TempDir tempDir: File) {
        val file = File(tempDir, "A.kt").apply { writeText("fun A() {}") }
        val content = fileSystem.readFile(file)
        assertEquals("fun A() {}", content)
    }

    @Test
    fun `throw exception for non-directory`() {
        val file = File("non_existing_dir")
        assertFailsWith<IllegalArgumentException> {
            fileSystem.findKotlinFiles(file, excludePattern = null, includePattern = null)
        }
    }
}
