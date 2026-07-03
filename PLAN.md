  Implementation Plan

  1. Project Scaffold
      - Create a Kotlin/libGDX Gradle project with at least core and lwjgl3 desktop launcher modules.
      - Add Kotlin, libGDX 3D, asset, and test dependencies.
      - Keep Android/mobile structure optional until desktop prototype is playable.

  2. Model Layer
      - Implement pure model classes under core/src/main/kotlin/model.
      - Include AppModel, GameState, PlayerBrigade, EnemyBrigade, RegularSoldier, Card, Projectile, Road, Background, Boss, LevelData, and RuntimeConfig.
      - Avoid rendering dependencies in model code.
      - Add AppModelFactory with a default hardcoded level matching the spec’s example.

  3. Controller / Simulation
      - Implement GameController.changeAppModelState(appModel, inputState, deltaTime).
      - Split systems into focused classes:
          - InputController
          - MovementSystem
          - FormationSystem
          - ShootingSystem
          - CollisionSystem
          - CardEffectSystem
          - LevelSystem

      - Start with radius-based collisions and simple nearest-target shooting.
      - Add projectile pooling early enough to avoid render-loop allocation habits.

  4. View / Rendering
      - Implement libGDX 3D rendering with PerspectiveCamera, ModelBatch, Environment, and primitive generated models.
      - Use placeholder primitives first:
          - road as long box/plane
          - soldiers as simple low-poly capsule-like or stacked primitive instances
          - cards as colored boxes with labels
          - boss as larger primitive
          - projectiles as small spheres

      - Keep GameView.presentAppModel(appModel) read-only against the model.

  5. Input
      - Support keyboard first: A/LEFT, D/RIGHT.
      - Add mouse/touch horizontal drag.
      - Add simple on-screen joystick after desktop movement is stable.

  6. Gameplay Milestone
      - Build the minimum playable prototype in this order:
          1. fixed camera, road, player brigade visible
          2. left/right bounded movement
          3. formation recalculation as soldier count changes
          4. incoming cards and card effects
          5. enemy brigades
          6. automatic shooting and projectiles
          7. projectile/enemy and player/card collisions
          8. enemy/player damage
          9. boss encounter
         10. win/loss UI state

  7. Testing
      - Unit test model/controller systems without libGDX rendering where possible:
          - card math and soldier count bounds
          - movement boundary clamping
          - formation slot generation
          - projectile collision behavior
          - win/loss transitions

      - Add a desktop smoke run task once rendering exists.

  8. Polish / Optimization
      - Cap fire rate.
      - Remove inactive entities safely.
      - Reuse Vector3 temporaries in hot paths.
      - Keep spatial partitioning out until soldier/projectile counts prove it necessary.
      - Replace primitive assets with real .g3db/.glb assets only after prototype gameplay is complete.

  9. Boss Collision Bounds
      - Add a model-footprint resolver that reads OBJ vertex extents once per model path.
      - Store boss hit half-width and half-depth on runtime boss instances.
      - Replace fixed-radius boss projectile/contact checks with X/Z footprint overlap checks.
      - Keep a radius fallback for missing or malformed models.

  10. VisUI Campaign Menu
      - Add VisUI dependency to the UI-capable modules.
      - Introduce application screen/state routing: main menu, game, editor.
      - Load campaign stats before menu render.
      - Persist last selected/played level in the data home folder.
      - Render a selectable level table with points, possible points, and percentage.
      - Add Play, Test, Edit, Create, Delete, and Reset Data Home actions.

  11. VisUI HUD Migration
      - Replace the temporary SpriteBatch HUD with a VisUI stage overlay.
      - Preserve the single-line HUD text contract:
        `level <number> <name> soldiers:<soldiers> fire:<fire> bullet caliber:<bullet power> life:<life value> speed:<speed> score:<hits>/<total possible hits>`.
      - Keep gameplay overlays for level intro and win/loss prompts in VisUI dialogs/labels.

  12. Level Editor
      - Build an editor app screen reachable from the campaign menu.
      - Add a `LevelTextWriter` so parsed levels can be saved back to `.level` files.
      - Edit real sorted `.crowdmaster` level files, and make Create/Delete operate on those files.
      - Add right-side prototype palette for cards, enemy brigades, decorations, and bosses.
      - Add left-side property editor table for scene/object fields.
      - Add single selection, delete selected, undo, redo, save, and guarded exit.
      - Implement file picker integration for OBJ paths and relative path conversion under `.crowdmaster`.
      - Implement color picker integration for `#RRGGBBAA` fields.
      - Rebuild the preview through `AppModelFactory` so gameplay model loading and formation layout stay shared.
      - Follow-up: replace current gameplay-camera preview with an orthographic editor renderer, add ray picking, visible bounding-box selection, and 800 ms debounced preview rebuilds.

  13. Campaign Progress
      - Extend campaign stats with completed/won state, best score, possible score, percentage, and last selected level.
      - Unlock levels based on completion rules.
      - Make Test mode run only a selected level without advancing campaign progress unless explicitly desired.
      - Make Play mode continue campaign from the selected level.
