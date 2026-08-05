package mindustry.uidsl.psi

import com.intellij.lang.*
import com.intellij.psi.tree.*

/**
 * Builds a minimal, flat PSI tree: a single root wrapping every token as a leaf.
 *
 * The real grammar (node nesting, key/value pairing, node-type vs. property resolution)
 * mirrors `UiDslParser` closely enough that it's more maintainable to keep it as a single
 * source of truth in [com.anuke.mindustry.uidsl.parser.MsuiDslParser], operating on raw text/
 * offsets - the same approach the reference VSCode extension takes. That semantic parse is
 * used directly by the annotator, completion contributor and documentation provider, so a
 * deep PSI tree (with composite element types per node) isn't needed for this plugin's features.
 */
class MsuiPsiParser : PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val marker = builder.mark()
        while(!builder.eof()) {
            builder.advanceLexer()
        }
        marker.done(root)
        return builder.treeBuilt
    }
}
