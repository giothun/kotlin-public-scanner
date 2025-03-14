package benchmarks

import kotlinx.coroutines.runBlocking
import scanner.impl.DefaultFileSystem
import scanner.impl.PublicDeclarationScanner
import scanner.impl.PublicDeclarationVisitor
import java.io.File
import java.io.FileWriter
import java.io.StringWriter
import kotlin.system.measureTimeMillis

object PerformanceComparison {

    data class TestResult(
        val mode: String,
        val threads: Int,
        val fileCount: Int,
        val timeMs: Long,
        val filesPerSecond: Double
    ) {
        override fun toString(): String =
            "Mode: $mode | Threads: $threads | Files: $fileCount | Time: ${timeMs}ms | " +
                    "Files/second: ${String.format("%.2f", filesPerSecond)}"
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            println("Usage: PerformanceComparison <directory-to-scan> [<output-file>]")
            return
        }

        val directoryPath = args[0]
        val outputFilePath = if (args.size > 1) args[1] else null

        val directory = File(directoryPath)
        if (!directory.exists() || !directory.isDirectory) {
            println("Error: '$directoryPath' is not a valid directory")
            return
        }

        val outputWriter = outputFilePath?.let { FileWriter(it) }

        try {
            println("Performance comparison for directory: $directoryPath")
            println("=" * 80)

            val availableCores = Runtime.getRuntime().availableProcessors()
            println("Available cores: $availableCores")
            println()

            val sequentialResult = runSequentialTest(directory)
            println(sequentialResult)

            val threadCounts = listOf(1, 2, 4, availableCores, availableCores * 2)
                .distinct()
                .sorted()

            val concurrentResults = mutableListOf<TestResult>()
            threadCounts.forEach { threads ->
                val result = runConcurrentTest(directory, threads)
                concurrentResults.add(result)
                println(result)
            }

            println("\nRelative Speedup:")
            println("=" * 80)

            concurrentResults.forEach { result ->
                val speedup = sequentialResult.timeMs.toDouble() / result.timeMs.toDouble()
                println("Threads: ${result.threads} | Speedup: ${String.format("%.2fx", speedup)}")
            }

            outputWriter?.let { writer ->
                writer.write("Performance Comparison Results\n")
                writer.write("=" * 80 + "\n")
                writer.write("Directory: $directoryPath\n")
                writer.write("Available cores: $availableCores\n\n")

                writer.write("Sequential: $sequentialResult\n\n")

                writer.write("Concurrent Results:\n")
                concurrentResults.forEach { result ->
                    writer.write("$result\n")
                }

                writer.write("\nRelative Speedup:\n")
                concurrentResults.forEach { result ->
                    val speedup = sequentialResult.timeMs.toDouble() / result.timeMs.toDouble()
                    writer.write("Threads: ${result.threads} | Speedup: ${String.format("%.2fx", speedup)}\n")
                }
            }

        } finally {
            outputWriter?.close()
        }
    }

    private fun runSequentialTest(directory: File): TestResult {
        val writer = StringWriter()
        val fileSystem = DefaultFileSystem()
        val visitor = PublicDeclarationVisitor(writer)

        val scanner = PublicDeclarationScanner(
            fileSystem = fileSystem,
            writer = writer,
            visitor = visitor,
            verbose = false
        )

        val fileCount = fileSystem.findKotlinFiles(directory, null, null).size

        val timeMs = measureTimeMillis {
            scanner.scanDirectory(directory)
        }

        return TestResult(
            mode = "Sequential",
            threads = 1,
            fileCount = fileCount,
            timeMs = timeMs,
            filesPerSecond = calculateFilesPerSecond(fileCount, timeMs)
        )
    }

    private fun runConcurrentTest(directory: File, threads: Int): TestResult {
        val writer = StringWriter()
        val fileSystem = DefaultFileSystem()
        val visitor = PublicDeclarationVisitor(writer)

        val scanner = PublicDeclarationScanner(
            fileSystem = fileSystem,
            writer = writer,
            visitor = visitor,
            verbose = false,
            maxConcurrency = threads
        )

        val fileCount = fileSystem.findKotlinFiles(directory, null, null).size

        val timeMs = measureTimeMillis {
            runBlocking {
                scanner.scanDirectoryConcurrently(directory)
            }
        }

        return TestResult(
            mode = "Concurrent",
            threads = threads,
            fileCount = fileCount,
            timeMs = timeMs,
            filesPerSecond = calculateFilesPerSecond(fileCount, timeMs)
        )
    }

    private fun calculateFilesPerSecond(fileCount: Int, timeMs: Long): Double {
        return (fileCount.toDouble() / timeMs.toDouble()) * 1000.0
    }

    private operator fun String.times(n: Int): String = this.repeat(n)
} 