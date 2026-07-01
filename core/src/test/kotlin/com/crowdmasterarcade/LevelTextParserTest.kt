package com.crowdmasterarcade

import com.crowdmasterarcade.model.CardOperation
import com.crowdmasterarcade.model.CardTarget
import com.crowdmasterarcade.model.DefaultLevels
import com.crowdmasterarcade.model.LevelCatalog
import com.crowdmasterarcade.model.LevelTextParser
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class LevelTextParserTest {
    @Test
    fun parsesRavensBendLevelText() {
        val level = LevelTextParser.parse(DefaultLevels.ravenBendText)

        assertEquals("The Raven's Bend", level.name)
        assertEquals(220f, level.roadLength)
        assertEquals(10, level.startingSoldiers)
        assertEquals(768, level.projectilePool)
        assertEquals(80f, level.projectileLength)
        assertEquals("assets/default-soldier.obj", level.modelPaths.soldier)
        assertEquals("assets/default-boss.obj", level.modelPaths.boss)
        assertEquals("assets/default-manpower-card.obj", level.modelPaths.manpowerCard)
        assertEquals("assets/default-firepower-card.obj", level.modelPaths.firepowerCard)
        assertEquals(5, level.cards.size)
        assertEquals(CardOperation.PLUS, level.cards[0].operation)
        assertEquals(CardTarget.MANPOWER, level.cards[0].target)
        assertEquals(CardOperation.TIMES, level.cards[2].operation)
        assertEquals(CardTarget.FIREPOWER, level.cards[2].target)
        assertEquals(2, level.enemyBrigades.size)
        assertEquals("vanguard", level.enemyBrigades[0].name)
        assertEquals(10f, level.enemyBrigades[0].unitStrength)
        assertEquals(12f, level.enemyBrigades[1].unitStrength)
        assertEquals(2, level.bosses.size)
        assertEquals("General Raven", level.bosses[0].name)
        assertEquals(null, level.bosses[1].name)
        assertEquals(380f, level.bosses[1].z)
    }

    @Test
    fun loadsLevelsFromFolderInFilenameOrder() {
        val folder = Files.createTempDirectory("crowd-master-levels").toFile()
        folder.resolve("002-second.level").writeText(DefaultLevels.ravenBendText.replace("The Raven's Bend", "Second"))
        folder.resolve("001-first.level").writeText(DefaultLevels.ravenBendText.replace("The Raven's Bend", "First"))

        val levels = LevelCatalog.loadFromFolder(folder.absolutePath)

        assertEquals(listOf("First", "Second"), levels.map { it.name })
    }
}
