package mindustry.uidsl.psi

import com.intellij.lang.*
import com.intellij.psi.tree.*

/** Builds a minimal, flat PSI tree: a single root wrapping every token as a leaf. */
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
