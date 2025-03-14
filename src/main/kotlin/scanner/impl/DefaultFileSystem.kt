package scanner.impl

import scanner.FileSystem
import java.io.File
import java.io.FileNotFoundException

class DefaultFileSystem : FileSystem {
    override fun findKotlinFiles(directory: File, excludePattern: String?, includePattern: String?): List<File> {
        require(
            directory.exists() && directory.isDirectory
        ) { "Provided path '${directory.path}' is not a directory" }

        val excludeRegex = excludePattern?.toRegex()
        val includeRegex = includePattern?.toRegex()
        return directory.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                val relativePath = file.relativeTo(directory).path
                when {
                    excludeRegex?.matches(relativePath) == true -> false
                    includeRegex != null && !includeRegex.matches(relativePath) -> false
                    else -> true
                }
            }
            .toList()

    }

    override fun readFile(file: File): String {
        if (!file.exists()) {
            throw IllegalStateException("File '${file.path}' does not exist")
        }
        try {
            return file.readText()
        } catch (e: FileNotFoundException) {
            throw IllegalStateException("File '${file.path}' could not be read", e)
        }
    }
} 