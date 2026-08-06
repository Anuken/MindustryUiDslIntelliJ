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

        for((name, def) in schema.nodeTypes) {
            // Only offer node types that would actually be valid to insert here (see
            // MsuiDslParser.validateChildNodeContainment): inside a non-container, a node type is
            // only sensible in the bare-shorthand-value form (no ' { }' body), and only when the
            // node type itself isn't a container - anything needing a '{ }' body (containers, and
            // leaf types with no properties to shorthand, like `defaults`/`space`) doesn't belong
            // inside a non-container and is skipped.
            val usesBraceForm = def.container || def.properties.isEmpty()
            if(!enclosingIsContainer && (def.container || usesBraceForm)) continue

            val tail = if(usesBraceForm) " { }" else ": \"\""
            items.add(
                LookupElementBuilder.create(name)
                    .withTypeText("node type")
                    .withTailText(tail, true)
                    .withInsertHandler { ctx, _ -> insertNodeSkeleton(ctx, usesBraceForm) }
            )
        }

        // `row` only makes sense as a layout break between children of a container.
        if(enclosingIsContainer) {
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
    private fun insertNodeSkeleton(ctx: InsertionContext, useBraceForm: Boolean) {
        val editor = ctx.editor
        val document = ctx.document
        val tail = ctx.tailOffset

        if(useBraceForm) {
            document.insertString(tail, " {\n\t\n}")
            ctx.commitDocument()
            editor.caretModel.moveToOffset(tail + 3) // right after "\n\t"
        } else {
            document.insertString(tail, ": \"\"")
            ctx.commitDocument()
            editor.caretModel.moveToOffset(tail + 3) // between the quotes
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