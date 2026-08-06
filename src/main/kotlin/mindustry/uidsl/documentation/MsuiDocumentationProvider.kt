package mindustry.uidsl.documentation

import com.intellij.lang.documentation.*
import com.intellij.openapi.editor.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import mindustry.uidsl.lexer.*
import mindustry.uidsl.schema.*

/** Port of the hover provider in extension.js: shows node-type / property docs from the schema. */
class MsuiDocumentationProvider : AbstractDocumentationProvider() {

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        // Our PSI tree is flat leaves (see MsuiPsiParser); the WORD token under the caret is
        // already the element we want to document - no reference resolution needed.
        return contextElement?.takeIf { it.elementType == MsuiTypes.WORD }
    }

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        val word = element.text
        val schema = MsuiSchemaService.getInstance().schema

        schema.nodeTypes[word]?.let { def ->
            val containerNote = when {
                def.container && def.noCells -> " &mdash; container, can hold children (no cells: 'row'/'defaults' don't apply)"
                def.container -> " &mdash; container, can hold children"
                else -> ""
            }
            return "<b>$word</b> (node type)$containerNote"
        }
        schema.properties[word]?.let { def ->
            val desc = def.description?.takeIf { it.isNotBlank() }?.let { "<br/>$it" } ?: ""
            return "<b>$word</b>: ${def.type}$desc"
        }
        return null
    }
}
