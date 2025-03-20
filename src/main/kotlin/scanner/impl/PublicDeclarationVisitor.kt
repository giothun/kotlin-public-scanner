package scanner.impl

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import utils.writeln
import java.io.Writer

class PublicDeclarationVisitor(
    private val writer: Writer,
    private val indentStep: String = "   "
) : KtTreeVisitorVoid() {

    private var currentIndentation = ""

    fun processDeclarations(declarations: List<KtDeclaration>?, currentIndent: String = "") {
        this.currentIndentation = currentIndent
        declarations?.forEach { member ->
            member.accept(this)
        }
    }

    override fun visitClass(klass: KtClass) {
        if (isPublic(klass)) {
            val name = klass.name ?: "<anonymous>"
            val prefix = when {
                klass.isInterface() -> "interface"
                klass.isData() -> "data class"
                klass.isSealed() -> "sealed class"
                klass.isEnum() -> "enum class"
                klass.isInner() -> "inner class"
                klass.isInline() && klass.hasModifier(KtTokens.VALUE_KEYWORD) -> "value class"
                else -> "class"
            }
            writer.writeln("$currentIndentation $prefix $name {")

            klass.primaryConstructor?.let { primaryConstructor ->
                primaryConstructor.valueParameters.forEach { param ->
                    if (isPublic(param)) {
                        val modifiers = if (param.hasValOrVar()) {
                            if (param.isMutable) "var" else "val"
                        } else {
                            "parameter"
                        }
                        val paramName = param.name ?: "<unnamed>"
                        val paramType = param.typeReference?.text ?: "<no type>"
                        writer.writeln("$currentIndentation$indentStep $modifiers $paramName: $paramType")
                    }
                }
            }

            val oldIndent = currentIndentation
            currentIndentation += indentStep
            super.visitClass(klass)
            currentIndentation = oldIndent
            writer.writeln("$currentIndentation}")
        }
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        if (isPublic(declaration)) {
            val name = declaration.name ?: "<anonymous>"
            val prefix = if (declaration.isCompanion()) "companion object" else "object"
            val nameDisplay = if (declaration.isCompanion() && name == "Companion") "" else " $name"
            writer.writeln("$currentIndentation $prefix$nameDisplay {")
            val oldIndent = currentIndentation
            currentIndentation += indentStep
            super.visitObjectDeclaration(declaration)
            currentIndentation = oldIndent
            writer.writeln("$currentIndentation}")
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (isPublic(function)) {
            val name = function.name ?: "<anonymous>"
            val params = function.valueParameters.joinToString(", ") { it.text }

            val receiverType = function.receiverTypeReference?.text?.let { "$it." } ?: ""

            val typeParameters = function.typeParameters.takeIf { it.isNotEmpty() }?.let {
                "<" + it.joinToString(", ") { it.name ?: "" } + ">"
            } ?: ""

            val operatorKeyword = if (function.hasModifier(KtTokens.OPERATOR_KEYWORD)) "operator " else ""

            val suspendKeyword = if (function.hasModifier(KtTokens.SUSPEND_KEYWORD)) "suspend " else ""

            writer.writeln("$currentIndentation $operatorKeyword$suspendKeyword${function.visibility}fun $typeParameters$receiverType$name($params)")
            super.visitNamedFunction(function)
        }
    }

    override fun visitProperty(property: KtProperty) {
        if (isPublic(property)) {
            val name = property.name ?: "<anonymous>"
            val kind = if (property.isVar) "var" else "val"

            val receiverType = property.receiverTypeReference?.text?.let { "$it." } ?: ""

            val initializer = property.delegate?.let { " by [delegate]" } ?: ""

            writer.writeln(
                "$currentIndentation $kind $receiverType$name" +
                        (property.typeReference?.let { ": ${it.text}" } ?: "") + initializer
            )
            super.visitProperty(property)
        }
    }

    override fun visitClassInitializer(initializer: KtClassInitializer) {
        writer.writeln("$currentIndentation init { ... }")
        super.visitClassInitializer(initializer)
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
        if (isPublic(constructor)) {
            val params = constructor.valueParameters.joinToString(", ") { it.text }
            writer.writeln("$currentIndentation secondary constructor($params)")
            super.visitSecondaryConstructor(constructor)
        }
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias) {
        if (isPublic(typeAlias)) {
            val name = typeAlias.name ?: "<anonymous>"
            val targetType = typeAlias.getTypeReference()?.text ?: ""
            writer.writeln("$currentIndentation typealias $name = $targetType")
            super.visitTypeAlias(typeAlias)
        }
    }

    override fun visitEnumEntry(enumEntry: KtEnumEntry) {
        if (isPublic(enumEntry)) {
            val name = enumEntry.name ?: "<anonymous>"
            writer.writeln("$currentIndentation enum entry $name {")
            val oldIndent = currentIndentation
            currentIndentation += indentStep
            super.visitEnumEntry(enumEntry)
            currentIndentation = oldIndent
            writer.writeln("$currentIndentation}")
        }
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
        if (isPublic(accessor)) {
            val accessorType = if (accessor.isGetter) "get()" else "set()"
            writer.writeln("$currentIndentation $accessorType")
            super.visitPropertyAccessor(accessor)
        }
    }

    private fun isPublic(element: KtDeclaration): Boolean {
        return element.hasModifier(KtTokens.PUBLIC_KEYWORD) ||
                (!element.hasModifier(KtTokens.PRIVATE_KEYWORD) &&
                        !element.hasModifier(KtTokens.PROTECTED_KEYWORD) &&
                        !element.hasModifier(KtTokens.INTERNAL_KEYWORD))
    }

    private fun isPublic(parameter: KtParameter): Boolean {
        return !parameter.hasModifier(KtTokens.PRIVATE_KEYWORD) &&
                !parameter.hasModifier(KtTokens.PROTECTED_KEYWORD) &&
                !parameter.hasModifier(KtTokens.INTERNAL_KEYWORD)
    }

    private val KtModifierListOwner.visibility: String
        get() = when {
            hasModifier(KtTokens.PUBLIC_KEYWORD) -> "public "
            hasModifier(KtTokens.PRIVATE_KEYWORD) -> "private "
            hasModifier(KtTokens.PROTECTED_KEYWORD) -> "protected "
            hasModifier(KtTokens.INTERNAL_KEYWORD) -> "internal "
            else -> ""
        }
}
