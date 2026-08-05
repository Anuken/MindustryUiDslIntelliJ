package mindustry.uidsl.highlighter

import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.colors.TextAttributesKey.*

object MsuiHighlighterColors {
    @JvmField
    val COMMENT = createTextAttributesKey("MSUI_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    @JvmField
    val STRING = createTextAttributesKey("MSUI_STRING", DefaultLanguageHighlighterColors.STRING)
    @JvmField
    val BRACES = createTextAttributesKey("MSUI_BRACES", DefaultLanguageHighlighterColors.BRACES)
    @JvmField
    val COLON = createTextAttributesKey("MSUI_COLON", DefaultLanguageHighlighterColors.SEMICOLON)
    @JvmField
    val BAD_CHARACTER = createTextAttributesKey("MSUI_BAD_CHARACTER", com.intellij.openapi.editor.colors.CodeInsightColors.WARNINGS_ATTRIBUTES)

    // Semantic keys, applied by MsuiAnnotator (a flat WORD token could be any of these).
    @JvmField
    val NODE_TYPE = createTextAttributesKey("MSUI_NODE_TYPE", DefaultLanguageHighlighterColors.KEYWORD)
    @JvmField
    val KEYWORD = createTextAttributesKey("MSUI_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
    @JvmField
    val PROPERTY_KEY = createTextAttributesKey("MSUI_PROPERTY_KEY", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
    @JvmField
    val BOOLEAN = createTextAttributesKey("MSUI_BOOLEAN", DefaultLanguageHighlighterColors.KEYWORD)
    @JvmField
    val NUMBER = createTextAttributesKey("MSUI_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    @JvmField
    val IDENTIFIER = createTextAttributesKey("MSUI_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
}
