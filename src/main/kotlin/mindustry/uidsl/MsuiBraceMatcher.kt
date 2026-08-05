package mindustry.uidsl

import com.intellij.lang.*
import com.intellij.psi.*
import com.intellij.psi.tree.*
import mindustry.uidsl.lexer.*

class MsuiBraceMatcher : PairedBraceMatcher {
    private val pairs = arrayOf(BracePair(MsuiTypes.LBRACE, MsuiTypes.RBRACE, true))

    override fun getPairs(): Array<BracePair> = pairs

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}
