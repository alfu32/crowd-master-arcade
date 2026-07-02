# Crowd Master Arcade Level IntelliJ Plugin Specification

## Purpose

This module builds an independent IntelliJ Platform plugin that provides syntax highlighting for Crowd Master Arcade level definition files. It is intentionally separate from the game runtime modules and can be packaged as its own plugin ZIP artifact.

## File Type

The plugin registers a custom language and file type named `Crowd Master Arcade Level`.

Highlighting applies to:

- `*.level`
- `*.cma-level`

## Token Classes

The highlighter recognizes the lightweight level format parsed by `LevelTextParser`, not full YAML.

Highlighted token classes:

- Key names: field names before `:`, including `name`, `road_length`, `road_width`, `starting_soldiers`, `fire_rate`, `max_fire_rate`, `projectile_pool`, `projectile_length`, `soldier_model`, `boss_model`, `manpower_card_model`, `firepower_card_model`, `player_color`, `enemy_color`, `boss_color`, `decoration_color`, `op`, `param`, `val`, `x`, `z`, `model`, `color`, `power`, `effective`, and `strength`.
- Operation enum values: `plus`, `minus`, `div`, `times`.
- Param enum values: `manpower`, `firepower`.
- Number values: signed integer and decimal values, with optional `f`/`F` suffix.
- Hex color values: `#RRGGBB` and `#RRGGBBAA`.
- Path reference values: values that look like asset or model paths, including slash-separated paths and `.obj` files.
- Object categories: `cards`, `decorations`, `enemy_brigades`, `bosses`, plus parser aliases `enemies` and `boss`.
- Comments: `# ` through end of line, so color values such as `#1FB8EBFF` remain highlightable values.
- Separators and punctuation: `:`, `,`, `-`, `(`, and `)`.

## Highlighting Behavior

- A bare identifier followed by `:` is highlighted as a key.
- A known object category followed by `:` at the end of a logical line is highlighted as an object category.
- Enum values are highlighted only when they appear as standalone value tokens.
- Hex color values are highlighted only when they contain exactly 6 or 8 hexadecimal digits after `#`.
- Path-like values are highlighted when they contain `/` or `\`, or when they end in `.obj`.
- Unknown free text remains plain text so level names such as `The Raven's Bend` are not over-highlighted.

## Build

The module uses the IntelliJ Platform Gradle Plugin 2.x. For local builds it prefers an installed IntelliJ IDE instead of downloading the platform:

- `-PintellijPlatformLocalPath=/path/to/idea`
- `INTELLIJ_PLATFORM_LOCAL_PATH=/path/to/idea`
- auto-detected JetBrains Toolbox installs under `~/.local/share/JetBrains/Toolbox/apps/intellij-idea-ultimate` or `~/.local/share/JetBrains/Toolbox/apps/intellij-idea-community`

If no local IDE is found, the build falls back to IntelliJ IDEA Community 2024.3.6, which is the expected CI behavior.

The default build artifact is produced by:

```bash
./gradlew :level-intellij-plugin:buildPlugin
```

The plugin ZIP is written under:

```text
level-intellij-plugin/build/distributions/
```

## CI

The plugin has a separate GitHub Actions workflow. It is tag based and can also be run manually. The workflow builds the plugin ZIP and uploads it to the GitHub release matching the tag, creating the release if needed.
