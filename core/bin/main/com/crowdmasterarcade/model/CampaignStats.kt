package com.crowdmasterarcade.model

import com.badlogic.gdx.files.FileHandle
import java.util.Locale

class CampaignStats(
    private val file: FileHandle = ResourceHome.root.child("campaign-stats.tsv"),
    private val stateFile: FileHandle = ResourceHome.root.child("campaign-state.properties")
) {
    private val records = linkedMapOf<Int, LevelScoreRecord>()
    private var lastSelectedLevel = 1

    init {
        load()
        loadState()
    }

    fun totalsBefore(levelNumber: Int): ScoreTotals {
        val completed = records.values.filter { it.levelNumber < levelNumber }
        return ScoreTotals(
            playerPoints = completed.sumOf { it.playerPoints.toDouble() }.toFloat(),
            possiblePoints = completed.sumOf { it.possiblePoints.toDouble() }.toFloat()
        )
    }

    fun recordForLevel(levelNumber: Int): LevelScoreRecord? =
        records[levelNumber]

    fun allRecords(): List<LevelScoreRecord> =
        records.values.sortedBy { it.levelNumber }

    fun record(appModel: AppModel) {
        val levelData = appModel.levelData
        val previous = records[levelData.levelNumber]
        val playerPoints = maxOf(previous?.playerPoints ?: 0f, appModel.scoreData.levelPoints)
        val won = previous?.won == true || appModel.gameState == GameState.WON
        records[levelData.levelNumber] = LevelScoreRecord(
            levelNumber = levelData.levelNumber,
            totalLevels = levelData.totalLevels,
            levelName = levelData.name,
            won = won,
            playerPoints = playerPoints,
            possiblePoints = appModel.scoreData.levelPossiblePoints,
            totalPlayerPoints = appModel.scoreData.totalPlayerPoints,
            totalPossiblePointsSoFar = appModel.scoreData.totalPossiblePointsSoFar
        )
        save()
    }

    fun recordSelectedLevel(levelNumber: Int) {
        lastSelectedLevel = levelNumber.coerceAtLeast(1)
        saveState()
    }

    fun lastSelectedLevel(totalLevels: Int): Int =
        lastSelectedLevel.coerceIn(1, totalLevels.coerceAtLeast(1))

    private fun load() {
        if (!file.exists()) return
        file.readString("UTF-8")
            .lineSequence()
            .drop(1)
            .mapNotNull(::parseRecord)
            .forEach { records[it.levelNumber] = it }
    }

    private fun save() {
        file.parent().mkdirs()
        val text = buildString {
            appendLine("level_number\ttotal_levels\tlevel_name\twon\tplayer_points\tpossible_points\ttotal_player_points\ttotal_possible_points_so_far")
            records.values.sortedBy { it.levelNumber }.forEach { record ->
                append(record.levelNumber).append('\t')
                append(record.totalLevels).append('\t')
                append(record.levelName.replace('\t', ' ')).append('\t')
                append(record.won).append('\t')
                append(format(record.playerPoints)).append('\t')
                append(format(record.possiblePoints)).append('\t')
                append(format(record.totalPlayerPoints)).append('\t')
                append(format(record.totalPossiblePointsSoFar)).append('\n')
            }
        }
        file.writeString(text, false, "UTF-8")
    }

    private fun parseRecord(line: String): LevelScoreRecord? {
        val columns = line.split('\t')
        if (columns.size < 8) return null
        return LevelScoreRecord(
            levelNumber = columns[0].toIntOrNull() ?: return null,
            totalLevels = columns[1].toIntOrNull() ?: return null,
            levelName = columns[2],
            won = columns[3].toBooleanStrictOrNull() ?: false,
            playerPoints = columns[4].toFloatOrNull() ?: 0f,
            possiblePoints = columns[5].toFloatOrNull() ?: 0f,
            totalPlayerPoints = columns[6].toFloatOrNull() ?: 0f,
            totalPossiblePointsSoFar = columns[7].toFloatOrNull() ?: 0f
        )
    }

    private fun format(value: Float): String =
        String.format(Locale.US, "%.2f", value)

    private fun loadState() {
        if (!stateFile.exists()) return
        stateFile.readString("UTF-8")
            .lineSequence()
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) null else line.substring(0, separator).trim() to line.substring(separator + 1).trim()
            }
            .firstOrNull { it.first == "last_selected_level" }
            ?.second
            ?.toIntOrNull()
            ?.let { lastSelectedLevel = it }
    }

    private fun saveState() {
        stateFile.parent().mkdirs()
        stateFile.writeString("last_selected_level=$lastSelectedLevel\n", false, "UTF-8")
    }
}

data class ScoreTotals(
    val playerPoints: Float,
    val possiblePoints: Float
)

data class LevelScoreRecord(
    val levelNumber: Int,
    val totalLevels: Int,
    val levelName: String,
    val won: Boolean,
    val playerPoints: Float,
    val possiblePoints: Float,
    val totalPlayerPoints: Float,
    val totalPossiblePointsSoFar: Float
)
