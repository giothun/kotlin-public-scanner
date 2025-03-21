import cli.CliParser
import scanner.ScannerRunner
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    try {
        val cliArgs = CliParser().parse(args)
        ScannerRunner.run(cliArgs)
    } catch (e: Exception) {
        e.printStackTrace()
        exitProcess(1)
    }
}