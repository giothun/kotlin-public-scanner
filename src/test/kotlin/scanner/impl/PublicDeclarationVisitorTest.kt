package scanner.impl

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.io.StringWriter
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PublicDeclarationVisitorTest {
    private lateinit var disposable: Disposable
    private lateinit var environment: KotlinCoreEnvironment
    private lateinit var psiFactory: KtPsiFactory
    private lateinit var writer: StringWriter
    private lateinit var visitor: PublicDeclarationVisitor

    @BeforeEach
    fun setUp() {
        disposable = Disposer.newDisposable()
        val configuration = CompilerConfiguration()
        environment = KotlinCoreEnvironment.createForProduction(
            disposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        psiFactory = KtPsiFactory(environment.project)
        writer = StringWriter()
        visitor = PublicDeclarationVisitor(writer)
    }

    @AfterEach
    fun tearDown() {
        Disposer.dispose(disposable)
    }

    private fun processCode(code: String): String {
        val ktFile: KtFile = psiFactory.createFile(code)
        visitor.processDeclarations(ktFile.declarations)
        return writer.toString()
    }

    @Test
    @DisplayName("Basic test for public declarations")
    fun `visit public declarations`() {
        val code = """
            package test

            class A {
                init { println("init") }
                constructor(x: Int)
                fun foo() {}
                val bar: Int = 42
                private fun hidden() {}
            }
            
            object O {
                fun baz() {}
            }
            
            typealias Alias = String
            
            enum class E {
                ENTRY1, ENTRY2;
                fun enumMethod() {}
            }
        """.trimIndent()

        val messages = processCode(code)
        assertTrue(messages.contains("class A {"), "Should contain class A")
        assertTrue(messages.contains("init { ... }"), "Should contain initializer block")
        assertTrue(messages.contains("secondary constructor("), "Should contain secondary constructor")
        assertTrue(messages.contains("fun foo("), "Should contain function foo")
        assertTrue(messages.contains("val bar"), "Should contain property bar")
        assertTrue(messages.contains("object O {"), "Should contain object O")
        assertTrue(messages.contains("typealias Alias = String"), "Should contain type alias")
        assertFalse(messages.contains("hidden"), "Should not contain private function")
    }

    @Nested
    @DisplayName("Visibility modifiers tests")
    inner class VisibilityModifiersTest {
        @Test
        @DisplayName("Explicit public modifiers")
        fun `explicit public declarations are included`() {
            val code = """
                public class ExplicitPublicClass
                public fun explicitPublicFunction() {}
                public val explicitPublicProperty = 42
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("class ExplicitPublicClass"), "Should contain explicitly public class")
            assertTrue(messages.contains("fun explicitPublicFunction"), "Should contain explicitly public function")
            assertTrue(messages.contains("val explicitPublicProperty"), "Should contain explicitly public property")
        }
        
        @Test
        @DisplayName("Private declarations are excluded")
        fun `private declarations are excluded`() {
            val code = """
                class Container {
                    private class PrivateClass
                    private fun privateFunction() {}
                    private val privateProperty = 42
                }
                
                private class TopLevelPrivateClass
                private fun topLevelPrivateFunction() {}
                private val topLevelPrivateProperty = 42
            """.trimIndent()
            
            val messages = processCode(code)
            assertFalse(messages.contains("PrivateClass"), "Should not contain private class")
            assertFalse(messages.contains("privateFunction"), "Should not contain private function")
            assertFalse(messages.contains("privateProperty"), "Should not contain private property")
            assertFalse(messages.contains("TopLevelPrivateClass"), "Should not contain top-level private class")
            assertFalse(messages.contains("topLevelPrivateFunction"), "Should not contain top-level private function")
            assertFalse(messages.contains("topLevelPrivateProperty"), "Should not contain top-level private property")
        }
        
        @Test
        @DisplayName("Protected declarations are excluded")
        fun `protected declarations are excluded`() {
            val code = """
                open class Base {
                    protected class ProtectedClass
                    protected open fun protectedFunction() {}
                    protected val protectedProperty = 42
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("class Base"), "Should contain the base class")
            assertFalse(messages.contains("ProtectedClass"), "Should not contain protected class")
            assertFalse(messages.contains("protectedFunction"), "Should not contain protected function")
            assertFalse(messages.contains("protectedProperty"), "Should not contain protected property")
        }
        
        @Test
        @DisplayName("Internal declarations are excluded")
        fun `internal declarations are excluded`() {
            val code = """
                internal class InternalClass
                internal fun internalFunction() {}
                internal val internalProperty = 42
                
                class Container {
                    internal class NestedInternalClass
                    internal fun nestedInternalFunction() {}
                    internal val nestedInternalProperty = 42
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertFalse(messages.contains("InternalClass"), "Should not contain internal class")
            assertFalse(messages.contains("internalFunction"), "Should not contain internal function")
            assertFalse(messages.contains("internalProperty"), "Should not contain internal property")
            assertFalse(messages.contains("NestedInternalClass"), "Should not contain nested internal class")
            assertFalse(messages.contains("nestedInternalFunction"), "Should not contain nested internal function")
            assertFalse(messages.contains("nestedInternalProperty"), "Should not contain nested internal property")
        }
    }
    
    @Nested
    @DisplayName("Complex declarations tests")
    inner class ComplexDeclarationsTest {
        @Test
        @DisplayName("Interface declarations")
        fun `interface declarations are included`() {
            val code = """
                interface SimpleInterface {
                    fun interfaceMethod()
                    val interfaceProperty: String
                }
                
                interface GenericInterface<T> {
                    fun process(item: T)
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("interface SimpleInterface"), "Should contain simple interface")
            assertTrue(messages.contains("fun interfaceMethod"), "Should contain interface method")
            assertTrue(messages.contains("val interfaceProperty"), "Should contain interface property")
            assertTrue(messages.contains("interface GenericInterface"), "Should contain generic interface")
            assertTrue(messages.contains("fun process"), "Should contain generic interface method")
        }
        
        @Test
        @DisplayName("Nested declarations")
        fun `nested declarations are processed correctly`() {
            val code = """
                class Outer {
                    class Nested {
                        fun nestedFunction() {}
                        
                        class DeepNested {
                            val deepProperty = "value"
                        }
                    }
                    
                    object NestedObject {
                        const val CONSTANT = 42
                    }
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("class Outer"), "Should contain outer class")
            assertTrue(messages.contains("class Nested"), "Should contain nested class")
            assertTrue(messages.contains("fun nestedFunction"), "Should contain nested function")
            assertTrue(messages.contains("class DeepNested"), "Should contain deeply nested class")
            assertTrue(messages.contains("val deepProperty"), "Should contain deeply nested property")
            assertTrue(messages.contains("object NestedObject"), "Should contain nested object")
            assertTrue(messages.contains("val CONSTANT"), "Should contain constant in nested object")
        }
        
        @Test
        @DisplayName("Data classes")
        fun `data classes are processed correctly`() {
            val code = """
                data class Person(
                    val name: String,
                    val age: Int,
                    private val ssn: String
                ) {
                    fun isAdult() = age >= 18
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("data class Person"), "Should contain data class")
            assertTrue(messages.contains("val name"), "Should contain public property from constructor")
            assertTrue(messages.contains("val age"), "Should contain public property from constructor")
            assertFalse(messages.contains("ssn"), "Should not contain private property")
            assertTrue(messages.contains("fun isAdult"), "Should contain method in data class")
        }
        
        @Test
        @DisplayName("Sealed classes and interfaces")
        fun `sealed classes and interfaces are processed correctly`() {
            val code = """
                sealed class Result {
                    data class Success(val data: String): Result()
                    class Error(val exception: Exception): Result()
                    object Loading: Result()
                }
                
                sealed interface Event {
                    class Click(val x: Int, val y: Int): Event
                    object Timeout: Event
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("sealed class Result"), "Should contain sealed class")
            assertTrue(messages.contains("data class Success"), "Should contain sealed class subclass")
            assertTrue(messages.contains("class Error"), "Should contain sealed class subclass")
            assertTrue(messages.contains("object Loading"), "Should contain sealed class subobject")
            assertTrue(messages.contains("interface Event"), "Should contain sealed interface")
            assertTrue(messages.contains("class Click"), "Should contain sealed interface implementation")
            assertTrue(messages.contains("object Timeout"), "Should contain sealed interface implementation object")
        }
        
        @Test
        @DisplayName("Extension functions and properties")
        fun `extension functions and properties are processed correctly`() {
            val code = """
                fun String.wordCount(): Int = this.split(" ").size
                val String.lastChar: Char get() = this[length - 1]
                
                class Extensions {
                    fun List<Int>.sum(): Int = this.reduce { acc, i -> acc + i }
                    val List<String>.firstOrEmpty: String get() = firstOrNull() ?: ""
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("fun String.wordCount"), "Should contain extension function")
            assertTrue(messages.contains("val String.lastChar"), "Should contain extension property")
            assertTrue(messages.contains("class Extensions"), "Should contain extensions container class")
            assertTrue(messages.contains("fun List<Int>.sum"), "Should contain class extension function")
            assertTrue(messages.contains("val List<String>.firstOrEmpty"), "Should contain class extension property")
        }
        
        @Test
        @DisplayName("Generic classes and functions")
        fun `generic classes and functions are processed correctly`() {
            val code = """
                class Box<T>(val content: T)
                
                fun <T> identity(value: T): T = value
                
                class GenericProcessor<T, R> {
                    fun process(input: T): R? = null
                    
                    class InnerProcessor<V> {
                        fun <X> transform(input: V, transformer: (V) -> X): X? = null
                    }
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("class Box"), "Should contain generic class")
            assertTrue(messages.contains("fun <T>identity"), "Should contain generic function")
            assertTrue(messages.contains("class GenericProcessor"), "Should contain multi-type generic class")
            assertTrue(messages.contains("fun process"), "Should contain method with generic types")
            assertTrue(messages.contains("class InnerProcessor"), "Should contain nested generic class")
            assertTrue(messages.contains("fun <X>transform"), "Should contain method with generic method type parameter")
        }
        
        @Test
        @DisplayName("Companion objects")
        fun `companion objects are processed correctly`() {
            val code = """
                class WithCompanion {
                    companion object {
                        const val DEFAULT_VALUE = 42
                        fun createDefault(): WithCompanion = WithCompanion()
                        
                        private const val INTERNAL_ID = "secret"
                    }
                }
                
                class NamedCompanion {
                    companion object Factory {
                        fun create(): NamedCompanion = NamedCompanion()
                    }
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("class WithCompanion"), "Should contain class with companion")
            assertTrue(messages.contains("object"), "Should contain companion object")
            assertTrue(messages.contains("val DEFAULT_VALUE"), "Should contain companion constant")
            assertTrue(messages.contains("fun createDefault"), "Should contain companion factory method")
            assertFalse(messages.contains("INTERNAL_ID"), "Should not contain private companion constant")
            
            assertTrue(messages.contains("class NamedCompanion"), "Should contain class with named companion")
            assertTrue(messages.contains("object Factory"), "Should contain named companion object")
            assertTrue(messages.contains("fun create"), "Should contain named companion factory method")
        }
    }
    
    @Nested
    @DisplayName("Edge cases and advanced features")
    inner class EdgeCasesTest {
        @Test
        @DisplayName("Function types and lambdas")
        fun `function types and lambdas are processed correctly`() {
            val code = """
                class Processor {
                    fun process(handler: (String) -> Int) {}
                    val callback: (Int, Int) -> Boolean = { a, b -> a > b }
                }
                
                fun higherOrder(fn: (Int) -> Int): (Int) -> Int {
                    return { x -> fn(x * 2) }
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("class Processor"), "Should contain class")
            assertTrue(messages.contains("fun process"), "Should contain function with function type parameter")
            assertTrue(messages.contains("val callback"), "Should contain property with function type")
            assertTrue(messages.contains("fun higherOrder"), "Should contain higher-order function")
        }
        
        @Test
        @DisplayName("Anonymous objects and functions")
        fun `anonymous objects and functions are processed correctly`() {
            val code = """
                val listener = object {
                    fun onClick() {}
                    val name = "Anonymous Listener"
                }
                
                fun execute(action: () -> Unit = {}) {
                    action()
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("val listener"), "Should contain anonymous object property")
            assertTrue(messages.contains("fun onClick"), "Should contain method in anonymous object")
            assertTrue(messages.contains("val name"), "Should contain property in anonymous object")
            assertTrue(messages.contains("fun execute"), "Should contain function with default lambda argument")
        }
        
        @Test
        @DisplayName("Nested and local functions")
        fun `nested and local functions are processed correctly`() {
            val code = """
                fun outerFunction() {
                    fun localFunction() {}
                    
                    class LocalClass {
                        fun localClassMethod() {}
                    }
                    
                    localFunction()
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("fun outerFunction"), "Should contain outer function")
        }
        
        @Test
        @DisplayName("Variance and type projections")
        fun `variance and type projections are processed correctly`() {
            val code = """
                class Producer<out T>(val item: T)
                class Consumer<in T> {
                    fun consume(item: T) {}
                }
                
                fun copy(from: Producer<out Any>, to: Consumer<in Any>) {}
                
                class Invariant<T> {
                    fun process(items: List<T>) {}
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("class Producer"), "Should contain covariant class")
            assertTrue(messages.contains("class Consumer"), "Should contain contravariant class")
            assertTrue(messages.contains("fun copy"), "Should contain function with type projections")
            assertTrue(messages.contains("class Invariant"), "Should contain invariant generic class")
        }
        
        @Test
        @DisplayName("Value classes")
        fun `value classes are processed correctly`() {
            val code = """
                @JvmInline
                value class EmailAddress(val value: String) {
                    fun isValid(): Boolean = value.contains("@")
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("class EmailAddress"), "Should contain value class")
        }
        
        @Test
        @DisplayName("Destructuring declarations")
        fun `destructuring declarations are processed correctly`() {
            val code = """
                data class Point(val x: Int, val y: Int)
                
                class DestructuringDemo {
                    operator fun component1(): String = "First"
                    operator fun component2(): Int = 42
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("data class Point"), "Should contain data class for destructuring")
            assertTrue(messages.contains("class DestructuringDemo"), "Should contain class with component functions")
            assertTrue(messages.contains("operator fun component1"), "Should contain component1 function")
            assertTrue(messages.contains("operator fun component2"), "Should contain component2 function")
        }
        
        @Test
        @DisplayName("Delegated properties")
        fun `delegated properties are processed correctly`() {
            val code = """
                import kotlin.properties.Delegates
                
                class DelegateDemo {
                    val lazyValue: String by lazy { "Computed" }
                    var observable: Int by Delegates.observable(0) { _, _, _ -> }
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("class DelegateDemo"), "Should contain class with delegated properties")
            assertTrue(messages.contains("val lazyValue"), "Should contain lazy property")
            assertTrue(messages.contains("by [delegate]"), "Should indicate delegated property")
            assertTrue(messages.contains("var observable"), "Should contain observable property")
        }
        
        @Test
        @DisplayName("Operator overloading")
        fun `operator overloading is processed correctly`() {
            val code = """
                class Complex(val real: Double, val imaginary: Double) {
                    operator fun plus(other: Complex): Complex = Complex(0.0, 0.0)
                    operator fun minus(other: Complex): Complex = Complex(0.0, 0.0)
                    operator fun times(other: Complex): Complex = Complex(0.0, 0.0)
                    operator fun unaryMinus(): Complex = Complex(0.0, 0.0)
                }
                
                class Container {
                    operator fun contains(element: String): Boolean = true
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("class Complex"), "Should contain class with operator overloading")
            assertTrue(messages.contains("operator fun plus"), "Should contain plus operator")
            assertTrue(messages.contains("operator fun minus"), "Should contain minus operator")
            assertTrue(messages.contains("operator fun times"), "Should contain times operator")
            assertTrue(messages.contains("operator fun unaryMinus"), "Should contain unary minus operator")
            assertTrue(messages.contains("class Container"), "Should contain class with contains operator")
            assertTrue(messages.contains("operator fun contains"), "Should contain contains operator")
        }
        
        @Test
        @DisplayName("Coroutines and suspend functions")
        fun `coroutines and suspend functions are processed correctly`() {
            val code = """
                class CoroutineDemo {
                    suspend fun fetchData(): String = "Data"
                    
                    suspend fun processWithContext(): List<String> {
                        return listOf("Processed")
                    }
                }
                
                interface AsyncRepository {
                    suspend fun getData(): List<String>
                    suspend fun saveData(data: List<String>)
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("class CoroutineDemo"), "Should contain class with coroutine functions")
            assertTrue(messages.contains("suspend fun fetchData"), "Should contain suspend function")
            assertTrue(messages.contains("suspend fun processWithContext"), "Should contain suspend function with context")
            assertTrue(messages.contains("interface AsyncRepository"), "Should contain interface with suspend functions")
            assertTrue(messages.contains("suspend fun getData"), "Should contain suspend interface method")
            assertTrue(messages.contains("suspend fun saveData"), "Should contain suspend interface method with parameters")
        }
 
        @Test
        @DisplayName("DSL builders")
        fun `kotlin DSL builders are processed correctly`() {
            val code = """
                class HtmlBuilder {
                    fun head(init: HeadBuilder.() -> Unit): HeadBuilder {
                        val head = HeadBuilder()
                        return head
                    }
                    
                    fun body(init: BodyBuilder.() -> Unit): BodyBuilder {
                        val body = BodyBuilder()
                        return body
                    }
                }
                
                class HeadBuilder {
                    fun title(text: String) {}
                    fun meta(name: String, content: String) {}
                }
                
                class BodyBuilder {
                    fun div(classes: String? = null, init: DivBuilder.() -> Unit) {
                        val div = DivBuilder()
                    }
                    
                    fun p(text: String) {}
                }
                
                class DivBuilder {
                    fun span(text: String) {}
                    fun a(href: String, text: String) {}
                }
                
                fun html(init: HtmlBuilder.() -> Unit): HtmlBuilder {
                    val html = HtmlBuilder()
                    return html
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("class HtmlBuilder"), "Should contain builder class")
            assertTrue(messages.contains("fun head"), "Should contain head builder function")
            assertTrue(messages.contains("fun body"), "Should contain body builder function")
            assertTrue(messages.contains("class HeadBuilder"), "Should contain head builder class")
            assertTrue(messages.contains("fun title"), "Should contain title function")
            assertTrue(messages.contains("class BodyBuilder"), "Should contain body builder class")
            assertTrue(messages.contains("fun div"), "Should contain div function")
            assertTrue(messages.contains("fun html"), "Should contain top-level html builder function")
        }

        @Test
        @DisplayName("Type aliases with complex types")
        fun `type aliases with complex types are processed correctly`() {
            val code = """
                typealias StringProcessor = (String) -> String
                typealias StringMap = Map<String, String>
                typealias ComplexHandler<T> = (T, List<T>, Map<String, T>) -> Set<T>
                typealias Predicate<T> = (T) -> Boolean
                
                class ProcessorRegistry {
                    val processors = mutableMapOf<String, StringProcessor>()
                    
                    fun <T> executeHandler(handler: ComplexHandler<T>, item: T, list: List<T>, map: Map<String, T>): Set<T> {
                        return handler(item, list, map)
                    }
                    
                    fun <T> filter(items: List<T>, predicate: Predicate<T>): List<T> {
                        return items.filter(predicate)
                    }
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("typealias StringProcessor"), "Should contain function type alias")
            assertTrue(messages.contains("typealias StringMap"), "Should contain map type alias")
            assertTrue(messages.contains("typealias ComplexHandler"), "Should contain generic complex type alias")
            assertTrue(messages.contains("typealias Predicate"), "Should contain generic predicate type alias")
            assertTrue(messages.contains("class ProcessorRegistry"), "Should contain class using type aliases")
            assertTrue(messages.contains("fun <T>executeHandler"), "Should contain function using complex type alias")
            assertTrue(messages.contains("fun <T>filter"), "Should contain function using predicate type alias")
        }
    }
    
    @Nested
    @DisplayName("Robustness tests")
    inner class RobustnessTest {
        @Test
        @DisplayName("Empty file")
        fun `empty file is processed without errors`() {
            val code = ""
            val messages = processCode(code)
            assertTrue(messages.isEmpty() || messages.isBlank(), "Should produce no output for empty file")
        }
        
        @Test
        @DisplayName("Comments and non-code elements")
        fun `comments and non-code elements are ignored`() {
            val code = """
                // This is a comment
                
                /**
                 * Documentation comment
                 * With multiple lines
                 */
                
                /* Block comment */
                
                class CommentedClass {
                    // Method comment
                    fun method() {}
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("class CommentedClass"), "Should contain the class")
            assertTrue(messages.contains("fun method"), "Should contain the method")
            assertFalse(messages.contains("This is a comment"), "Should not contain comments")
            assertFalse(messages.contains("Documentation comment"), "Should not contain doc comments")
            assertFalse(messages.contains("Block comment"), "Should not contain block comments")
            assertFalse(messages.contains("Method comment"), "Should not contain method comments")
        }
        
        @Test
        @DisplayName("Complex package structure")
        fun `complex package structure is processed correctly`() {
            val code = """
                package com.example.app.feature.module
                
                import kotlin.collections.List
                import kotlin.text.isNotEmpty
                
                class PackagedClass {
                    fun method() {}
                }
            """.trimIndent()
            
            val messages = processCode(code)
            assertTrue(messages.contains("class PackagedClass"), "Should contain the class")
            assertTrue(messages.contains("fun method"), "Should contain the method")
        }
    }
}
