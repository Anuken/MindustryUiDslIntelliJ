package mindustry.uidsl.highlighter

import com.intellij.openapi.fileTypes.*
import com.intellij.openapi.options.colors.*
import mindustry.uidsl.*
import javax.swing.*

class MsuiColorSettingsPage : ColorSettingsPage {

    private val descriptors = arrayOf(
        AttributesDescriptor("Node type", MsuiHighlighterColors.NODE_TYPE),
        AttributesDescriptor("Keyword (row / portrait / landscape)", MsuiHighlighterColors.KEYWORD),
        AttributesDescriptor("Property key", MsuiHighlighterColors.PROPERTY_KEY),
        AttributesDescriptor("String", MsuiHighlighterColors.STRING),
        AttributesDescriptor("Number", MsuiHighlighterColors.NUMBER),
        AttributesDescriptor("Boolean", MsuiHighlighterColors.BOOLEAN),
        AttributesDescriptor("Identifier / bare value", MsuiHighlighterColors.IDENTIFIER),
        AttributesDescriptor("Comment", MsuiHighlighterColors.COMMENT),
        AttributesDescriptor("Braces", MsuiHighlighterColors.BRACES),
        AttributesDescriptor("Colon", MsuiHighlighterColors.COLON)
    )

    override fun getIcon(): Icon = MsuiIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = MsuiSyntaxHighlighter()

    override fun getDemoText(): String = """
        // Example server dialog UI
        background: "dialog"
        margin: 12

        table {
          row
          label: "Enter your name:" {
            style: outlineLabel
            wrap: true
          }
          row
          field: "" {
            id: "nameField"
            hint: "Player name"
            maxLength: 32
            growX: true
          }
          row
          condition: "width >= 400"
          check: "Enable hardcore mode" {
            id: "hardcore"
            checked: false
          }
        }
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): MutableMap<String, com.intellij.openapi.editor.colors.TextAttributesKey>? = null
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = descriptors
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName(): String = "Mindustry UI DSL"
}
