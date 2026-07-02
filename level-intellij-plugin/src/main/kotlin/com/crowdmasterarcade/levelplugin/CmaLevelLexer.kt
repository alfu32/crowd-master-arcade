package com.crowdmasterarcade.levelplugin

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class CmaLevelLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var endOffset: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var tokenType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        tokenStart = startOffset
        tokenEnd = startOffset
        advance()
    }

    override fun getState(): Int = 0
    override fun getTokenType(): IElementType? = tokenType
    override fun getTokenStart(): Int = tokenStart
    override fun getTokenEnd(): Int = tokenEnd
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = endOffset

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= endOffset) {
            tokenType = null
            return
        }

        val current = buffer[tokenStart]
        when {
            current.isWhitespace() -> readWhile { it.isWhitespace() }.also { tokenType = TokenType.WHITE_SPACE }
            current == '#' && tokenStart + 1 < endOffset && buffer[tokenStart + 1] == ' ' ->
                readUntilLineEnd().also { tokenType = CmaLevelTokenTypes.COMMENT }
            current == '#' -> readHexColor()
            current in ":,()" -> readOne(CmaLevelTokenTypes.SEPARATOR)
            current == '-' && isListMarker(tokenStart) -> readOne(CmaLevelTokenTypes.SEPARATOR)
            isNumberStart(tokenStart) -> readNumber()
            else -> readWord()
        }
    }

    private fun readOne(type: IElementType) {
        tokenEnd = tokenStart + 1
        tokenType = type
    }

    private fun readWhile(predicate: (Char) -> Boolean) {
        var index = tokenStart
        while (index < endOffset && predicate(buffer[index])) index++
        tokenEnd = index
    }

    private fun readUntilLineEnd() {
        var index = tokenStart
        while (index < endOffset && buffer[index] != '\n' && buffer[index] != '\r') index++
        tokenEnd = index
    }

    private fun readNumber() {
        var index = tokenStart
        if (buffer[index] == '+' || buffer[index] == '-') index++
        var dotSeen = false
        while (index < endOffset) {
            val char = buffer[index]
            if (char.isDigit()) {
                index++
                continue
            }
            if (char == '.' && !dotSeen) {
                dotSeen = true
                index++
                continue
            }
            break
        }
        if (index < endOffset && (buffer[index] == 'f' || buffer[index] == 'F')) index++
        tokenEnd = index
        tokenType = CmaLevelTokenTypes.NUMBER
    }

    private fun readHexColor() {
        var index = tokenStart + 1
        while (index < endOffset && buffer[index].isHexDigit()) index++
        tokenEnd = index.coerceAtLeast(tokenStart + 1)
        val digits = tokenEnd - tokenStart - 1
        tokenType = if (digits == 6 || digits == 8) CmaLevelTokenTypes.HEX_COLOR else TokenType.BAD_CHARACTER
    }

    private fun readWord() {
        var index = tokenStart
        while (index < endOffset && !buffer[index].isWhitespace() && buffer[index] !in ":,()#") index++
        tokenEnd = index

        val text = buffer.subSequence(tokenStart, tokenEnd).toString()
        val canonical = text.canonical()
        tokenType = when {
            isCategoryDeclaration(canonical) -> CmaLevelTokenTypes.CATEGORY
            isKeyToken() -> CmaLevelTokenTypes.KEY
            canonical in operations -> CmaLevelTokenTypes.OPERATION
            canonical in params -> CmaLevelTokenTypes.PARAM
            isPathToken(text) -> CmaLevelTokenTypes.PATH
            else -> CmaLevelTokenTypes.VALUE
        }
    }

    private fun isListMarker(index: Int): Boolean {
        if (index + 1 >= endOffset || !buffer[index + 1].isWhitespace()) return false
        var cursor = index - 1
        while (cursor >= 0 && buffer[cursor] != '\n' && buffer[cursor] != '\r') {
            if (!buffer[cursor].isWhitespace()) return false
            cursor--
        }
        return true
    }

    private fun isNumberStart(index: Int): Boolean {
        val char = buffer[index]
        if (char.isDigit()) return true
        if ((char == '+' || char == '-') && index + 1 < endOffset && buffer[index + 1].isDigit()) return true
        return false
    }

    private fun isKeyToken(): Boolean {
        var cursor = tokenEnd
        while (cursor < endOffset && buffer[cursor].isWhitespace() && !isLineBreak(buffer[cursor])) cursor++
        return cursor < endOffset && buffer[cursor] == ':'
    }

    private fun isCategoryDeclaration(canonical: String): Boolean {
        if (canonical !in categories) return false
        var cursor = tokenEnd
        while (cursor < endOffset && buffer[cursor].isWhitespace() && !isLineBreak(buffer[cursor])) cursor++
        if (cursor >= endOffset || buffer[cursor] != ':') return false
        cursor++
        while (cursor < endOffset && buffer[cursor].isWhitespace() && !isLineBreak(buffer[cursor])) cursor++
        return cursor >= endOffset || isLineBreak(buffer[cursor]) || buffer[cursor] == '#'
    }

    private fun isPathToken(text: String): Boolean =
        text.contains('/') || text.contains('\\') || text.endsWith(".obj", ignoreCase = true)

    private fun isLineBreak(char: Char): Boolean = char == '\n' || char == '\r'

    private fun Char.isHexDigit(): Boolean =
        isDigit() || this in 'a'..'f' || this in 'A'..'F'

    private fun String.canonical(): String =
        lowercase().replace("-", "_").replace(" ", "_")

    companion object {
        private val operations = setOf("plus", "minus", "div", "times")
        private val params = setOf("manpower", "firepower")
        private val categories = setOf("cards", "decorations", "enemy_brigades", "bosses", "enemies", "boss")
    }
}
