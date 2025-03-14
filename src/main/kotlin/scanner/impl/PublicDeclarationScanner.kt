package scanner.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtPsiFactory
import scanner.FileSystem
import utils.writeln
import java.io.File
import java.io.StringWriter
import java.io.Writer
import kotlin.coroutines.CoroutineContext

class PublicDeclarationScanner(
    private val fileSystem: FileSystem,
    private val writer: Writer,
    private val visitor: PublicDeclarationVisitor,
    private val verbose: Boolean = false,
    private val psiFactory: KtPsiFactory? = null,
    private val maxConcurrency: Int = Runtime.getRuntime().availableProcessors(),
    private val dispatcher: CoroutineContext = Dispatchers.Default
) {
    /**
     * Scan a directory for Kotlin files and process them sequentially.
     * This maintains backward compatibility with the existing implementation.
     */
    fun scanDirectory(sourceDir: File, excludePattern: String? = null, includePattern: String? = null) {
        val ktFiles = fileSystem.findKotlinFiles(sourceDir, excludePattern, includePattern)
        if (ktFiles.isEmpty()) {
            writer.writeln("No Kotlin source files found in the directory")
            return
        }
        if (verbose) {
            writer.writeln("Found ${ktFiles.size} Kotlin files")
        }
        val disposable: Disposable = Disposer.newDisposable()
        try {
            val factory = psiFactory ?: createPsiFactory(disposable)
            ktFiles.forEach { file ->
                processFile(file, factory)
            }
        } finally {
            Disposer.dispose(disposable)
        }
    }

    /**
     * Scan a directory for Kotlin files and process them concurrently.
     * @param sourceDir Directory to scan
     * @param excludePattern Regex pattern to exclude files
     * @param includePattern Regex pattern to include files
     */
    suspend fun scanDirectoryConcurrently(sourceDir: File, excludePattern: String? = null, includePattern: String? = null) {
        val ktFiles = fileSystem.findKotlinFiles(sourceDir, excludePattern, includePattern)
        if (ktFiles.isEmpty()) {
            writer.writeln("No Kotlin source files found in the directory")
            return
        }
        if (verbose) {
            writer.writeln("Found ${ktFiles.size} Kotlin files to process concurrently with $maxConcurrency threads")
        }
        
        val disposable: Disposable = Disposer.newDisposable()
        try {
            val factory = psiFactory ?: createPsiFactory(disposable)
            
            withContext(dispatcher) {
                val processingJobs = ktFiles.map { file ->
                    async {
                        processFileConcurrently(file, factory)
                    }
                }
                processingJobs.awaitAll()
            }
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private fun processFile(file: File, psiFactory: KtPsiFactory) {
        try {
            val content = fileSystem.readFile(file)
            val psiFile = psiFactory.createFile(content)
            printFileHeader(file.absolutePath)
            visitor.processDeclarations(psiFile.declarations, "")
        } catch (ex: Exception) {
            writer.writeln("Error processing file ${file.absolutePath}: ${ex.message}")
        }
    }

    private fun processFileConcurrently(file: File, psiFactory: KtPsiFactory) {
        try {
            val fileWriter = StringWriter()
            val fileVisitor = PublicDeclarationVisitor(fileWriter)
            
            val content = fileSystem.readFile(file)
            val psiFile = psiFactory.createFile(content)
            
            fileVisitor.processDeclarations(psiFile.declarations, "")
            
            synchronized(writer) {
                printFileHeader(file.absolutePath)
                writer.write(fileWriter.toString())
                writer.flush()
            }
        } catch (ex: Exception) {
            synchronized(writer) {
                writer.writeln("Error processing file ${file.absolutePath}: ${ex.message}")
            }
        }
    }

    private fun createPsiFactory(disposable: Disposable): KtPsiFactory {
        val configuration = CompilerConfiguration()
        val environment = KotlinCoreEnvironment.createForProduction(
            disposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        return KtPsiFactory(environment.project)
    }

    private fun printFileHeader(filePath: String) {
        writer.write(System.lineSeparator())
        writer.write("File: $filePath")
        writer.write(System.lineSeparator())
        writer.write("=".repeat(80))
        writer.write(System.lineSeparator())
        writer.flush()
    }
}
