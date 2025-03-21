package scanner

import cli.CliArgs
import kotlinx.coroutines.runBlocking
import scanner.impl.DefaultFileSystem
import scanner.impl.PublicDeclarationScanner
import scanner.impl.PublicDeclarationVisitor
import java.io.File

object ScannerRunner {

    fun run(cliArgs: CliArgs) {
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

        val sourceDir = File(cliArgs.sourceDir)
        
        if (cliArgs.verbose) {
            println("Scanning with ${if (cliArgs.concurrent) "concurrent (${cliArgs.concurrency} threads)" else "sequential (single-threaded)"} processing")
        }

        if (cliArgs.concurrent) {
            runBlocking {
                scanner.scanDirectoryConcurrently(
                    sourceDir,
                    cliArgs.excludePattern,
                    cliArgs.includePattern
                )
            }
        } else {
            scanner.scanDirectory(
                sourceDir,
                cliArgs.excludePattern,
                cliArgs.includePattern
            )
        }
    }
} 