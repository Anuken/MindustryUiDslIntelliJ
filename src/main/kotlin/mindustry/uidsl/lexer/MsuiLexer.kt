package mindustry.uidsl.lexer

import com.intellij.lexer.*
import com.intellij.psi.*
import com.intellij.psi.tree.*

class MsuiLexer : LexerBase() {
    private lateinit var buffer: CharSequence
    private var bufferEnd: Int = 0
    private var pos: Int = 0

    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var tokenType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        this.pos = startOffset
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokenType
    override fun getTokenStart(): Int = tokenStart
    override fun getTokenEnd(): Int = tokenEnd

    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = bufferEnd

    override fun advance() {
        if(pos >= bufferEnd) {
            tokenType = null
            tokenStart = pos
            tokenEnd = pos
            return
        }

        tokenStart = pos
        val c = buffer[pos]

        when {
            c.isWhitespace() -> {
                while(pos < bufferEnd && buffer[pos].isWhitespace()) pos++
                tokenType = TokenType.WHITE_SPACE
            }

            c == '/' && pos + 1 < bufferEnd && buffer[pos + 1] == '/' -> {
                while(pos < bufferEnd && buffer[pos] != '\n') pos++
                tokenType = MsuiTypes.COMMENT
            }

            c == '{' -> {
                pos++
                tokenType = MsuiTypes.LBRACE
            }

            c == '}' -> {
                pos++
                tokenType = MsuiTypes.RBRACE
            }

            c == ':' -> {
                pos++
                tokenType = MsuiTypes.COLON
            }

            c == '"' -> {
                pos++ // opening quote
                while(pos < bufferEnd && buffer[pos] != '"') {
                    if(buffer[pos] == '\\' && pos + 1 < bufferEnd) pos += 2 else pos++
                }
                if(pos < bufferEnd) pos++ // closing quote, if present
                tokenType = MsuiTypes.STRING
            }

            else -> {
                // bare word/number token: read until whitespace / { / } / : / "
                while(pos < bufferEnd) {
                    val ch = buffer[pos]
                    if(ch.isWhitespace() || ch == '{' || ch == '}' || ch == ':' || ch == '"') break
                    pos++
                }
                if(pos == tokenStart) pos++ // stray character safety net, avoid infinite loop
                tokenType = MsuiTypes.WORD
            }
        }

        tokenEnd = pos
    }
}
