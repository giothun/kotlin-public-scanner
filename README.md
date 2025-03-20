# Kotlin Public Scanner

A tool for scanning Kotlin source files and extracting their public API declarations. This tool uses the Kotlin compiler frontend to parse Kotlin files and identify public classes, interfaces, functions, properties, and other declarations.

## Features

- **Public API Extraction**: Scan Kotlin files and extract all public API elements
- **Concurrent Processing**: Uses multi-threaded processing by default for significantly faster scanning
- **Flexible Filtering**: Include/exclude files using regex patterns
- **Command-Line Interface**: Easy-to-use CLI with various configuration options

## Installation

Clone the repository and build the project:

```bash
git clone https://github.com/giothun/kotlin-public-scanner.git
cd kotlin-public-scanner
./gradlew build
```

## Usage

### Basic Usage

```bash
# Scan a directory (uses concurrent processing by default)
./gradlew run --args="path/to/your/kotlin/sources"

# Scan with verbose output
./gradlew run --args="path/to/your/kotlin/sources -v"
```

### Advanced Usage

```bash
# Set custom concurrency level (number of threads)
./gradlew run --args="path/to/your/kotlin/sources -j 4"

# Use sequential processing (slower but uses less memory)
./gradlew run --args="path/to/your/kotlin/sources -s"

# Filter files with patterns
./gradlew run --args="path/to/your/kotlin/sources -i '.*Service.*' -e '.*Test.*'"
```

### Command-Line Options

| Option | Description |
|--------|-------------|
| `-v, --verbose` | Enable verbose output |
| `-s, --sequential` | Use sequential processing (concurrent is default) |
| `-j, --concurrency <n>` | Set number of concurrent threads (default: number of CPU cores) |
| `-i, --includePattern <regex>` | Regex pattern to include files |
| `-e, --excludePattern <regex>` | Regex pattern to exclude files |

## Performance Benchmarks

I've conducted benchmark tests comparing sequential vs. concurrent processing on a large codebase ([Exposed](https://github.com/JetBrains/Exposed) Kotlin SQL library with 371 Kotlin files). Here are the results:

### Test Environment
- 8 CPU cores
- 371 Kotlin files
- macOS Darwin 24.3.0

### Results

| Mode       | Threads | Time (ms) | Files/Second | Speedup |
|------------|---------|-----------|--------------|---------|
| Sequential | 1       | ~1100     | ~330         | 1.00x   |
| Concurrent | 1       | ~265      | ~1400        | 4.24x   |
| Concurrent | 2       | ~212      | ~1750        | 5.20x   |
| Concurrent | 4       | ~197      | ~1880        | 5.70x   |
| Concurrent | 8       | ~190      | ~1950        | 5.90x   |
| Concurrent | 16      | ~170      | ~2170        | 6.50x   |

### Key Findings

- **Significant Speedup**: ~6x faster processing with concurrency (now the default)
- **Efficiency**: Even with just 1-2 threads, concurrent mode is 4-5x faster than sequential

## Implementation Details

The Kotlin Public Scanner uses the Kotlin compiler frontend to parse Kotlin source files into a PSI (Program Structure Interface) tree, then traverses this tree to extract public declarations. It implements the KtTreeVisitorVoid pattern from the Kotlin compiler to identify and process various Kotlin language elements.

Key components:
- `PublicDeclarationVisitor`: Traverses the PSI tree and extracts public declarations
- `PublicDeclarationScanner`: Coordinates the process of scanning files and directories
- `DefaultFileSystem`: Handles file operations including finding and reading Kotlin files

The concurrent implementation uses Kotlin Coroutines to process multiple files simultaneously, significantly improving performance for large codebases.

## Running Benchmarks

To run the performance benchmark on your own codebase:

```bash
./gradlew runBenchmark --args="path/to/your/kotlin/sources output-file.txt"
```

## Tests
To run tests use:
````bash
./gradlew test
````

CI for Github Actions was provided.
