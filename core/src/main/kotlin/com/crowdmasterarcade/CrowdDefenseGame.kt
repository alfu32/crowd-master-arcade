package com.crowdmasterarcade

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.crowdmasterarcade.controller.GameController
import com.crowdmasterarcade.controller.InputController
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.AppModelFactory
import com.crowdmasterarcade.model.CampaignLevelContext
import com.crowdmasterarcade.model.CampaignStats
import com.crowdmasterarcade.model.GameState
import com.crowdmasterarcade.model.InputState
import com.crowdmasterarcade.model.LevelCatalog
import com.crowdmasterarcade.model.LevelDefinition
import com.crowdmasterarcade.model.ResourceHome
import com.crowdmasterarcade.view.CampaignMenuView
import com.crowdmasterarcade.view.GameView
import com.crowdmasterarcade.view.LevelEditorView

class CrowdDefenseGame : ApplicationAdapter() {
    private lateinit var appModel: AppModel
    private lateinit var levels: List<LevelDefinition>
    private lateinit var controller: GameController
    private lateinit var inputController: InputController
    private lateinit var inputState: InputState
    private var view: GameView? = null
    private var editorView: LevelEditorView? = null
    private lateinit var menuView: CampaignMenuView
    private lateinit var campaignStats: CampaignStats
    private var levelIndex = 0
    private var screen = AppScreen.MENU
    private var campaignMode = true

    override fun create() {
        ResourceHome.initialize()
        levels = LevelCatalog.load()
        campaignStats = CampaignStats()
        levelIndex = campaignStats.lastSelectedLevel(levels.size) - 1
        controller = GameController()
        inputController = InputController()
        inputState = InputState()
        menuView = createMenuView()
        Gdx.input.inputProcessor = menuView.stage
    }

    override fun render() {
        when (screen) {
            AppScreen.MENU -> renderMenu()
            AppScreen.GAME -> renderGame()
            AppScreen.EDITOR -> renderEditor()
        }
    }

    override fun dispose() {
        editorView?.dispose()
        view?.dispose()
        menuView.dispose()
    }

    private fun renderMenu() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.08f, 0.1f, 0.11f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) menuView.moveSelection(-1)
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) menuView.moveSelection(1)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) menuView.activatePlay()
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit()
        menuView.render()
    }

    private fun renderEditor() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            editorView?.let { /* let the editor exit button handle dirty confirmations */ }
        }
        editorView?.render()
    }

    private fun renderGame() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            campaignStats.recordSelectedLevel(levelIndex + 1)
            appModel = loadLevel(levelIndex)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C) && appModel.gameState == GameState.WON) {
            recordCompletionIfNeeded()
            if (!campaignMode) {
                showMenu()
                return
            }
            if (levelIndex + 1 < levels.size) {
                levelIndex += 1
                campaignStats.recordSelectedLevel(levelIndex + 1)
                appModel = loadLevel(levelIndex)
            } else {
                appModel.gameState = GameState.EXIT
                Gdx.app.exit()
            }
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            recordCompletionIfNeeded()
            campaignStats.recordSelectedLevel(levelIndex + 1)
            appModel.gameState = GameState.EXIT
            Gdx.app.exit()
            return
        }

        val previousState = appModel.gameState
        inputController.readInput(inputState)
        controller.changeAppModelState(appModel, inputState, Gdx.graphics.deltaTime)
        if (previousState == GameState.RUNNING && appModel.gameState != GameState.RUNNING) {
            recordCompletionIfNeeded()
        }
        view?.presentAppModel(appModel)
    }

    private fun loadLevel(index: Int): AppModel {
        campaignStats.recordSelectedLevel(index + 1)
        val level = levels[index]
        val levelNumber = index + 1
        val previousTotals = campaignStats.totalsBefore(levelNumber)
        return AppModelFactory.initAppModel(
            level,
            CampaignLevelContext(
                levelNumber = levelNumber,
                totalLevels = levels.size,
                levelPossiblePoints = CampaignLevelContext.possiblePoints(level),
                previousPlayerPoints = previousTotals.playerPoints,
                previousPossiblePoints = previousTotals.possiblePoints
            )
        )
    }

    private fun recordCompletionIfNeeded() {
        if (appModel.completionRecorded || appModel.gameState !in setOf(GameState.WON, GameState.LOST)) return
        campaignStats.record(appModel)
        appModel.completionRecorded = true
    }

    private fun createMenuView(): CampaignMenuView =
        CampaignMenuView(
            levels = levels,
            stats = campaignStats,
            initialSelection = levelIndex,
            onPlay = { index -> startGame(index, campaign = true) },
            onTest = { index -> startGame(index, campaign = false) },
            onEdit = { index -> openEditor(index) },
            onCreate = { createLevel() },
            onDelete = { index -> deleteLevel(index) },
            onResetHome = {
                ResourceHome.resetFromPackagedAssets()
                levels = LevelCatalog.load()
                campaignStats = CampaignStats()
                levelIndex = campaignStats.lastSelectedLevel(levels.size) - 1
                menuView.refresh(levels, campaignStats)
            }
        )

    private fun openEditor(index: Int) {
        val files = LevelCatalog.resourceHomeLevelFiles()
        if (levels.isEmpty() || index !in levels.indices) return
        val target = files.getOrNull(index) ?: LevelCatalog.createResourceHomeLevelFile(levels[index])
        editorView?.dispose()
        editorView = LevelEditorView(
            level = levels[index],
            targetFile = target,
            onExit = { closeEditor() },
            onSaved = { reloadLevels(index) }
        )
        Gdx.input.inputProcessor = editorView?.inputProcessor
        screen = AppScreen.EDITOR
    }

    private fun createLevel() {
        val base = levels.getOrNull(levelIndex) ?: return
        val newLevel = base.copy(name = "New Level ${levels.size + 1}", cards = emptyList(), decorations = emptyList(), enemyBrigades = emptyList(), bosses = emptyList())
        val file = LevelCatalog.createResourceHomeLevelFile(newLevel)
        levels = LevelCatalog.load()
        levelIndex = LevelCatalog.resourceHomeLevelFiles().indexOfFirst { it.path() == file.path() }.coerceAtLeast(0)
        menuView.refresh(levels, campaignStats)
        openEditor(levelIndex)
    }

    private fun deleteLevel(index: Int) {
        val file = LevelCatalog.resourceHomeLevelFiles().getOrNull(index) ?: return
        file.delete()
        reloadLevels((index - 1).coerceAtLeast(0))
    }

    private fun closeEditor() {
        editorView?.dispose()
        editorView = null
        reloadLevels(levelIndex)
        Gdx.input.inputProcessor = menuView.stage
        screen = AppScreen.MENU
    }

    private fun reloadLevels(preferredIndex: Int) {
        levels = LevelCatalog.load()
        levelIndex = preferredIndex.coerceIn(0, (levels.size - 1).coerceAtLeast(0))
        campaignStats.recordSelectedLevel(levelIndex + 1)
        menuView.refresh(levels, campaignStats)
    }

    private fun startGame(index: Int, campaign: Boolean) {
        levelIndex = index.coerceIn(0, levels.lastIndex)
        campaignMode = campaign
        appModel = loadLevel(levelIndex)
        if (view == null) view = GameView()
        Gdx.input.inputProcessor = view?.uiStage
        screen = AppScreen.GAME
    }

    private fun showMenu() {
        campaignStats.recordSelectedLevel(levelIndex + 1)
        campaignStats = CampaignStats()
        menuView.refresh(levels, campaignStats)
        Gdx.input.inputProcessor = menuView.stage
        screen = AppScreen.MENU
    }

    private enum class AppScreen {
        MENU,
        GAME,
        EDITOR
    }
}
