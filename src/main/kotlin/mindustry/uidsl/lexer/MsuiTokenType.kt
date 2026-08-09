package mindustry.uidsl.lexer

import com.intellij.psi.tree.*
import mindustry.uidsl.*

class MsuiTokenType(debugName: String) : IElementType(debugName, MsuiLanguage) {
    override fun toString(): String = "MsuiTokenType." + super.toString()
}

object MsuiTypes {
    @JvmField
    val WORD = MsuiTokenType("WORD")
    @JvmField
    val STRING = MsuiTokenType("STRING")
    @JvmField
    val COLON = MsuiTokenType("COLON")
    @JvmField
    val LBRACE = MsuiTokenType("LBRACE")
    @JvmField
    val RBRACE = MsuiTokenType("RBRACE")
    @JvmField
    val COMMENT = MsuiTokenType("COMMENT")
}
