package mindustry.uidsl.highlighter

import com.intellij.lexer.*
import com.intellij.openapi.editor.colors.*
import com.intellij.openapi.fileTypes.*
import com.intellij.psi.*
import com.intellij.psi.tree.*
import mindustry.uidsl.lexer.*

/**
 * Handles the token kinds whose color never depends on context (strings, comments, braces,
 * colon). WORD tokens (node types, property keys, booleans, numbers, plain identifiers) are
 * colored by [mindustry.uidsl.annotator.MsuiAnnotator] after a real parse.
 */
class MsuiSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = MsuiLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = when(tokenType) {
        MsuiTypes.STRING -> arrayOf(MsuiHighlighterColors.STRING)
        MsuiTypes.COMMENT -> arrayOf(MsuiHighlighterColors.COMMENT)
        MsuiTypes.LBRACE, MsuiTypes.RBRACE -> arrayOf(MsuiHighlighterColors.BRACES)
        MsuiTypes.COLON -> arrayOf(MsuiHighlighterColors.COLON)
        TokenType.BAD_CHARACTER -> arrayOf(MsuiHighlighterColors.BAD_CHARACTER)
        else -> emptyArray()
    }
}
