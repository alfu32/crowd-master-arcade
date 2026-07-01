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

