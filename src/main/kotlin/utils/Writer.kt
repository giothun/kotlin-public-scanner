package utils

import java.io.Writer

fun Writer.writeln(message: String) {
    write(message)
    write(System.lineSeparator())
    flush()
}
