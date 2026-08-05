package mindustry.uidsl.color

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ElementColorProvider
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import mindustry.uidsl.lexer.MsuiTypes
import mindustry.uidsl.schema.MsuiSchemaService
import java.awt.Color

/**
 * Everything [MsuiColorProvider.setColorTo] needs for one interactive color-picker session,
 * captured up front. See the comment on [MsuiColorProvider.setColorTo] for why: after the
 * first edit, the `element` the platform keeps calling back with is an invalidated PSI leaf,
 * so none of its accessors (`.project`, `.containingFile`, `.textRange`, `.node`, ...) - only
 * `getUserData`/`putUserData`, which just read/write a plain field on the object - are safe
 * to call again.
 */
private class ColorEditSession(val project: Project, val document: Document, val marker: RangeMarker, val quoted: Boolean)

private val COLOR_SESSION_KEY = Key.create<ColorEditSession>("mindustry.uidsl.color.session")

/**
 * Shows an inline color swatch next to the value of a `color: "..."` property (Mindustry's
 * cell color-tint property), and lets the user edit it with the platform's standard color
 * picker by clicking the swatch - the same mechanism CSS/Java hex-color literals use.
 *
 * Our PSI tree is intentionally flat (see [mindustry.uidsl.psi.MsuiPsiParser]): every token
 * is a leaf directly under the file root. So "is this element a color value" is answered by
 * walking backwards over sibling leaves rather than looking at a parent property node.
 *
 * The value can be a hex string (optionally `#`-prefixed, 3/4/6/8 hex digits, matching
 * `Color.valueOf`) or one of arc's built-in named color constants (`"white"`, `"scarlet"`, ...),
 * which are looked up in [mindustry.uidsl.schema.MsuiSchema.namedColors].
 */
class MsuiColorProvider : ElementColorProvider {

    override fun getColorFrom(element: PsiElement): Color? {
        if(!isColorPropertyValue(element)) return null
        val text = valueText(element)
        return parseMsuiColor(text)
            ?: MsuiSchemaService.getInstance().schema.namedColors[text.lowercase()]?.let { parseMsuiColor(it) }
    }

    override fun setColorTo(element: PsiElement, color: Color) {
        val session = element.getUserData(COLOR_SESSION_KEY)?.takeIf { it.marker.isValid }
            ?: run {
                if(!element.isValid) return
                val project = element.project
                val document = PsiDocumentManager.getInstance(project).getDocument(element.containingFile) ?: return
                val range = element.textRange
                val marker = document.createRangeMarker(range.startOffset, range.endOffset)
                val quoted = element.node?.elementType == MsuiTypes.STRING
                ColorEditSession(project, document, marker, quoted).also { element.putUserData(COLOR_SESSION_KEY, it) }
            }

        val hex = formatMsuiColor(color)
        val replacement = if(session.quoted) "\"$hex\"" else hex

        WriteCommandAction.runWriteCommandAction(session.project, "Change Color", null, {
            session.document.replaceString(session.marker.startOffset, session.marker.endOffset, replacement)
            PsiDocumentManager.getInstance(session.project).commitDocument(session.document)
        })
    }

    /** True for a WORD/STRING leaf that's the value token of a `color: <value>` entry. */
    private fun isColorPropertyValue(element: PsiElement): Boolean {
        val type = element.node?.elementType
        if(type != MsuiTypes.WORD && type != MsuiTypes.STRING) return false

        val colon = prevSignificantLeaf(element) ?: return false
        if(colon.node?.elementType != MsuiTypes.COLON) return false

        val key = prevSignificantLeaf(colon) ?: return false
        return key.node?.elementType == MsuiTypes.WORD && key.text == "color"
    }

    private fun prevSignificantLeaf(element: PsiElement): PsiElement? {
        var e = PsiTreeUtil.prevLeaf(element)
        while(e != null && (e is PsiWhiteSpace || e is PsiComment)) {
            e = PsiTreeUtil.prevLeaf(e)
        }
        return e
    }

    private fun valueText(element: PsiElement): String {
        val text = element.text
        return if(element.node?.elementType == MsuiTypes.STRING) text.trim('"') else text
    }
}

private val HEX_DIGITS = "0123456789abcdefABCDEF".toSet()

/** Parses a hex color string the way Mindustry's `Color.valueOf` does: optional leading '#', then 3/4/6/8 hex digits. */
fun parseMsuiColor(raw: String): Color? {
    val hex = raw.removePrefix("#")
    if(hex.length !in intArrayOf(3, 4, 6, 8) || hex.any { it !in HEX_DIGITS }) return null

    val channels = when(hex.length) {
        3, 4 -> hex.map { Integer.parseInt("$it$it", 16) }
        else -> hex.chunked(2).map { Integer.parseInt(it, 16) }
    }
    val r = channels[0]
    val g = channels[1]
    val b = channels[2]
    val a = channels.getOrElse(3) { 255 }
    return Color(r, g, b, a)
}

/** Formats a color back to Mindustry's 8-digit `rrggbbaa` hex form (no leading '#'). */
fun formatMsuiColor(color: Color): String {
    fun byte(v: Int) = v.coerceIn(0, 255).toString(16).padStart(2, '0')
    return byte(color.red) + byte(color.green) + byte(color.blue) + byte(color.alpha)
}