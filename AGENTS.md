# Repository Guidelines

## Project Structure & Module Organization

This is a Kotlin/libGDX multi-module Gradle project. Core gameplay, models, controllers, and views live in `core/src/main/kotlin/com/crowdmasterarcade`; unit tests mirror that package under `core/src/test/kotlin`. Bundled OBJ models, shaders, and campaign data are in `core/src/main/resources`. Keep level files in `levels/` and list them in `levels/index.txt` in play order.

Platform launchers are separate: `lwjgl3/` targets desktop and `android/` targets Android. `level-intellij-plugin/` and `level-vscode-extension/` provide editor support for `.level` and `.cma-level` files. Shared artwork and release-support files live in `assets/`, `documentation/`, and `scripts/`.

## Build, Test, and Development Commands

Use JDK 21 and the checked-in Gradle wrapper. A project-local cache is helpful in constrained environments:

```bash
GRADLE_USER_HOME=.gradle-user ./gradlew test
GRADLE_USER_HOME=.gradle-user ./gradlew :lwjgl3:run
GRADLE_USER_HOME=.gradle-user ./gradlew :lwjgl3:jar
GRADLE_USER_HOME=.gradle-user ./gradlew :android:assembleDebug
```

The first command runs all enabled tests; the others launch desktop, build its fat JAR, and create a debug APK. To test custom levels, add `-Dlevels.dir=/absolute/path/to/levels` to `:lwjgl3:run`. In `level-vscode-extension/`, run `npm ci` followed by `npm run package` to create the VSIX.

## Coding Style & Naming Conventions

Follow existing Kotlin style: four-space indentation, braces on the declaration line, and explicit imports. Use `PascalCase` for classes and enums, `camelCase` for functions/properties, and descriptive package names under `com.crowdmasterarcade`. Prefer small systems/controllers with focused responsibilities. Name tests `*Test.kt` and bundled levels with a zero-padded sequence plus slug, such as `025-storm-gate.level`.

## Testing Guidelines

Tests use `kotlin.test` on JUnit Platform. Add deterministic unit coverage for parser, model, collision, movement, shooting, and card-effect changes. There is no configured coverage threshold, but new behavior and regressions should have focused tests. Run `./gradlew test` before submitting; manually launch desktop for rendering or input changes.

## Commit & Pull Request Guidelines

Recent history favors Conventional Commit-style subjects such as `feat(gameplay): improvements`, `feat(campaign): 24 new levels`, and `fix(editor): UI vs scene click priority`. Use an imperative, scoped subject and keep each commit focused. Pull requests should explain player-visible impact, list verification performed, link relevant issues, and include screenshots or short recordings for UI, rendering, asset, or level-design changes.
