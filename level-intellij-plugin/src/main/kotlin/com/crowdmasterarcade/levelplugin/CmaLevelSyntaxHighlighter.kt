package com.crowdmasterarcade.levelplugin

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class CmaLevelSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = CmaLevelLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        pack(attributes[tokenType])

    companion object {
        val KEY = TextAttributesKey.createTextAttributesKey(
            "CMA_LEVEL_KEY",
            DefaultLanguageHighlighterColors.INSTANCE_FIELD
        )
        val CATEGORY = TextAttributesKey.createTextAttributesKey(
            "CMA_LEVEL_CATEGORY",
            DefaultLanguageHighlighterColors.KEYWORD
        )
        val OPERATION = TextAttributesKey.createTextAttributesKey(
            "CMA_LEVEL_OPERATION",
            DefaultLanguageHighlighterColors.STATIC_METHOD
        )
        val PARAM = TextAttributesKey.createTextAttributesKey(
            "CMA_LEVEL_PARAM",
            DefaultLanguageHighlighterColors.CONSTANT
        )
        val NUMBER = TextAttributesKey.createTextAttributesKey(
            "CMA_LEVEL_NUMBER",
            DefaultLanguageHighlighterColors.NUMBER
        )
        val PATH = TextAttributesKey.createTextAttributesKey(
            "CMA_LEVEL_PATH",
            DefaultLanguageHighlighterColors.STRING
        )
        val COMMENT = TextAttributesKey.createTextAttributesKey(
            "CMA_LEVEL_COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT
        )
        val SEPARATOR = TextAttributesKey.createTextAttributesKey(
            "CMA_LEVEL_SEPARATOR",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
        )
        val BAD_CHARACTER = TextAttributesKey.createTextAttributesKey(
            "CMA_LEVEL_BAD_CHARACTER",
            HighlighterColors.BAD_CHARACTER
        )

        private val attributes = mapOf(
            CmaLevelTokenTypes.KEY to KEY,
            CmaLevelTokenTypes.CATEGORY to CATEGORY,
            CmaLevelTokenTypes.OPERATION to OPERATION,
            CmaLevelTokenTypes.PARAM to PARAM,
            CmaLevelTokenTypes.NUMBER to NUMBER,
            CmaLevelTokenTypes.PATH to PATH,
            CmaLevelTokenTypes.COMMENT to COMMENT,
            CmaLevelTokenTypes.SEPARATOR to SEPARATOR,
            TokenType.BAD_CHARACTER to BAD_CHARACTER
        )
    }
}
