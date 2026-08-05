package mindustry.uidsl.psi

import com.intellij.extapi.psi.*
import com.intellij.lang.*
import com.intellij.lexer.*
import com.intellij.openapi.project.*
import com.intellij.psi.*
import com.intellij.psi.tree.*
import mindustry.uidsl.*
import mindustry.uidsl.lexer.*

class MsuiParserDefinition : ParserDefinition {

    companion object {
        val FILE = IFileElementType(MsuiLanguage)
        val WHITESPACES: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)
        val COMMENTS: TokenSet = TokenSet.create(MsuiTypes.COMMENT)
        val STRINGS: TokenSet = TokenSet.create(MsuiTypes.STRING)
    }

    override fun createLexer(project: Project?): Lexer = MsuiLexer()
    override fun createParser(project: Project?): PsiParser = MsuiPsiParser()
    override fun getFileNodeType() = FILE
    override fun getWhitespaceTokens(): TokenSet = WHITESPACES
    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getStringLiteralElements(): TokenSet = STRINGS

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)

    override fun createFile(viewProvider: FileViewProvider): MsuiFile = MsuiFile(viewProvider)
}
