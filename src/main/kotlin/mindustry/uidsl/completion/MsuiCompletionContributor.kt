package mindustry.uidsl.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.*
import com.intellij.patterns.*
import com.intellij.util.*
import com.intellij.util.ui.*
import mindustry.uidsl.*
import mindustry.uidsl.color.parseMsuiColor
import mindustry.uidsl.parser.*
import mindustry.uidsl.schema.*

class MsuiCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(MsuiLanguage),
            MsuiCompletionProvider()
        )
    }
}

private enum class Mode { KEY, VALUE }
private class CompletionContext(val mode: Mode, val enclosingType: String, val key: String?)

private class MsuiCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        // Never autocomplete inside comments - the flat PSI tree puts the caret's dummy
        // identifier straight inside the COMMENT leaf when typing after '//', and none of the
        // key/value suggestions below make sense there.
        if(parameters.position.node?.elementType == mindustry.uidsl.lexer.MsuiTypes.COMMENT) return

        val file = parameters.originalFile
        val text = file.text
        val offset = parameters.offset
        val schema = MsuiSchemaService.getInstance().schema

        // Parse the file with a placeholder removed so a partially-typed identifier at the
        // caret (CompletionInitializationContext already replaces it with the dummy identifier)
        // doesn't itself confuse the enclosing-node lookup.
        val parseResult = MsuiDslParser.parse(text, schema)
        val enclosing = MsuiDslParser.findEnclosingNode(parseResult.root, offset)
        val ctx = completionContext(text, offset, enclosing.type)

        val elements: List<LookupElement> = if(ctx.mode == Mode.VALUE) {
            buildValueCompletions(schema, ctx.enclosingType, ctx.key)
        } else {
            buildKeyCompletions(schema, ctx.enclosingType, enclosing.isRoot)
        }
        result.addAllElements(elements)
    }

    /**
     * Port of `getCompletionContext()`: looks backwards on the current line (and a bit before)
     * for the most recent unmatched ':' since the last '{', '}' to know if we're completing a
     * value, and if so which key it belongs to.
     */
    private fun completionContext(text: String, offset: Int, enclosingType: String): CompletionContext {
        val windowStart = (offset - 200).coerceAtLeast(0)
        val linePrefix = text.substring(windowStart, offset.coerceIn(0, text.length))

        val lastColon = linePrefix.lastIndexOf(':')
        val lastBraceOpen = maxOf(linePrefix.lastIndexOf('{'), linePrefix.lastIndexOf('}'))
        val lastNewlineIsh = maxOf(linePrefix.lastIndexOf('\n'), lastBraceOpen)

        if(lastColon > lastNewlineIsh) {
            val beforeColon = linePrefix.substring(0, lastColon)
            val m = Regex("""([A-Za-z_][A-Za-z0-9_]*)\s*$""").find(beforeColon)
            val key = m?.groupValues?.get(1)
            return CompletionContext(Mode.VALUE, enclosingType, key)
        }

        return CompletionContext(Mode.KEY, enclosingType, null)
    }

    private fun buildKeyCompletions(schema: MsuiSchema, enclosingType: String, enclosingIsRoot: Boolean): List<LookupElement> {
        val items = ArrayList<LookupElement>()
        val allowedProps = MsuiDslParser.allowedPropertiesFor(enclosingType, schema)
        // The root is always the implicit outer `table`, i.e. a container.
        val enclosingIsContainer = enclosingIsRoot || schema.nodeTypes[enclosingType]?.container == true
        // Cell-less containers (e.g. `stack`) don't lay children out via Cells, so `row` and
        // `defaults` - both Cell/Table concepts - are meaningless there and shouldn't be offered.
        val enclosingIsCellless = !enclosingIsRoot && schema.nodeTypes[enclosingType]?.noCells == true

        // Node types (in any form, shorthand value or '{ }' body) only belong inside a container
        // (or the always-container root) - see MsuiDslParser.validateChildNodeContainment.
        // Non-container node types are leaf widgets and don't accept child nodes at all.
        if(enclosingIsContainer) {
            for((name, def) in schema.nodeTypes){
                if((name == "defaults" || name == "space") && enclosingIsCellless) continue

                val supportsShorthand = !def.container && def.properties.isNotEmpty()


                // widgets like `button` can be completed as `button "" { }`
                items.add(
                    LookupElementBuilder.create(name)
                        .withPresentableText(if(supportsShorthand) "$name: \"\" { }" else name)
                        .withTypeText("node type")
                        .withTailText(if(supportsShorthand) "" else " { }", true)
                        .withInsertHandler { ctx, _ -> insertNodeSkeleton(ctx, useBraceForm = true, supportsShorthand = supportsShorthand) }
                )

                if(name == "label"){ //only show shorthand-only completion for labels, for buttons it's useless
                    items.add(
                        LookupElementBuilder.create(name)
                            .withTypeText("node type")
                            .withTailText(": \"\"", true)
                            .withInsertHandler { ctx, _ -> insertNodeSkeleton(ctx, useBraceForm = false) }
                    )
                }
            }
        }

        // `row` only makes sense as a layout break between children of a container that lays
        // them out via Cells - not inside a cell-less container like `stack`.
        if(enclosingIsContainer && !enclosingIsCellless) {
            items.add(
                LookupElementBuilder.create("row")
                    .withTypeText("layout")
                    .withInsertHandler { _, _ -> /* bare keyword, nothing extra to insert */ }
            )
        }

        for(propName in allowedProps) {
            val def = schema.properties[propName] ?: continue
            items.add(
                LookupElementBuilder.create(propName)
                    .withTypeText(def.type)
                    .withTailText(propertyValueHint(schema, enclosingType, def), true)
                    .withInsertHandler { ctx, _ -> insertPropertyColon(ctx) }
            )
        }

        return items
    }

    private fun propertyValueHint(
        schema: MsuiSchema,
        enclosingType: String,
        def: PropertyDef
    ): String = when(def.type) {
        "boolean" -> ": true|false"
        "enum" -> ": " + (def.values?.joinToString("|") ?: "")
        "style" -> {
            val styleTypes = schema.nodeTypes[enclosingType]?.styleTypeNames().orEmpty()
            val names = MsuiDslParser.styleNamesFor(styleTypes, schema)
            if(names.isNotEmpty()) ": " + names.joinToString("|") else ": \"...\""
        }

        "number" -> ": 0"
        else -> ": \"...\""
    }

    private fun buildValueCompletions(schema: MsuiSchema, enclosingType: String, key: String?): List<LookupElement> {
        if(key == null) return emptyList()
        if(key == "color") return buildColorCompletions(schema)
        val def = schema.properties[key] ?: return emptyList()
        val items = ArrayList<LookupElement>()

        when(def.type) {
            "boolean" -> {
                items.add(LookupElementBuilder.create("true"))
                items.add(LookupElementBuilder.create("false"))
            }

            "enum" -> {
                def.values?.forEach { items.add(LookupElementBuilder.create(it)) }
            }

            "condition" -> {
                listOf("portrait", "landscape", "width >= ", "width <= ", "height >= ", "height <= ").forEach {
                    items.add(LookupElementBuilder.create(it))
                }
            }

            "style" -> {
                val styleTypes = schema.nodeTypes[enclosingType]?.styleTypeNames().orEmpty()
                for(styleTypeName in styleTypes) {
                    for(style in schema.styles[styleTypeName].orEmpty()) {
                        items.add(
                            LookupElementBuilder.create(style.name)
                                .withTypeText(styleTypeName)
                                .withTailText(style.description?.let { "  $it" } ?: "", true)
                        )
                    }
                }
            }
        }
        return items
    }

    /** Named built-in colors (`namedColors` in the schema), each shown with its actual swatch. */
    private fun buildColorCompletions(schema: MsuiSchema): List<LookupElement> =
        schema.namedColors.mapNotNull { (name, hex) ->
            val color = parseMsuiColor(hex) ?: return@mapNotNull null
            LookupElementBuilder.create(name)
                .withIcon(ColorIcon(12, color))
                .withTypeText(hex, false)
        }

    /** Types `{ }` (or `: ""` for leaf-typed shorthand) after a node-type key and drops the caret inside. */
    private fun insertNodeSkeleton(ctx: InsertionContext, useBraceForm: Boolean, supportsShorthand: Boolean = false) {
        val editor = ctx.editor
        val document = ctx.document
        val tail = ctx.tailOffset

        if(useBraceForm) {
            val lineNumber = document.getLineNumber(tail)
            val lineStart = document.getLineStartOffset(lineNumber)
            val lineText = document.charsSequence.subSequence(lineStart, tail).toString()
            val currentIndent = lineText.takeWhile{ it == ' ' || it == '\t' }
            val innerIndent = "$currentIndent\t"

            val skeleton = if(supportsShorthand) ": \"\" {\n$innerIndent\n$currentIndent}" else "{\n$innerIndent\n$currentIndent}"
            document.insertString(tail, skeleton)
            ctx.commitDocument()
            editor.caretModel.moveToOffset(tail + (if(supportsShorthand) 7 else 2) + innerIndent.length) // after "{\n" + innerIndent
        } else {
            document.insertString(tail, ": \"\"")
            ctx.commitDocument()
            editor.caretModel.moveToOffset(tail + 3)
        }
    }

    private fun insertPropertyColon(ctx: InsertionContext) {
        val document = ctx.document
        val tail = ctx.tailOffset
        document.insertString(tail, ": ")
        ctx.commitDocument()
        ctx.editor.caretModel.moveToOffset(tail + 2)
        // Immediately re-trigger completion so value suggestions show up, mirroring the
        // VSCode extension's `:` completion trigger character.
        AutoPopupControllerHelper.scheduleAutoPopup(ctx)
    }
}

/** Small indirection so we don't need a hard compile-time dependency shape change if this helper evolves. */
private object AutoPopupControllerHelper {
    fun scheduleAutoPopup(ctx: InsertionContext) {
        com.intellij.codeInsight.AutoPopupController.getInstance(ctx.project).scheduleAutoPopup(ctx.editor)
    }
}