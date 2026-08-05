package mindustry.uidsl.annotator

import com.intellij.lang.annotation.*
import com.intellij.openapi.editor.colors.*
import com.intellij.openapi.util.*
import com.intellij.psi.*
import mindustry.uidsl.highlighter.*
import mindustry.uidsl.parser.*
import mindustry.uidsl.psi.*
import mindustry.uidsl.schema.*

/**
 * Ported from `validateDocument()` in extension.js (error/warning squiggles) plus semantic
 * highlighting that the flat lexer alone can't express (e.g. telling a node-type keyword
 * apart from a boolean literal or a style name, all of which lex as plain WORD tokens -
 * the same ambiguity the TextMate grammar resolves with regex lookarounds).
 *
 * Runs once per file: the annotator infrastructure visits every element in the tree, but our
 * PSI tree is intentionally flat (see [com.anuke.mindustry.uidsl.psi.MsuiPsiParser]), so we
 * gate on the file root and do the whole-document parse/annotate pass there.
 */
class MsuiAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if(element !is MsuiFile) return

        val text = element.text
        val schema = MsuiSchemaService.getInstance().schema
        val result = MsuiDslParser.parse(text, schema)

        highlightSemantics(result, schema, holder, text.length)

        for(d in result.diagnostics) {
            val start = d.start.coerceIn(0, text.length)
            val end = d.end.coerceIn(start, text.length).let { if(it == start) (start + 1).coerceAtMost(text.length) else it }
            if(start >= end) continue
            val severity = if(d.severity == MsuiDslParser.Severity.ERROR) HighlightSeverity.ERROR else HighlightSeverity.WARNING
            holder.newAnnotation(severity, d.message).range(TextRange(start, end)).create()
        }
    }

    private fun highlightSemantics(
        result: MsuiDslParser.ParseResult,
        schema: MsuiSchema,
        holder: AnnotationHolder,
        textLength: Int
    ) {
        fun paint(tok: MsuiDslParser.Tok, key: TextAttributesKey) {
            val start = tok.start.coerceIn(0, textLength)
            val end = tok.end.coerceIn(start, textLength)
            if(start >= end) return
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(start, end))
                .textAttributes(key)
                .create()
        }

        fun visit(node: MsuiDslParser.Node) {
            if(!node.isRoot) {
                paint(node.keyToken, MsuiHighlighterColors.NODE_TYPE)
            }
            node.shorthandValue?.let { valueTok ->
                if(valueTok.type == MsuiDslParser.TokType.WORD) paint(valueTok, wordValueKey(valueTok.value))
            }
            for(entry in node.entries) {
                when(entry.kind) {
                    MsuiDslParser.EntryKind.ROW -> paint(entry.keyToken, MsuiHighlighterColors.KEYWORD)
                    MsuiDslParser.EntryKind.CHILD -> entry.child?.let { visit(it) }
                    MsuiDslParser.EntryKind.PROP -> {
                        paint(entry.keyToken, MsuiHighlighterColors.PROPERTY_KEY)
                        val valueTok = entry.valueToken
                        if(valueTok != null && valueTok.type == MsuiDslParser.TokType.WORD) {
                            paint(valueTok, wordValueKey(valueTok.value))
                        }
                    }
                }
            }
        }

        visit(result.root)
    }

    private fun wordValueKey(word: String): TextAttributesKey = when(word) {
        "true", "false" -> MsuiHighlighterColors.BOOLEAN
        "portrait", "landscape" -> MsuiHighlighterColors.KEYWORD
        else -> if(word.toFloatOrNull() != null) MsuiHighlighterColors.NUMBER else MsuiHighlighterColors.IDENTIFIER
    }
}
