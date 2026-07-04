package com.crowdmasterarcade.levelplugin

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class CmaLevelFileType private constructor() : LanguageFileType(CmaLevelLanguage) {
    override fun getName(): String = "Crowd Master Arcade Level"
    override fun getDescription(): String = "Crowd Master Arcade level definition"
    override fun getDefaultExtension(): String = "level"
    override fun getIcon(): Icon? = null

    companion object {
        @JvmField
        val INSTANCE = CmaLevelFileType()
    }
}
