package com.crowdmasterarcade

import com.crowdmasterarcade.model.CardOperation
import com.crowdmasterarcade.model.CardTarget
import com.crowdmasterarcade.model.DefaultLevels
import com.crowdmasterarcade.model.LevelCatalog
import com.crowdmasterarcade.model.LevelParseException
import com.crowdmasterarcade.model.LevelTextParser
import com.crowdmasterarcade.model.AppModelFactory
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
        assertEquals(1, level.decorations.size)
        assertEquals("triumphal arch", level.decorations[0].name)
        assertEquals(999999f, level.decorations[0].power)
        assertEquals("assets/triumphal-arch.obj", level.decorations[0].modelPath)
    }

    @Test
    fun parsesDecorationShorthandFields() {
        val level = LevelTextParser.parse(
            """
            name: Decor Test
            decorations:
              - name: triumphal arch, power 999999, x:0, z:95, model: assets/triumphal-arch.obj
            """.trimIndent()
        )

        assertEquals(1, level.decorations.size)
        assertEquals(999999f, level.decorations[0].power)
    }

    @Test
    fun parsesCrlfLevelText() {
        val level = LevelTextParser.parse(DefaultLevels.ravenBendText.replace("\n", "\r\n"))

        assertEquals("The Raven's Bend", level.name)
        assertEquals(2, level.bosses.size)
    }

    @Test
    fun parsesPerObjectModelOverridesAndFactoryFallsBackToLevelModels() {
        val level = LevelTextParser.parse(
            """
            name: Model Override Test
            soldier_model: assets/default-soldier.obj
            boss_model: assets/default-boss.obj
            manpower_card_model: assets/default-manpower-card.obj
            firepower_card_model: assets/default-firepower-card.obj
            cards:
              - op: plus, param: manpower, val: 1, x: 0, z: 10, model: assets/default-firepower-card.obj
              - op: times, param: firepower, val: 2, x: 0, z: 20
            enemy_brigades:
              - effective: 1, strength: 10, x: 0, z: 30, model: assets/default-boss.obj
              - effective: 1, strength: 10, x: 0, z: 40
            bosses:
              - name: Custom Boss, power: 100, x: 0, z: 50, model: assets/default-decoration.obj
              - name: Default Boss, power: 100, x: 0, z: 60
            """.trimIndent()
        )

        assertEquals("assets/default-firepower-card.obj", level.cards[0].modelPath)
        assertEquals(null, level.cards[1].modelPath)
        assertEquals("assets/default-boss.obj", level.enemyBrigades[0].modelPath)
        assertEquals(null, level.enemyBrigades[1].modelPath)
        assertEquals("assets/default-decoration.obj", level.bosses[0].modelPath)
        assertEquals(null, level.bosses[1].modelPath)

        val appModel = AppModelFactory.initAppModel(level)
        assertEquals("assets/default-firepower-card.obj", appModel.cards[0].modelPath)
        assertEquals("assets/default-firepower-card.obj", appModel.cards[1].modelPath)
        assertEquals("assets/default-boss.obj", appModel.enemyBrigades[0].modelPath)
        assertEquals("assets/default-soldier.obj", appModel.enemyBrigades[1].modelPath)
        assertEquals("assets/default-decoration.obj", appModel.bosses[0].modelPath)
        assertEquals("assets/default-boss.obj", appModel.bosses[1].modelPath)
    }

    @Test
    fun loadsLevelsFromFolderInFilenameOrder() {
        val folder = Files.createTempDirectory("crowd-master-levels").toFile()
        folder.resolve("index.txt").writeText("001-first.level")
        folder.resolve("notes.txt").writeText("this is not a level")
        folder.resolve("002-second.level").writeText(DefaultLevels.ravenBendText.replace("The Raven's Bend", "Second"))
        folder.resolve("001-first.level").writeText(DefaultLevels.ravenBendText.replace("The Raven's Bend", "First"))
        folder.resolve("003-third.cma-level").writeText(DefaultLevels.ravenBendText.replace("The Raven's Bend", "Third"))

        val levels = LevelCatalog.loadFromFolder(folder.absolutePath)

        assertEquals(listOf("First", "Second", "Third"), levels.map { it.name })
    }

    @Test
    fun skipsMalformedUserLevelFiles() {
        val folder = Files.createTempDirectory("crowd-master-levels-bad").toFile()
        folder.resolve("001-good.level").writeText(DefaultLevels.ravenBendText.replace("The Raven's Bend", "Good"))
        folder.resolve("002-bad.level").writeText(
            """
            name: Bad
            enemy_brigades:
              - name: missing effective, strength: 10, x: 0, z: 10
            """.trimIndent()
        )

        val levels = LevelCatalog.loadFromFolder(folder.absolutePath)

        assertEquals(listOf("Good"), levels.map { it.name })
    }

    @Test
    fun malformedInlineItemsReportLineAndText() {
        val exception = assertFailsWith<LevelParseException> {
            LevelTextParser.parse(
                """
                name: Bad
                enemy_brigades:
                  - name: missing effective, strength: 10, x: 0, z: 10
                """.trimIndent()
            )
        }

        assertTrue(exception.message.orEmpty().contains("Line 3"))
        assertTrue(exception.message.orEmpty().contains("missing effective"))
    }
}
