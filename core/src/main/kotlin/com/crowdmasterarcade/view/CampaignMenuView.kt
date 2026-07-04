package com.crowdmasterarcade.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.crowdmasterarcade.model.CampaignStats
import com.crowdmasterarcade.model.LevelDefinition
import com.kotcrab.vis.ui.widget.VisDialog
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton

class CampaignMenuView(
    private var levels: List<LevelDefinition>,
    private var stats: CampaignStats,
    initialSelection: Int,
    private val onPlay: (Int) -> Unit,
    private val onTest: (Int) -> Unit,
    private val onEdit: (Int) -> Unit,
    private val onCreate: () -> Unit,
    private val onDelete: (Int) -> Unit,
    private val onResetHome: () -> Unit,
    private val onExit: () -> Unit
) {
    val stage = Stage(ScreenViewport())
    private lateinit var root: VisTable
    private lateinit var rowsTable: VisTable
    private lateinit var statusLabel: VisLabel
    private lateinit var playButton: VisTextButton
    private lateinit var testButton: VisTextButton
    private lateinit var editButton: VisTextButton
    private lateinit var createButton: VisTextButton
    private lateinit var deleteButton: VisTextButton
    private lateinit var resetButton: VisTextButton
    private lateinit var exitButton: VisTextButton
    private var selectedIndex = initialSelection.coerceIn(0, (levels.size - 1).coerceAtLeast(0))

    init {
        UiRenderer.ensureVisUiLoaded()
        root = VisTable(true)
        rowsTable = VisTable(true)
        statusLabel = VisLabel("")
        playButton = VisTextButton("Play")
        testButton = VisTextButton("Test")
        editButton = VisTextButton("Edit")
        createButton = VisTextButton("Create")
        deleteButton = VisTextButton("Delete")
        resetButton = VisTextButton("Reset Data Home")
        exitButton = VisTextButton("Exit")
        root.setFillParent(true)
        root.top().left().pad(16f)
        stage.addActor(root)
        build()
    }

    fun refresh(levels: List<LevelDefinition>, stats: CampaignStats) {
        this.levels = levels
        this.stats = stats
        selectedIndex = selectedIndex.coerceIn(0, (levels.size - 1).coerceAtLeast(0))
        buildRows()
    }

    fun render() {
        stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    fun moveSelection(delta: Int) {
        if (levels.isEmpty()) return
        selectedIndex = (selectedIndex + delta).coerceIn(0, levels.lastIndex)
        buildRows()
    }

    fun activatePlay() {
        if (selectedLevelAccessible()) onPlay(selectedIndex) else showStatus("Level is locked")
    }

    fun activateTest() {
        if (levels.isNotEmpty()) onTest(selectedIndex)
    }

    fun dispose() {
        stage.dispose()
    }

    private fun build() {
        root.clearChildren()
        root.add(VisLabel("Crowd Master Arcade")).left().colspan(2).padBottom(12f).row()
        root.add(VisLabel("Level    Name                              Points   Total    %   State")).left().colspan(2).row()

        buildRows()
        val scroll = VisScrollPane(rowsTable)
        scroll.setScrollingDisabled(true, false)
        root.add(scroll).colspan(2).left().width(900f).height(320f).padBottom(12f).row()

        val actions = VisTable(true)
        listOf(playButton, testButton, editButton, createButton, deleteButton, resetButton, exitButton).forEach {
            actions.add(it).padRight(8f)
        }
        root.add(actions).left().colspan(2).row()
        root.add(statusLabel).left().colspan(2).padTop(10f)

        playButton.addClickListener { activatePlay() }
        testButton.addClickListener { activateTest() }
        editButton.addClickListener { if (levels.isNotEmpty()) onEdit(selectedIndex) }
        createButton.addClickListener { onCreate() }
        deleteButton.addClickListener { confirmDelete() }
        resetButton.addClickListener { confirmReset() }
        exitButton.addClickListener { onExit() }
    }

    private fun buildRows() {
        rowsTable.clearChildren()
        levels.forEachIndexed { index, level ->
            val record = stats.recordForLevel(index + 1)
            val possible = record?.possiblePoints ?: 0f
            val points = record?.playerPoints ?: 0f
            val percent = if (possible > 0f) points / possible * 100f else 0f
            val accessible = isAccessible(index)
            val row = VisTextButton(rowText(index, level, points, possible, percent, accessible))
            row.isChecked = index == selectedIndex
            row.addClickListener {
                selectedIndex = index
                buildRows()
            }
            rowsTable.add(row).left().width(860f).padBottom(3f).row()
        }
        updateActionState()
    }

    private fun updateActionState() {
        val accessible = selectedLevelAccessible()
        playButton.isDisabled = !accessible
        testButton.isDisabled = levels.isEmpty()
        editButton.isDisabled = levels.isEmpty()
        createButton.isDisabled = false
        deleteButton.isDisabled = levels.isEmpty()
    }

    private fun rowText(
        index: Int,
        level: LevelDefinition,
        points: Float,
        possible: Float,
        percent: Float,
        accessible: Boolean
    ): String =
        "%s %3d  %-32s %7d %7d %4d%%  %s".format(
            if (index == selectedIndex) ">" else " ",
            index + 1,
            level.name.take(32),
            points.toInt(),
            possible.toInt(),
            percent.toInt(),
            rowState(index, accessible)
        )

    private fun rowState(index: Int, accessible: Boolean): String =
        when {
            stats.recordForLevel(index + 1)?.won == true -> "completed"
            accessible -> "available"
            else -> "locked"
        }

    private fun selectedLevelAccessible(): Boolean =
        levels.isNotEmpty() && isAccessible(selectedIndex)

    private fun isAccessible(index: Int): Boolean =
        index == 0 || stats.recordForLevel(index)?.won == true

    private fun showStatus(text: String) {
        statusLabel.setText(text)
    }

    private fun confirmReset() {
        object : VisDialog("Reset Data Home") {
            init {
                text("Delete .crowdmaster and recreate it from packaged assets?")
                button("Cancel")
                button("Reset", true)
            }

            override fun result(obj: Any?) {
                if (obj == true) onResetHome()
            }
        }.show(stage)
    }

    private fun confirmDelete() {
        object : VisDialog("Delete Level") {
            init {
                text("Delete selected level file?")
                button("Cancel")
                button("Delete", true)
            }

            override fun result(obj: Any?) {
                if (obj == true) onDelete(selectedIndex)
            }
        }.show(stage)
    }

    private fun VisTextButton.addClickListener(action: () -> Unit) {
        addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (!isDisabled) action()
            }
        })
    }
}
