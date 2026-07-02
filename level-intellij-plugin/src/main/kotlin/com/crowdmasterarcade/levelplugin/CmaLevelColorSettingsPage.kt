package com.crowdmasterarcade.levelplugin

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class CmaLevelColorSettingsPage : ColorSettingsPage {
    override fun getIcon(): Icon? = null
    override fun getHighlighter(): SyntaxHighlighter = CmaLevelSyntaxHighlighter()
    override fun getDemoText(): String = DEMO_TEXT
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName(): String = "Crowd Master Arcade Level"

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Key", CmaLevelSyntaxHighlighter.KEY),
            AttributesDescriptor("Object category", CmaLevelSyntaxHighlighter.CATEGORY),
            AttributesDescriptor("Operation enum", CmaLevelSyntaxHighlighter.OPERATION),
            AttributesDescriptor("Param enum", CmaLevelSyntaxHighlighter.PARAM),
            AttributesDescriptor("Number", CmaLevelSyntaxHighlighter.NUMBER),
            AttributesDescriptor("Hex color", CmaLevelSyntaxHighlighter.HEX_COLOR),
            AttributesDescriptor("Path reference", CmaLevelSyntaxHighlighter.PATH),
            AttributesDescriptor("Comment", CmaLevelSyntaxHighlighter.COMMENT),
            AttributesDescriptor("Separator", CmaLevelSyntaxHighlighter.SEPARATOR)
        )

        private const val DEMO_TEXT = """
name: The Raven's Bend
road_length: 220
road_width: 16
soldier_model: assets/default-soldier.obj
player_color: #1FB8EBFF
enemy_color: #D6292EFF

cards:
  - op: plus, param: manpower, val: 15, x: -2, z: 10
  - op: times, param: firepower, val: 5, x: -1.5, z: 20

decorations:
  - name: triumphal arch, power: 999999, x: 0, z: 95, model: assets/triumphal-arch.obj, color: #8C8578FF

enemy_brigades:
  - name: vanguard, effective: 40, strength: 10, x: 2, z: 50, color: #A02020FF

bosses:
  - name: General Raven, power: 800, x: 0, z: 102
"""
    }
}
