package com.crowdmasterarcade.levelplugin

import com.intellij.psi.tree.IElementType

class CmaLevelTokenType(debugName: String) : IElementType(debugName, CmaLevelLanguage)

object CmaLevelTokenTypes {
    val COMMENT = CmaLevelTokenType("COMMENT")
    val KEY = CmaLevelTokenType("KEY")
    val CATEGORY = CmaLevelTokenType("CATEGORY")
    val OPERATION = CmaLevelTokenType("OPERATION")
    val PARAM = CmaLevelTokenType("PARAM")
    val NUMBER = CmaLevelTokenType("NUMBER")
    val HEX_COLOR = CmaLevelTokenType("HEX_COLOR")
    val PATH = CmaLevelTokenType("PATH")
    val VALUE = CmaLevelTokenType("VALUE")
    val SEPARATOR = CmaLevelTokenType("SEPARATOR")
}
