import cli.CliParser
import kotlinx.coroutines.runBlocking
import scanner.impl.DefaultFileSystem
import scanner.impl.PublicDeclarationScanner
import scanner.impl.PublicDeclarationVisitor
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    try {
        val cliArgs = CliParser().parse(args)
        val writer = System.out.writer()
        val fileSystem = DefaultFileSystem()
        val visitor = PublicDeclarationVisitor(writer)

        val scanner = PublicDeclarationScanner(
            fileSystem = fileSystem,
            writer = writer,
            visitor = visitor,
            verbose = cliArgs.verbose,
            maxConcurrency = cliArgs.concurrency
        )

        if (cliArgs.concurrent) {
            if (cliArgs.verbose) {
                println("Scanning with concurrent processing (${cliArgs.concurrency} threads)")
            }

            runBlocking {
                scanner.scanDirectoryConcurrently(
                    File(cliArgs.sourceDir),
                    cliArgs.excludePattern,
                    cliArgs.includePattern
                )
            }
        } else {
            if (cliArgs.verbose) {
                println("Scanning with sequential processing (single-threaded)")
            }

            scanner.scanDirectory(
                File(cliArgs.sourceDir),
                cliArgs.excludePattern,
                cliArgs.includePattern
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        exitProcess(1)
    }
}