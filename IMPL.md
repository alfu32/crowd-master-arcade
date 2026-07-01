# Implementation Report

This report tracks the current implementation against `PLAN.md`.

## 1. Project Scaffold

Status: complete for desktop prototype.

- Created a Kotlin/libGDX Gradle project.
- Added `core` module for game/model/controller/view code.
- Added `lwjgl3` module for the desktop launcher.
- Added libGDX, LWJGL3 backend, Kotlin, and test dependencies.
- Android/mobile modules are intentionally deferred until the desktop prototype is stable.

Key files:

- `settings.gradle.kts`
- `build.gradle.kts`
- `core/build.gradle.kts`
- `lwjgl3/build.gradle.kts`
- `lwjgl3/src/main/kotlin/com/crowdmasterarcade/desktop/DesktopLauncher.kt`

## 2. Model Layer

Status: complete for prototype.

- Implemented the core model types:
  - `AppModel`
  - `GameState`
  - `PlayerBrigade`
  - `EnemyBrigade`
  - `RegularSoldier`
  - `Card`
  - `Projectile`
  - `Road`
  - `Background`
  - `Boss`
  - `LevelData`
  - `RuntimeConfig`
  - `InputState`
- Added `AppModelFactory` that builds runtime models from parsed textual level definitions.
- Added `LevelCatalog` to load levels from an external folder or bundled assets.
- Added a bundled serialized level: `The Raven's Bend`.
- Model code is independent from rendering classes, except for libGDX `Vector3` as the shared math type.

Key files:

- `core/src/main/kotlin/com/crowdmasterarcade/model/Models.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/model/GameState.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/model/CardOperation.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/model/AppModelFactory.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/model/LevelDefinition.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/model/LevelTextParser.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/model/LevelCatalog.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/config/GameConfig.kt`

## 3. Controller / Simulation

Status: substantially complete for minimum playable prototype.

- Implemented `GameController.changeAppModelState`.
- Added focused systems:
  - `InputController`
  - `MovementSystem`
  - `FormationSystem`
  - `ShootingSystem`
  - `CollisionSystem`
  - `CardEffectSystem`
  - `LevelSystem`
- Added simple radius-based collision handling.
- Added object-pooled projectiles.
- Updated shooting so each live player soldier fires its own projectile per volley.
- Added card effects for operation/target pairs such as `plus manpower` and `times firepower`.
- Player soldiers now shoot straight forward along the road instead of aiming diagonally.
- Projectile lifetime is derived from the level's `projectile_length`.
- Enemy brigades support per-unit `strength` and optional names.
- Bosses support names and display current/maximum life above their model.
- Decorations can be declared in level files, render from OBJ models, fall back to cubes when missing, and are ignored by gameplay collisions.
- Model height calculations now scan mesh vertices directly instead of relying on framework bounding-box helpers.
- Added a directional shadow-map pass for 3D scene shadows.
- 3D label blocks are black and larger for readability.
- Added win/loss state transitions.

Remaining work:

- Collision is intentionally simple and may need tuning for feel.
- Enemy/player combat currently resolves in a simple mass-removal interaction.
- Projectile target selection is nearest-target based and can be improved later.

Key files:

- `core/src/main/kotlin/com/crowdmasterarcade/controller/GameController.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/controller/InputController.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/controller/MovementSystem.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/controller/FormationSystem.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/controller/ShootingSystem.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/controller/CollisionSystem.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/controller/CardEffectSystem.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/controller/LevelSystem.kt`

## 4. View / Rendering

Status: complete for placeholder prototype.

- Implemented libGDX 3D rendering using:
  - `PerspectiveCamera`
  - `ModelBatch`
  - `Environment`
  - `DirectionalLight`
  - generated primitive `ModelInstance`s
- Rendered placeholder primitives for:
  - road
  - side rails
  - player soldiers
  - enemy soldiers
  - cards
  - boss
  - projectiles
- Added HUD rendering with `SpriteBatch` and `BitmapFont`.
- Added operation card labels:
  - operation text such as `x2`, `/2`, `+15`, `-5`, `+1`
  - target text: `MANPOWER` or `FIREPOWER`
- Replaced card label overlays with larger in-world 3D block text.
- Added OBJ model loading for soldier, boss, manpower card, and firepower card paths from level files.
- Added default OBJ assets in `core/src/main/resources/assets`.
- Reduced camera FOV so the road and entities occupy more screen space.

Remaining work:

- Default models are simple OBJ reference boxes, not final low-poly production assets.
- Card labels are block-style 3D geometry rather than textured typography.

Key files:

- `core/src/main/kotlin/com/crowdmasterarcade/view/GameView.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/view/WorldRenderer.kt`
- `core/src/main/kotlin/com/crowdmasterarcade/view/UiRenderer.kt`

## 5. Input

Status: partially complete.

- Added keyboard input:
  - `A` / left arrow moves left.
  - `D` / right arrow moves right.
- Added mouse/touch horizontal drag steering.
- Fixed an initial direction reversal bug across keyboard and drag input.

Remaining work:

- On-screen joystick has not been implemented yet.
- Touch input is functional as drag steering, but not presented as a mobile UI control.

Key file:

- `core/src/main/kotlin/com/crowdmasterarcade/controller/InputController.kt`

## 6. Gameplay Milestone

Status: minimum playable prototype is implemented.

Implemented:

1. Fixed camera, road, and visible player brigade.
2. Left/right bounded player movement.
3. Formation recalculation when soldier count changes.
4. Incoming cards and card effects.
5. Incoming enemy brigades.
6. Automatic shooting and projectile movement.
7. Projectile/enemy and player/card collisions.
8. Enemy/player damage interaction.
9. Basic boss encounter.
10. Win/loss UI state.

Known prototype limitations:

- Level data is loaded from text definitions.
- No menus or pause UI beyond basic state support.
- Gameplay balance is not tuned.
- Boss behavior is limited to movement/contact/life total.

## 7. Testing

Status: initial test coverage added.

Implemented tests:

- Card subtraction cannot reduce soldier count below zero.
- Multiplication adds soldiers.
- Fire rate upgrades respect the cap.
- A shooting volley creates one projectile per live player soldier.
- The bundled textual level definition parses into the expected cards, enemies, and bosses.

Verification command:

```bash
GRADLE_USER_HOME=.gradle-user gradle test :lwjgl3:compileKotlin
```

Latest result: passing.

Remaining test work:

- Movement boundary clamping tests.
- Formation layout tests.
- Projectile collision tests.
- Win/loss transition tests.
- Desktop smoke run task.

Key test files:

- `core/src/test/kotlin/com/crowdmasterarcade/CardEffectSystemTest.kt`
- `core/src/test/kotlin/com/crowdmasterarcade/ShootingSystemTest.kt`

## 8. Polish / Optimization

Status: started.

Implemented:

- Fire rate cap.
- Projectile object pool.
- Inactive projectiles are reused.
- Basic `Vector3` temporary reuse in hot shooting/formation paths.
- Generated primitive assets keep the prototype asset-free.

Remaining work:

- Avoid more render-loop allocations as the renderer grows.
- Add spatial partitioning only if entity counts require it.
- Replace placeholders with low-poly `.g3db` or `.glb` assets.
- Tune projectile pool size against expected maximum soldier/fire-rate counts.
- Improve visual clarity and card typography.

## Run Instructions

Run the desktop prototype:

```bash
GRADLE_USER_HOME=.gradle-user gradle :lwjgl3:run
```

Run tests and compile the desktop module:

```bash
GRADLE_USER_HOME=.gradle-user gradle test :lwjgl3:compileKotlin
```

## Current Summary

The project has moved from specification-only to a working desktop libGDX prototype. The implemented version covers the minimum gameplay loop with placeholder 3D rendering, player movement, card effects, enemy waves, per-soldier projectile volleys, boss state, and win/loss outcomes. The next best step is to tighten tests around movement/collision/win-loss behavior, then improve mobile controls and visual fidelity.
