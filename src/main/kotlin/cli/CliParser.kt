package cli

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default

data class CliArgs(
    val sourceDir: String,
    val excludePattern: String?,
    val includePattern: String?,
    val verbose: Boolean,
    val concurrent: Boolean,
    val concurrency: Int
)

class CliParser {
    fun parse(args: Array<String>): CliArgs {
        if (args.isEmpty()) {
            printUsage()
            throw IllegalArgumentException("Missing required argument: sourceDir")
        }

        val parser = ArgParser("kotlin-public-scanner")
        val sourceDir by parser.argument(ArgType.String, description = "Source directory to scan")
        val excludePattern by parser.option(ArgType.String, shortName = "e", description = "Regex pattern to exclude files")
        val includePattern by parser.option(ArgType.String, shortName = "i", description = "Regex pattern to include files")
        val verbose by parser.option(ArgType.Boolean, shortName = "v", description = "Enable verbose output").default(false)
        val sequential by parser.option(ArgType.Boolean, shortName = "s", description = "Use sequential processing (slower, but lower memory usage)").default(false)
        val concurrency by parser.option(ArgType.Int, shortName = "j", description = "Number of concurrent threads").default(Runtime.getRuntime().availableProcessors())

        parser.parse(args)
        
        val concurrent = !sequential
        
        return CliArgs(sourceDir, excludePattern, includePattern, verbose, concurrent, concurrency)
    }

    private fun printUsage() {
        println("Usage: kotlin-public-scanner options_list")
        println("Arguments:")
        println("    sourceDir -> Source directory to scan { String }")
        println("Options:")
        println("    --excludePattern, -e -> Regex pattern to exclude files { String }")
        println("    --includePattern, -i -> Regex pattern to include files { String }")
        println("    --verbose, -v [false] -> Enable verbose output")
        println("    --sequential, -s [false] -> Use sequential processing (concurrent mode is default)")
        println("    --concurrency, -j [num_cores] -> Number of concurrent threads")
        println("    --help, -h -> Usage info")
    }
}
