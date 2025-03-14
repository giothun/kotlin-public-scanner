package scanner

import java.io.File

interface FileSystem {
    fun findKotlinFiles(directory: File, excludePattern: String?, includePattern: String?): List<File>
    fun readFile(file: File): String
} 