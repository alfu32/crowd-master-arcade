# Functional and Technical Specification

## Crowd Defense Runner Game

**Platform:** libGDX
**Language:** Kotlin
**Architecture:** Model–View–Controller

---

# 1. Game Concept

The game is a 3D/2.5D crowd defense runner inspired by games such as *Count Masters*.

The player controls a brigade of small low-poly soldiers moving along a road. The camera is fixed behind the player brigade. Enemies, upgrade cards, multiplier cards, subtractor cards, projectiles, and bosses advance toward the player brigade.

The player can move the entire brigade left or right using:

* Keyboard
* Mouse/touch drag
* On-screen joystick

The objective is to survive incoming waves, grow or upgrade the brigade, defeat enemies, and eventually defeat the boss.

---

# 2026-07-04 Functional Addendum

## Boss Collision Bounds

Boss projectile and player-contact hit areas must be proportional to the rendered model footprint, not a fixed radius. The model footprint is derived from OBJ vertex extents on the X/Z plane after applying the same anchoring convention used by rendering: X/Z centered, Y placed on the model minimum. Missing or malformed models fall back to the legacy boss collision radius.

## HUD

The in-game HUD should be a single top line:

```text
level <number> <name> soldiers:<soldiers> fire:<fire> bullet caliber:<bullet power> life:<life value> speed:<speed> score:<hits>/<total possible hits>
```

The HUD is rendered with VisUI. It must use larger, high-contrast text than the default skin size so the single-line statistics remain readable during gameplay.

## Campaign Menu

On startup the app should show a VisUI menu before entering gameplay. The menu lists accessible levels in alphabetical campaign order and shows:

- level number
- level name
- user points
- level total possible points
- completion percentage
- completion/unlocked status

The selected level supports:

- `Play`: continue the campaign from the selected level.
- `Test`: run only the selected level.
- `Edit`: open the level editor.
- `Create`: create a new level file in the data home folder.
- `Delete`: delete the selected user level after confirmation.
- `Reset Data Home`: delete and recreate `.crowdmaster` from internal packaged assets.
- `Exit`: close the application.

The app remembers the last selected/played level on exit and restores it on next launch.

Menu behavior:

- the level list has a fixed visible height and scrolls vertically when it overflows
- level rows are rendered as aligned table rows, not as proportional preformatted strings
- clicking anywhere on a level row selects it
- the selected row is highlighted
- any level can be selected, including locked levels
- locked levels disable `Play`, but still allow management actions such as edit/test/delete
- pressing `Esc` on the main menu exits the app
- pressing `Esc` during gameplay returns to the main menu instead of exiting the app

## Campaign Progress

The app persists per-level records in the resource home folder:

- completed/won flag
- best user points
- total possible points
- percentage achieved
- last selected level

Totals remain derivable from the per-level records.

## Level Editor

The level editor uses VisUI for all controls and an orthographic libGDX 3D scene view for the level preview.

Required layout:

- center: orthographic 3D editor scene
- right: prototype palette for `enemy_brigades`, `cards`, `decorations`, and `bosses`
- left: property panel for the selected object or scene
- bottom/top command bar: save, undo, redo, delete selected, exit

Interactions:

- drag prototypes from the right palette into the scene
- select one object at a time
- selected object is highlighted with a bounding box
- clicking empty scene selects scene-level properties
- delete selected object with a button or the Delete key
- Delete/Backspace while a property text field is focused must edit the field only and must not delete the selected scene object
- save writes the `.level` file
- exit prompts if there are unsaved changes
- the orthographic editor camera supports panning, zooming, and rotation
- the orthographic editor camera target remains on the ground/causeway
- the editor scene uses the same shadow-map lighting path as the gameplay camera
- the editor road preview extends beyond the declared road length when placed content exceeds it; the extension is visually distinct and about 60% opaque
- picking uses the projected 3D selection bounds so small or vertically visible objects can be selected reliably

Property editing:

- fields are shown as a two-column table: parameter name and editor
- colors use VisUI color picker
- OBJ model references use the system file picker
- paths under `.crowdmaster` are saved relative to `.crowdmaster`; other paths remain absolute
- number/text fields use VisUI inputs
- changes are applied to preview with an 800 ms debounce
- crowd/unit layout uses the same formation logic as gameplay

## UI Framework

All menus, HUD, dialogs, and editor panels should be migrated to VisUI. Existing `SpriteBatch`/`BitmapFont` UI is considered temporary after this addendum.

---

# 2. Main Application Loop

The application follows this model lifecycle:

```text
init_app_model
while app_model_running:
    present_app_model
    change_app_model_state(
        keyboard_mouse_touch_inputs,
        simulation_time
    )
end
```

Equivalent Kotlin structure:

```kotlin
fun initAppModel(): AppModel

while (appModel.running) {
    view.present(appModel)
    controller.changeAppModelState(
        appModel,
        inputState,
        deltaTime
    )
}
```

---

# 3. MVC Architecture

## 3.1 Model

The model contains all game state and simulation data.

Main model objects:

```text
AppModel
 ├─ GameState
 ├─ PlayerBrigade
 ├─ EnemyBrigades
 ├─ Cards
 ├─ Projectiles
 ├─ Road
 ├─ Background
 ├─ Boss
 ├─ LevelData
 └─ RuntimeConfig
```

The model must not depend on rendering classes.

---

## 3.2 View

The view renders the current model state.

Responsibilities:

* Render road
* Render background
* Render player brigade
* Render enemy brigades
* Render cards
* Render projectiles
* Render boss
* Render UI
* Render joystick on mobile/touch devices

The view reads from the model but does not modify it directly.

---

## 3.3 Controller

The controller updates the model.

Responsibilities:

* Read input
* Move player brigade left/right
* Advance enemies, cards, projectiles, and boss toward player
* Handle shooting
* Handle collisions
* Apply card effects
* Spawn or activate entities
* Determine win/loss state

---

# 4. Game Entities

## 4.1 Player Brigade

The player brigade is composed of individual regular soldiers.

Data:

```kotlin
data class PlayerBrigade(
    var position: Vector3,
    var lateralSpeed: Float,
    var soldiers: MutableList<RegularSoldier>,
    var fireRate: Float,
    var fireCooldown: Float,
    var alive: Boolean
)
```

Behavior:

* Moves left/right as a group.
* Stays constrained inside the road boundaries.
* Soldiers maintain formation around the brigade center.
* Automatically shoots at incoming enemies.
* Can gain or lose soldiers through cards.
* Can gain fire rate upgrades.

---

## 4.2 Enemy Brigade

Enemy brigades are groups of enemy regulars.

```kotlin
data class EnemyBrigade(
    var position: Vector3,
    var speed: Float,
    var soldiers: MutableList<RegularSoldier>,
    var alive: Boolean
)
```

Behavior:

* Moves toward the player brigade.
* Individual enemies can be killed by projectiles.
* If enemies reach the player brigade, they damage or remove player soldiers.

---

## 4.3 Regular Soldier

Used by both player and enemy brigades.

```kotlin
data class RegularSoldier(
    val id: Long,
    var localOffset: Vector3,
    var worldPosition: Vector3,
    var health: Float,
    var alive: Boolean
)
```

Rendering style:

* Low-poly humanoid
* Single-color material
* Rounded simple “gummy bear” look
* Minimal animation

---

## 4.4 Cards

Cards move toward the player and apply effects on collision.

Card types:

```kotlin
enum class CardType {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    FIRE_RATE_UP
}
```

Card data:

```kotlin
data class Card(
    val id: Long,
    val type: CardType,
    val value: Float,
    var position: Vector3,
    var speed: Float,
    var active: Boolean
)
```

Effects:

```text
ADD:        player soldiers += value
SUBTRACT:   player soldiers -= value
MULTIPLY:   player soldiers *= value
DIVIDE:     player soldiers /= value
FIRE_RATE:  player fireRate += value
```

Soldier count must never go below zero.

---

## 4.5 Projectiles

```kotlin
data class Projectile(
    val id: Long,
    var position: Vector3,
    var velocity: Vector3,
    var damage: Float,
    var active: Boolean
)
```

Behavior:

* Spawned by player soldiers or the player brigade.
* Move toward enemies.
* Destroy or damage enemies on collision.
* Removed when outside active range.

---

## 4.6 Road

```kotlin
data class Road(
    val width: Float,
    val length: Float,
    val leftBoundary: Float,
    val rightBoundary: Float
)
```

The player brigade can only move laterally inside road boundaries.

---

## 4.7 Background

Static or scrolling environment behind and around the road.

```kotlin
data class Background(
    val theme: String
)
```

---

## 4.8 Boss

```kotlin
data class Boss(
    var position: Vector3,
    var health: Float,
    var speed: Float,
    var active: Boolean,
    var alive: Boolean
)
```

Behavior:

* Appears near the end of the level.
* Moves toward the player.
* Has high health.
* Can attack or absorb player soldiers if it reaches the brigade.

---

# 5. Level Initialization

Before the game starts, the model contains:

* Player brigade
* Road
* Background
* Incoming enemies
* Incoming cards
* Boss
* Runtime configuration

Entities may be loaded from:

1. Level file
2. Procedural generation
3. Default configuration

## 5.1 Level File Example

```json
{
  "road": {
    "width": 8.0,
    "length": 200.0
  },
  "player": {
    "initialSoldiers": 10,
    "fireRate": 1.0
  },
  "cards": [
    { "type": "MULTIPLY", "value": 2, "z": 30, "x": -2 },
    { "type": "ADD", "value": 15, "z": 45, "x": 2 },
    { "type": "DIVIDE", "value": 2, "z": 60, "x": 0 }
  ],
  "enemyBrigades": [
    { "soldiers": 20, "z": 80, "x": 0 },
    { "soldiers": 40, "z": 130, "x": 1 }
  ],
  "boss": {
    "health": 500,
    "z": 190
  }
}
```

---

# 6. Coordinate System

Recommended coordinate system:

```text
X = left/right movement
Y = vertical height
Z = forward/backward road direction
```

The player brigade remains near the camera.

Incoming entities move along negative Z toward the player.

```text
enemy.position.z -= enemy.speed * deltaTime
card.position.z  -= card.speed * deltaTime
boss.position.z  -= boss.speed * deltaTime
```

---

# 7. Camera

The camera is fixed behind the player brigade.

Recommended setup:

```kotlin
camera.position.set(0f, 7f, -12f)
camera.lookAt(0f, 0f, 15f)
camera.near = 0.1f
camera.far = 300f
camera.update()
```

The camera may remain fixed or softly follow the player brigade laterally.

---

# 8. Player Input

Supported inputs:

## Keyboard

```text
A / LEFT  -> move brigade left
D / RIGHT -> move brigade right
```

## Touch / Mouse Drag

Dragging horizontally changes the target X position of the player brigade.

## On-screen Joystick

Joystick X-axis controls lateral movement.

Unified input state:

```kotlin
data class InputState(
    var moveX: Float = 0f,
    var dragging: Boolean = false,
    var dragDeltaX: Float = 0f
)
```

`moveX` range:

```text
-1.0 = full left
 0.0 = no movement
 1.0 = full right
```

---

# 9. Simulation Systems

## 9.1 Player Movement System

```kotlin
player.position.x += input.moveX * player.lateralSpeed * deltaTime
player.position.x = player.position.x.coerceIn(
    road.leftBoundary,
    road.rightBoundary
)
```

---

## 9.2 Incoming Movement System

All enemies, cards, and boss move toward the player.

```kotlin
for (enemy in enemies) {
    enemy.position.z -= enemy.speed * deltaTime
}

for (card in cards) {
    card.position.z -= card.speed * deltaTime
}

if (boss.active) {
    boss.position.z -= boss.speed * deltaTime
}
```

---

## 9.3 Formation System

The player brigade should not simulate complex AI for each soldier.

Instead:

```text
brigade center position + predefined soldier slot offsets
```

Each soldier moves toward its assigned slot.

```kotlin
soldier.worldPosition.lerp(
    player.position + soldier.localOffset,
    0.15f
)
```

Formation should be recalculated when soldier count changes.

Possible formation:

* Grid
* Circle
* Arc
* Staggered rows

---

## 9.4 Shooting System

Player soldiers automatically shoot at enemies in range.

Simplified approach:

```text
if fireCooldown <= 0:
    find nearest enemy
    spawn projectile
    reset fireCooldown
```

```kotlin
player.fireCooldown -= deltaTime

if (player.fireCooldown <= 0f) {
    val target = findNearestEnemy(appModel)
    if (target != null) {
        spawnProjectile(player.position, target.position)
        player.fireCooldown = 1f / player.fireRate
    }
}
```

---

## 9.5 Collision System

Required collisions:

```text
Projectile ↔ Enemy soldier
Player brigade ↔ Card
Enemy brigade ↔ Player brigade
Projectile ↔ Boss
Boss ↔ Player brigade
```

Use simple radius-based collision first.

```kotlin
fun overlaps(a: Vector3, b: Vector3, radius: Float): Boolean {
    return a.dst2(b) <= radius * radius
}
```

Avoid Box2D initially.

---

# 10. Card Effects

```kotlin
fun applyCard(player: PlayerBrigade, card: Card) {
    when (card.type) {
        CardType.ADD -> addSoldiers(player, card.value.toInt())
        CardType.SUBTRACT -> removeSoldiers(player, card.value.toInt())
        CardType.MULTIPLY -> multiplySoldiers(player, card.value.toInt())
        CardType.DIVIDE -> divideSoldiers(player, card.value.toInt())
        CardType.FIRE_RATE_UP -> player.fireRate += card.value
    }

    card.active = false
}
```

Rules:

* Soldier count cannot go below zero.
* If player soldier count reaches zero, the game is lost.
* Fire rate should have a maximum cap.

---

# 11. Game States

```kotlin
enum class GameState {
    LOADING,
    MAIN_MENU,
    READY,
    RUNNING,
    PAUSED,
    WON,
    LOST,
    EXIT
}
```

The main loop runs only while:

```kotlin
appModel.running == true
```

---

# 12. Rendering Requirements

Use libGDX 3D rendering.

Recommended:

```text
ModelBatch
PerspectiveCamera
Environment
DirectionalLight
ModelInstance
SpriteBatch for UI
Scene2D for buttons/menus
```

Characters:

* Use one low-poly soldier model.
* Instance it many times.
* Change material color for player/enemy.
* Keep geometry simple.

Cards:

* Flat rectangular 3D planes or boxes.
* Text label: `+10`, `x2`, `/2`, `-5`, `FIRE +1`.

Projectiles:

* Small spheres, capsules, or simple billboards.

Road:

* Long plane or box.
* Optional lane markings.

Background:

* Static models, skybox, or simple colored backdrop.

---

# 13. Asset Requirements

Minimum assets:

```text
soldier.g3db or soldier.glb
enemy_soldier.g3db optional
boss.g3db
card model or generated box
projectile model
road texture or material
background props optional
```

For prototype:

* Generate simple primitives in code.
* Use placeholder cubes/capsules/spheres.
* Replace with Blender assets later.

---

# 14. Performance Requirements

Target:

```text
Desktop: 60 FPS
Mobile: 30–60 FPS
Entities: 100–500 soldiers initially
```

Important constraints:

* Use object pooling for projectiles.
* Avoid allocating objects inside the render loop.
* Reuse `Vector3` where possible.
* Use instancing/batching where possible.
* Do not use heavy per-soldier AI.
* Use simple collision checks first.
* Add spatial partitioning only if needed.

---

# 15. Recommended Project Structure

```text
core/
 └─ src/main/kotlin/
     ├─ model/
     │   ├─ AppModel.kt
     │   ├─ PlayerBrigade.kt
     │   ├─ EnemyBrigade.kt
     │   ├─ RegularSoldier.kt
     │   ├─ Card.kt
     │   ├─ Projectile.kt
     │   ├─ Boss.kt
     │   └─ LevelData.kt
     │
     ├─ controller/
     │   ├─ GameController.kt
     │   ├─ InputController.kt
     │   ├─ MovementSystem.kt
     │   ├─ ShootingSystem.kt
     │   ├─ CollisionSystem.kt
     │   └─ LevelSystem.kt
     │
     ├─ view/
     │   ├─ GameView.kt
     │   ├─ WorldRenderer.kt
     │   ├─ UiRenderer.kt
     │   └─ AssetManager.kt
     │
     ├─ config/
     │   └─ GameConfig.kt
     │
     └─ CrowdDefenseGame.kt
```

---

# 16. Main Game Class

```kotlin
class CrowdDefenseGame : ApplicationAdapter() {

    private lateinit var appModel: AppModel
    private lateinit var controller: GameController
    private lateinit var view: GameView

    override fun create() {
        appModel = AppModelFactory.initAppModel()
        controller = GameController()
        view = GameView()
    }

    override fun render() {
        val deltaTime = Gdx.graphics.deltaTime

        if (appModel.running) {
            view.presentAppModel(appModel)
            controller.changeAppModelState(
                appModel,
                deltaTime
            )
        }
    }

    override fun dispose() {
        view.dispose()
    }
}
```

---

# 17. Win/Loss Conditions

Loss:

```text
player soldier count == 0
boss reaches player and player cannot defeat it
enemy brigade destroys player brigade
```

Win:

```text
boss health <= 0
or all level entities cleared
```

---

# 18. Minimum Playable Prototype

The first playable version must include:

1. Fixed camera behind player brigade.
2. Road.
3. Player brigade moving left/right.
4. Incoming enemy brigade.
5. Incoming multiplier/add/subtract/divide cards.
6. Projectile shooting.
7. Collision between projectiles and enemies.
8. Collision between player and cards.
9. Basic boss at end of level.
10. Win/loss state.

---

# 19. Development Order

Recommended implementation order:

```text
1. Create libGDX Kotlin project
2. Implement AppModel
3. Implement fixed camera and road renderer
4. Render placeholder player soldiers
5. Add left/right movement
6. Add incoming cards
7. Apply card effects
8. Add incoming enemies
9. Add automatic shooting
10. Add projectiles
11. Add collisions
12. Add boss
13. Add UI
14. Replace placeholders with low-poly models
15. Optimize
```
