package com.crowdmasterarcade.view

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.crowdmasterarcade.model.AppModel
import com.crowdmasterarcade.model.AppModelFactory
import com.crowdmasterarcade.model.BackgroundDecorationDefinition
import com.crowdmasterarcade.model.BossDefinition
import com.crowdmasterarcade.model.CardDefinition
import com.crowdmasterarcade.model.CardOperation
import com.crowdmasterarcade.model.CardTarget
import com.crowdmasterarcade.model.DecorationDefinition
import com.crowdmasterarcade.model.EnemyBrigadeDefinition
import com.crowdmasterarcade.model.LevelColor
import com.crowdmasterarcade.model.LevelColors
import com.crowdmasterarcade.model.LevelDefinition
import com.crowdmasterarcade.model.LevelModelPaths
import com.crowdmasterarcade.model.LevelTextParser
import com.crowdmasterarcade.model.LevelTextWriter
import com.crowdmasterarcade.model.ModelFootprintCatalog
import com.crowdmasterarcade.model.ResourceHome
import com.crowdmasterarcade.config.GameConfig
import com.kotcrab.vis.ui.widget.VisDialog
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.color.ColorPicker
import com.kotcrab.vis.ui.widget.color.ColorPickerAdapter
import com.kotcrab.vis.ui.widget.file.FileChooser
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter
import kotlin.math.abs

class LevelEditorView(
    level: LevelDefinition,
    private val targetFile: FileHandle,
    private val onExit: () -> Unit,
    private val onSaved: () -> Unit
) {
    val stage = Stage(ScreenViewport())
    private val worldRenderer = WorldRenderer()
    val inputProcessor = InputMultiplexer(EditorInput(), stage)
    private val root = VisTable()
    private val properties = VisTable(true)
    private val statusLabel = VisLabel("")
    private val saveButton = VisTextButton("Save")
    private val undoButton = VisTextButton("Undo")
    private val redoButton = VisTextButton("Redo")
    private val deleteButton = VisTextButton("Delete Selected")
    private val exitButton = VisTextButton("Exit")
    private var draft = EditableLevel.from(level)
    private var selected: EditorSelection = EditorSelection.Scene
    private var pendingPrototype: EditorPrototype? = null
    private var previewModel: AppModel = preview()
    private var previewRebuildDelay = -1f
    private var dirty = false
    private val undo = mutableListOf<String>()
    private val redo = mutableListOf<String>()

    init {
        UiRenderer.ensureVisUiLoaded()
        root.setFillParent(true)
        root.top().left()
        stage.addActor(root)
        rebuildUi()
        rebuildProperties()
    }

    fun render() {
        applyPendingPreviewRebuild()
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.13f, 0.15f, 0.16f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        worldRenderer.renderEditor(
            previewModel,
            sceneViewportX(),
            sceneViewportY(),
            sceneViewportWidth(),
            sceneViewportHeight(),
            selectedBox()
        )
        if (!textFieldFocused() && (Gdx.input.isKeyJustPressed(Input.Keys.DEL) || Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL))) {
            deleteSelected()
        }
        stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    fun dispose() {
        worldRenderer.dispose()
        stage.dispose()
    }

    private fun rebuildUi() {
        root.clearChildren()
        val commandBar = VisTable(true)
        listOf(saveButton, undoButton, redoButton, deleteButton, exitButton).forEach { commandBar.add(it).padRight(8f) }
        commandBar.add(statusLabel).left().expandX().fillX()
        root.add(commandBar).left().colspan(3).expandX().fillX().pad(8f).row()

        root.add(VisScrollPane(properties)).top().left().width(330f).expandY().fillY().padLeft(8f).padBottom(8f)
        root.add().expand().fill()
        root.add(prototypePanel()).top().right().width(260f).expandY().fillY().padRight(8f).padBottom(8f).row()

        saveButton.addClickListener { save() }
        undoButton.addClickListener { restoreFromStack(undo, redo) }
        redoButton.addClickListener { restoreFromStack(redo, undo) }
        deleteButton.addClickListener { deleteSelected() }
        exitButton.addClickListener { requestExit() }
        updateButtons()
    }

    private fun prototypePanel(): VisTable {
        val table = VisTable(true)
        table.add(VisLabel("Prototypes")).left().row()
        prototypeButton(table, "Enemy brigade", EditorPrototype.Enemy)
        prototypeButton(table, "Manpower card", EditorPrototype.Card(CardTarget.MANPOWER))
        prototypeButton(table, "Firepower card", EditorPrototype.Card(CardTarget.FIREPOWER))
        prototypeButton(table, "Bullet power card", EditorPrototype.Card(CardTarget.BULLET_POWER))
        prototypeButton(table, "Soldier life card", EditorPrototype.Card(CardTarget.SOLDIER_LIFE))
        prototypeButton(table, "Bullet range card", EditorPrototype.Card(CardTarget.BULLET_RANGE))
        prototypeButton(table, "Decoration", EditorPrototype.Decoration)
        prototypeButton(table, "Background decoration", EditorPrototype.BackgroundDecoration)
        prototypeButton(table, "Boss", EditorPrototype.Boss)
        table.add(VisLabel("Pick a prototype, then click the road preview.")).left().width(240f).padTop(16f)
        return table
    }

    private fun prototypeButton(table: VisTable, text: String, prototype: EditorPrototype) {
        val button = VisTextButton(text)
        button.addClickListener {
            pendingPrototype = prototype
            status("placing $text")
        }
        table.add(button).left().width(220f).padBottom(6f).row()
    }

    private fun rebuildProperties() {
        properties.clearChildren()
        when (val current = selected) {
            EditorSelection.Scene -> sceneProperties()
            is EditorSelection.Card -> cardProperties(current.index)
            is EditorSelection.Enemy -> enemyProperties(current.index)
            is EditorSelection.Decoration -> decorationProperties(current.index)
            is EditorSelection.BackgroundDecoration -> backgroundDecorationProperties(current.index)
            is EditorSelection.Boss -> bossProperties(current.index)
        }
    }

    private fun sceneProperties() {
        properties.add(VisLabel("Scene")).left().colspan(2).row()
        text("name", draft.name) { draft.name = it }
        float("road_length", draft.roadLength) { draft.roadLength = it }
        float("road_width", draft.roadWidth) { draft.roadWidth = it }
        int("starting_soldiers", draft.startingSoldiers) { draft.startingSoldiers = it }
        float("fire_rate", draft.fireRate) { draft.fireRate = it }
        int("projectile_pool", draft.projectilePool) { draft.projectilePool = it }
        float("projectile_length", draft.projectileLength) { draft.projectileLength = it }
        float("max_fire_rate", draft.maxFireRate) { draft.maxFireRate = it }
        path("soldier_model", draft.soldierModel) { draft.soldierModel = it }
        path("boss_model", draft.bossModel) { draft.bossModel = it }
        path("manpower_card_model", draft.manpowerCardModel) { draft.manpowerCardModel = it }
        path("firepower_card_model", draft.firepowerCardModel) { draft.firepowerCardModel = it }
        path("bulletpower_card_model", draft.bulletPowerCardModel) { draft.bulletPowerCardModel = it }
        path("soldierlife_card_model", draft.soldierLifeCardModel) { draft.soldierLifeCardModel = it }
        path("bulletrange_card_model", draft.bulletRangeCardModel) { draft.bulletRangeCardModel = it }
        color("player_color", draft.playerColor) { draft.playerColor = it }
        color("enemy_color", draft.enemyColor) { draft.enemyColor = it }
        color("boss_color", draft.bossColor) { draft.bossColor = it }
        color("decoration_color", draft.decorationColor) { draft.decorationColor = it }
    }

    private fun cardProperties(index: Int) {
        val card = draft.cards[index]
        properties.add(VisLabel("Card ${index + 1}")).left().colspan(2).row()
        enum("op", card.operation, CardOperation.entries.toTypedArray()) { card.operation = it }
        enum("param", card.target, CardTarget.entries.toTypedArray()) { card.target = it }
        float("val", card.value) { card.value = it }
        float("x", card.x) { card.x = it }
        float("z", card.z) { card.z = it }
        nullablePath("model", card.modelPath) { card.modelPath = it }
    }

    private fun enemyProperties(index: Int) {
        val enemy = draft.enemies[index]
        properties.add(VisLabel("Enemy ${index + 1}")).left().colspan(2).row()
        nullableText("name", enemy.name) { enemy.name = it }
        int("effective", enemy.effective) { enemy.effective = it }
        float("strength", enemy.unitStrength) { enemy.unitStrength = it }
        float("x", enemy.x) { enemy.x = it }
        float("z", enemy.z) { enemy.z = it }
        nullablePath("model", enemy.modelPath) { enemy.modelPath = it }
        nullableColor("color", enemy.color) { enemy.color = it }
    }

    private fun decorationProperties(index: Int) {
        val decoration = draft.decorations[index]
        properties.add(VisLabel("Decoration ${index + 1}")).left().colspan(2).row()
        text("name", decoration.name) { decoration.name = it }
        float("power", decoration.power) { decoration.power = it }
        float("x", decoration.x) { decoration.x = it }
        float("z", decoration.z) { decoration.z = it }
        path("model", decoration.modelPath) { decoration.modelPath = it }
        nullableColor("color", decoration.color) { decoration.color = it }
    }

    private fun backgroundDecorationProperties(index: Int) {
        val decoration = draft.backgroundDecorations[index]
        properties.add(VisLabel("Background decoration ${index + 1}")).left().colspan(2).row()
        text("name", decoration.name) { decoration.name = it }
        float("power", decoration.power) { decoration.power = it }
        float("x", decoration.x) { decoration.x = it }
        float("z", decoration.z) { decoration.z = it }
        path("model", decoration.modelPath) { decoration.modelPath = it }
        nullableColor("color", decoration.color) { decoration.color = it }
    }

    private fun bossProperties(index: Int) {
        val boss = draft.bosses[index]
        properties.add(VisLabel("Boss ${index + 1}")).left().colspan(2).row()
        nullableText("name", boss.name) { boss.name = it }
        float("power", boss.power) { boss.power = it }
        float("x", boss.x) { boss.x = it }
        float("z", boss.z) { boss.z = it }
        nullablePath("model", boss.modelPath) { boss.modelPath = it }
        nullableColor("color", boss.color) { boss.color = it }
    }

    private fun text(label: String, value: String, setter: (String) -> Unit) =
        field(label, value) { setter(it) }

    private fun nullableText(label: String, value: String?, setter: (String?) -> Unit) =
        field(label, value.orEmpty()) { setter(it.takeIf(String::isNotBlank)) }

    private fun float(label: String, value: Float, setter: (Float) -> Unit) =
        field(label, textNumber(value)) { it.toFloatOrNull()?.let(setter) }

    private fun int(label: String, value: Int, setter: (Int) -> Unit) =
        field(label, value.toString()) { it.toIntOrNull()?.let(setter) }

    private fun path(label: String, value: String, setter: (String) -> Unit) =
        pathField(label, value) { setter(it.ifBlank { value }) }

    private fun nullablePath(label: String, value: String?, setter: (String?) -> Unit) =
        pathField(label, value.orEmpty()) { setter(it.takeIf(String::isNotBlank)) }

    private fun field(label: String, value: String, setter: (String) -> Unit) {
        val input = VisTextField(value)
        input.addChangeListener { mutate { setter(input.text) } }
        properties.add(VisLabel(label)).left().width(130f)
        properties.add(input).left().width(170f).row()
    }

    private fun pathField(label: String, value: String, setter: (String) -> Unit) {
        val row = VisTable(true)
        val input = VisTextField(value)
        val browse = VisTextButton("...")
        input.addChangeListener { mutate { setter(input.text) } }
        browse.addClickListener {
            val chooser = FileChooser(FileChooser.Mode.OPEN)
            chooser.setDirectory(ResourceHome.root.child("assets"))
            chooser.setListener(object : FileChooserAdapter() {
                override fun selected(files: Array<FileHandle>) {
                    if (files.size == 0) return
                    val path = relativize(files.first())
                    input.setText(path)
                    mutate { setter(path) }
                }
            })
            stage.addActor(chooser.fadeIn())
        }
        row.add(input).width(132f)
        row.add(browse).width(34f)
        properties.add(VisLabel(label)).left().width(130f)
        properties.add(row).left().width(170f).row()
    }

    private fun color(label: String, value: LevelColor, setter: (LevelColor) -> Unit) =
        colorField(label, value) { it?.let(setter) }

    private fun nullableColor(label: String, value: LevelColor?, setter: (LevelColor?) -> Unit) =
        colorField(label, value) { setter(it) }

    private fun colorField(label: String, value: LevelColor?, setter: (LevelColor?) -> Unit) {
        val row = VisTable(true)
        val input = VisTextField(value?.let(LevelTextWriter::color).orEmpty())
        val pick = VisTextButton("pick")
        input.addChangeListener { mutate { setter(parseColor(input.text)) } }
        pick.addClickListener {
            val picker = ColorPicker("Pick $label", object : ColorPickerAdapter() {
                override fun finished(newColor: Color) {
                    val levelColor = LevelColor(newColor.r, newColor.g, newColor.b, newColor.a)
                    input.setText(LevelTextWriter.color(levelColor))
                    mutate { setter(levelColor) }
                }
            })
            picker.setAllowAlphaEdit(true)
            value?.let { picker.setColor(Color(it.red, it.green, it.blue, it.alpha)) }
            stage.addActor(picker.fadeIn())
        }
        row.add(input).width(116f)
        row.add(pick).width(50f)
        properties.add(VisLabel(label)).left().width(130f)
        properties.add(row).left().width(170f).row()
    }

    private fun <T : Enum<T>> enum(label: String, value: T, values: kotlin.Array<T>, setter: (T) -> Unit) {
        val select = VisSelectBox<T>()
        select.setItems(*values)
        select.selected = value
        select.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: com.badlogic.gdx.scenes.scene2d.Actor?) {
                mutate { setter(select.selected) }
            }
        })
        properties.add(VisLabel(label)).left().width(130f)
        properties.add(select).left().width(170f).row()
    }

    private fun VisTextField.addChangeListener(action: () -> Unit) {
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: com.badlogic.gdx.scenes.scene2d.Actor?) {
                action()
            }
        })
    }

    private fun mutate(block: () -> Unit) {
        val before = serialize()
        block()
        val after = serialize()
        if (before != after) {
            undo += before
            redo.clear()
            dirty = true
            schedulePreviewRebuild()
            updateButtons()
        }
    }

    private fun restoreFromStack(source: MutableList<String>, destination: MutableList<String>) {
        if (source.isEmpty()) return
        destination += serialize()
        draft = EditableLevel.from(LevelTextParser.parse(source.removeAt(source.lastIndex)))
        dirty = true
        rebuildPreview()
        rebuildProperties()
        updateButtons()
    }

    private fun save() {
        targetFile.parent().mkdirs()
        targetFile.writeString(serialize(), false, "UTF-8")
        dirty = false
        status("saved ${targetFile.name()}")
        onSaved()
        updateButtons()
    }

    private fun requestExit() {
        if (!dirty) {
            onExit()
            return
        }
        object : VisDialog("Unsaved changes") {
            init {
                text("Exit without saving?")
                button("Cancel")
                button("Exit", true)
            }

            override fun result(obj: Any?) {
                if (obj == true) onExit()
            }
        }.show(stage)
    }

    private fun deleteSelected() {
        when (val current = selected) {
            EditorSelection.Scene -> return
            is EditorSelection.Card -> mutate { draft.cards.removeAt(current.index) }
            is EditorSelection.Enemy -> mutate { draft.enemies.removeAt(current.index) }
            is EditorSelection.Decoration -> mutate { draft.decorations.removeAt(current.index) }
            is EditorSelection.BackgroundDecoration -> mutate { draft.backgroundDecorations.removeAt(current.index) }
            is EditorSelection.Boss -> mutate { draft.bosses.removeAt(current.index) }
        }
        selected = EditorSelection.Scene
        rebuildProperties()
        updateButtons()
    }

    private fun sceneClick(screenX: Int, screenY: Int) {
        if (screenX < sceneViewportX() || screenX > sceneViewportX() + sceneViewportWidth() || screenY < TOP_BAR) return
        val world = worldRenderer.editorWorldAt(
            screenX,
            screenY,
            previewModel,
            sceneViewportX(),
            sceneViewportY(),
            sceneViewportWidth(),
            sceneViewportHeight()
        ) ?: return
        val x = world.x.coerceIn(-draft.roadWidth * 0.5f, draft.roadWidth * 0.5f)
        val z = (world.z - GameConfig.LEVEL_INTRO_DISTANCE).coerceAtLeast(0f)

        pendingPrototype?.let {
            insertPrototype(it, x, z)
            pendingPrototype = null
            return
        }

        selected = nearestSelection(screenX, screenY, x, z)
        rebuildProperties()
        updateButtons()
    }

    private fun insertPrototype(prototype: EditorPrototype, x: Float, z: Float) {
        mutate {
            when (prototype) {
                is EditorPrototype.Card -> draft.cards += EditableCard(CardOperation.PLUS, prototype.target, 10f, x, z, null)
                EditorPrototype.Enemy -> draft.enemies += EditableEnemy(25, 10f, null, x, z, null, null)
                EditorPrototype.Decoration -> draft.decorations += EditableDecoration("decoration ${draft.decorations.size + 1}", 1f, x, z, "assets/default-decoration.obj", null)
                EditorPrototype.BackgroundDecoration -> draft.backgroundDecorations += EditableDecoration("background decoration ${draft.backgroundDecorations.size + 1}", 1f, x, z, "assets/default-decoration.obj", null)
                EditorPrototype.Boss -> draft.bosses += EditableBoss(400f, null, x, z, null, null)
            }
        }
        selected = when (prototype) {
            is EditorPrototype.Card -> EditorSelection.Card(draft.cards.lastIndex)
            EditorPrototype.Enemy -> EditorSelection.Enemy(draft.enemies.lastIndex)
            EditorPrototype.Decoration -> EditorSelection.Decoration(draft.decorations.lastIndex)
            EditorPrototype.BackgroundDecoration -> EditorSelection.BackgroundDecoration(draft.backgroundDecorations.lastIndex)
            EditorPrototype.Boss -> EditorSelection.Boss(draft.bosses.lastIndex)
        }
        rebuildProperties()
    }

    private fun nearestSelection(screenX: Int, screenY: Int, x: Float, z: Float): EditorSelection {
        var best: EditorSelection = EditorSelection.Scene
        var bestDistance = Float.POSITIVE_INFINITY
        draft.cards.forEachIndexed { index, item ->
            val box = cardBox(index) ?: return@forEachIndexed
            if (!boxHit(screenX, screenY, box)) return@forEachIndexed
            val distance = abs(item.x - x) + abs(item.z - z)
            if (distance < bestDistance) {
                best = EditorSelection.Card(index)
                bestDistance = distance
            }
        }
        previewModel.enemyBrigades.forEachIndexed { index, brigade ->
            val box = enemyBox(index) ?: return@forEachIndexed
            if (!boxHit(screenX, screenY, box)) return@forEachIndexed
            val centerX = (box.minX + box.maxX) * 0.5f
            val centerZ = (box.minZ + box.maxZ) * 0.5f - GameConfig.LEVEL_INTRO_DISTANCE
            val distance = abs(centerX - x) + abs(centerZ - z)
            if (distance < bestDistance) {
                best = EditorSelection.Enemy(index)
                bestDistance = distance
            }
        }
        draft.decorations.forEachIndexed { index, item ->
            val box = decorationBox(index) ?: return@forEachIndexed
            if (!boxHit(screenX, screenY, box)) return@forEachIndexed
            val distance = abs(item.x - x) + abs(item.z - z)
            if (distance < bestDistance) {
                best = EditorSelection.Decoration(index)
                bestDistance = distance
            }
        }
        draft.backgroundDecorations.forEachIndexed { index, item ->
            val box = backgroundDecorationBox(index) ?: return@forEachIndexed
            if (!boxHit(screenX, screenY, box)) return@forEachIndexed
            val distance = abs(item.x - x) + abs(item.z - z)
            if (distance < bestDistance) {
                best = EditorSelection.BackgroundDecoration(index)
                bestDistance = distance
            }
        }
        previewModel.bosses.forEachIndexed { index, item ->
            val box = bossBox(index) ?: return@forEachIndexed
            if (!boxHit(screenX, screenY, box)) return@forEachIndexed
            val levelZ = item.position.z - GameConfig.LEVEL_INTRO_DISTANCE
            val distance = abs(item.position.x - x) + abs(levelZ - z)
            if (distance < bestDistance) {
                best = EditorSelection.Boss(index)
                bestDistance = distance
            }
        }
        return best
    }

    private fun boxHit(screenX: Int, screenY: Int, box: WorldRenderer.EditorSelectionBox): Boolean =
        worldRenderer.editorBoxContains(
            screenX,
            screenY,
            box,
            previewModel,
            sceneViewportX(),
            sceneViewportY(),
            sceneViewportWidth(),
            sceneViewportHeight()
        )

    private fun rebuildPreview() {
        previewModel = preview()
        previewRebuildDelay = -1f
    }

    private fun schedulePreviewRebuild() {
        previewRebuildDelay = PREVIEW_REBUILD_DELAY_SECONDS
    }

    private fun applyPendingPreviewRebuild() {
        if (previewRebuildDelay < 0f) return
        previewRebuildDelay -= Gdx.graphics.deltaTime
        if (previewRebuildDelay <= 0f) rebuildPreview()
    }

    private fun preview(): AppModel =
        AppModelFactory.initAppModel(draft.toDefinition())

    private fun updateButtons() {
        undoButton.isDisabled = undo.isEmpty()
        redoButton.isDisabled = redo.isEmpty()
        deleteButton.isDisabled = selected == EditorSelection.Scene
    }

    private fun selectedBox(): WorldRenderer.EditorSelectionBox? =
        when (val current = selected) {
            EditorSelection.Scene -> null
            is EditorSelection.Card -> cardBox(current.index)
            is EditorSelection.Enemy -> enemyBox(current.index)
            is EditorSelection.Decoration -> decorationBox(current.index)
            is EditorSelection.BackgroundDecoration -> backgroundDecorationBox(current.index)
            is EditorSelection.Boss -> bossBox(current.index)
        }

    private fun cardBox(index: Int): WorldRenderer.EditorSelectionBox? =
        previewModel.cards.getOrNull(index)?.let {
            boxAt(it.position.x, it.position.y, it.position.z, 0.75f, 0.75f, 0.45f)
        }

    private fun enemyBox(index: Int): WorldRenderer.EditorSelectionBox? =
        previewModel.enemyBrigades.getOrNull(index)?.soldiers
            ?.filter { it.alive }
            ?.map { it.worldPosition }
            ?.takeIf { it.isNotEmpty() }
            ?.let { positions ->
                WorldRenderer.EditorSelectionBox(
                    positions.minOf { it.x } - 0.24f,
                    0f,
                    positions.minOf { it.z } - 0.24f,
                    positions.maxOf { it.x } + 0.24f,
                    1.15f,
                    positions.maxOf { it.z } + 0.24f
                )
            }

    private fun decorationBox(index: Int): WorldRenderer.EditorSelectionBox? =
        previewModel.decorations.getOrNull(index)?.let {
            val footprint = ModelFootprintCatalog.footprint(it.modelPath, 1f)
            boxAt(it.position.x, it.position.y, it.position.z, footprint.halfWidth, 1.25f, footprint.halfDepth)
        }

    private fun backgroundDecorationBox(index: Int): WorldRenderer.EditorSelectionBox? =
        previewModel.backgroundDecorations.getOrNull(index)?.let {
            val footprint = ModelFootprintCatalog.footprint(it.modelPath, 1f)
            boxAt(it.position.x, it.position.y, it.position.z, footprint.halfWidth, 1.25f, footprint.halfDepth)
        }

    private fun bossBox(index: Int): WorldRenderer.EditorSelectionBox? =
        previewModel.bosses.getOrNull(index)?.let {
            boxAt(it.position.x, it.position.y, it.position.z, it.hitHalfWidth, 3.2f, it.hitHalfDepth)
        }

    private fun boxAt(x: Float, y: Float, z: Float, halfWidth: Float, height: Float, halfDepth: Float): WorldRenderer.EditorSelectionBox =
        WorldRenderer.EditorSelectionBox(
            minX = x - halfWidth,
            minY = y,
            minZ = z - halfDepth,
            maxX = x + halfWidth,
            maxY = y + height,
            maxZ = z + halfDepth
        )

    private fun sceneViewportX(): Int = LEFT_PANEL

    private fun sceneViewportY(): Int = 0

    private fun sceneViewportWidth(): Int =
        (Gdx.graphics.width - LEFT_PANEL - RIGHT_PANEL).coerceAtLeast(1)

    private fun sceneViewportHeight(): Int =
        Gdx.graphics.height.coerceAtLeast(1)

    private fun status(text: String) {
        statusLabel.setText(text)
    }

    private fun serialize(): String =
        LevelTextWriter.write(draft.toDefinition())

    private fun parseColor(text: String): LevelColor? =
        runCatching {
            LevelTextParser.parse(
                """
                name: color parse
                player_color: ${text.trim()}
                bosses:
                  - power: 1, z: 1
                """.trimIndent()
            ).colors.player
        }.getOrNull()

    private fun relativize(file: FileHandle): String {
        val absolute = file.file().absoluteFile.toPath().normalize()
        val root = ResourceHome.root.file().absoluteFile.toPath().normalize()
        return runCatching { root.relativize(absolute).toString().replace('\\', '/') }
            .getOrElse { absolute.toString() }
    }

    private fun textNumber(value: Float): String =
        if (value % 1f == 0f) value.toInt().toString() else value.toString()

    private fun textFieldFocused(): Boolean =
        stage.keyboardFocus is VisTextField

    private fun uiWantsPointer(screenX: Int, screenY: Int): Boolean {
        if (stageHasModalUi()) return true
        val stagePoint = stage.screenToStageCoordinates(Vector2(screenX.toFloat(), screenY.toFloat()))
        val hit = stage.hit(stagePoint.x, stagePoint.y, true) ?: return false
        return hit != root
    }

    private fun stageHasModalUi(): Boolean =
        stage.actors.any { actor -> actor.containsModalUi() }

    private fun Actor.containsModalUi(): Boolean {
        if (this is VisDialog || this is FileChooser || this is ColorPicker) return true
        if (this !is com.badlogic.gdx.scenes.scene2d.Group) return false
        return children.any { child -> child.containsModalUi() }
    }

    private fun VisTextButton.addClickListener(action: () -> Unit) {
        addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (!isDisabled) action()
            }
        })
    }

    private inner class EditorInput : InputAdapter() {
        private var cameraDrag = false

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (uiWantsPointer(screenX, screenY)) return false
            if (!inScene(screenX, screenY)) return false
            if (button == Input.Buttons.RIGHT || button == Input.Buttons.MIDDLE) {
                cameraDrag = worldRenderer.editorCameraController.touchDown(screenX, screenY, pointer, button)
                return cameraDrag
            }
            if (button != Input.Buttons.LEFT) return false
            sceneClick(screenX, screenY)
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (!cameraDrag) return false
            return worldRenderer.editorCameraController.touchDragged(screenX, screenY, pointer)
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (!cameraDrag) return false
            cameraDrag = false
            return worldRenderer.editorCameraController.touchUp(screenX, screenY, pointer, button)
        }

        override fun scrolled(amountX: Float, amountY: Float): Boolean {
            return if (!uiWantsPointer(Gdx.input.x, Gdx.input.y) && inScene(Gdx.input.x, Gdx.input.y)) {
                worldRenderer.editorCameraController.scrolled(amountX, amountY)
            } else {
                false
            }
        }
    }

    private fun inScene(screenX: Int, screenY: Int): Boolean =
        screenX >= sceneViewportX() &&
            screenX <= sceneViewportX() + sceneViewportWidth() &&
            screenY >= TOP_BAR

    private data class EditableLevel(
        var name: String,
        var roadLength: Float,
        var roadWidth: Float,
        var startingSoldiers: Int,
        var fireRate: Float,
        var projectilePool: Int,
        var projectileLength: Float,
        var maxFireRate: Float,
        var soldierModel: String,
        var bossModel: String,
        var manpowerCardModel: String,
        var firepowerCardModel: String,
        var bulletPowerCardModel: String,
        var soldierLifeCardModel: String,
        var bulletRangeCardModel: String,
        var playerColor: LevelColor,
        var enemyColor: LevelColor,
        var bossColor: LevelColor,
        var decorationColor: LevelColor,
        val cards: MutableList<EditableCard>,
        val decorations: MutableList<EditableDecoration>,
        val backgroundDecorations: MutableList<EditableDecoration>,
        val enemies: MutableList<EditableEnemy>,
        val bosses: MutableList<EditableBoss>
    ) {
        fun toDefinition(): LevelDefinition =
            LevelDefinition(
                name = name,
                roadLength = roadLength,
                roadWidth = roadWidth,
                startingSoldiers = startingSoldiers,
                fireRate = fireRate,
                projectilePool = projectilePool,
                projectileLength = projectileLength,
                maxFireRate = maxFireRate,
                modelPaths = LevelModelPaths(soldierModel, bossModel, manpowerCardModel, firepowerCardModel, bulletPowerCardModel, soldierLifeCardModel, bulletRangeCardModel),
                colors = LevelColors(playerColor, enemyColor, bossColor, decorationColor),
                cards = cards.map { CardDefinition(it.operation, it.target, it.value, it.x, it.z, it.modelPath) },
                decorations = decorations.map { DecorationDefinition(it.name, it.power, it.x, it.z, it.modelPath, it.color) },
                backgroundDecorations = backgroundDecorations.map { BackgroundDecorationDefinition(it.name, it.power, it.x, it.z, it.modelPath, it.color) },
                enemyBrigades = enemies.map { EnemyBrigadeDefinition(it.effective, it.unitStrength, it.name, it.x, it.z, it.modelPath, it.color) },
                bosses = bosses.map { BossDefinition(it.power, it.name, it.x, it.z, it.modelPath, it.color) }
            )

        companion object {
            fun from(level: LevelDefinition): EditableLevel =
                EditableLevel(
                    name = level.name,
                    roadLength = level.roadLength,
                    roadWidth = level.roadWidth,
                    startingSoldiers = level.startingSoldiers,
                    fireRate = level.fireRate,
                    projectilePool = level.projectilePool,
                    projectileLength = level.projectileLength,
                    maxFireRate = level.maxFireRate,
                    soldierModel = level.modelPaths.soldier,
                    bossModel = level.modelPaths.boss,
                    manpowerCardModel = level.modelPaths.manpowerCard,
                    firepowerCardModel = level.modelPaths.firepowerCard,
                    bulletPowerCardModel = level.modelPaths.bulletPowerCard,
                    soldierLifeCardModel = level.modelPaths.soldierLifeCard,
                    bulletRangeCardModel = level.modelPaths.bulletRangeCard,
                    playerColor = level.colors.player,
                    enemyColor = level.colors.enemy,
                    bossColor = level.colors.boss,
                    decorationColor = level.colors.decoration,
                    cards = level.cards.map { EditableCard(it.operation, it.target, it.value, it.x, it.z, it.modelPath) }.toMutableList(),
                    decorations = level.decorations.map { EditableDecoration(it.name, it.power, it.x, it.z, it.modelPath, it.color) }.toMutableList(),
                    backgroundDecorations = level.backgroundDecorations.map { EditableDecoration(it.name, it.power, it.x, it.z, it.modelPath, it.color) }.toMutableList(),
                    enemies = level.enemyBrigades.map { EditableEnemy(it.effective, it.unitStrength, it.name, it.x, it.z, it.modelPath, it.color) }.toMutableList(),
                    bosses = level.bosses.map { EditableBoss(it.power, it.name, it.x, it.z, it.modelPath, it.color) }.toMutableList()
                )
        }
    }

    private data class EditableCard(var operation: CardOperation, var target: CardTarget, var value: Float, var x: Float, var z: Float, var modelPath: String?)
    private data class EditableEnemy(var effective: Int, var unitStrength: Float, var name: String?, var x: Float, var z: Float, var modelPath: String?, var color: LevelColor?)
    private data class EditableDecoration(var name: String, var power: Float, var x: Float, var z: Float, var modelPath: String, var color: LevelColor?)
    private data class EditableBoss(var power: Float, var name: String?, var x: Float, var z: Float, var modelPath: String?, var color: LevelColor?)

    private sealed interface EditorSelection {
        data object Scene : EditorSelection
        data class Card(val index: Int) : EditorSelection
        data class Enemy(val index: Int) : EditorSelection
        data class Decoration(val index: Int) : EditorSelection
        data class BackgroundDecoration(val index: Int) : EditorSelection
        data class Boss(val index: Int) : EditorSelection
    }

    private sealed interface EditorPrototype {
        data class Card(val target: CardTarget) : EditorPrototype
        data object Enemy : EditorPrototype
        data object Decoration : EditorPrototype
        data object BackgroundDecoration : EditorPrototype
        data object Boss : EditorPrototype
    }

    companion object {
        private const val LEFT_PANEL = 350
        private const val RIGHT_PANEL = 280
        private const val TOP_BAR = 48
        private const val PREVIEW_REBUILD_DELAY_SECONDS = 0.8f
    }
}
