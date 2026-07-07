# Crowd Master Arcade Level VS Code Extension Specification

## Purpose

This independent module packages a VS Code extension that provides syntax highlighting for Crowd Master Arcade level definition files. It is separate from the game runtime and Gradle modules.

## File Type

The extension contributes a language named `Crowd Master Arcade Level` with language id `cma-level`.

Highlighting applies to:

- `*.level`
- `*.cma-level`

## Token Classes

The TextMate grammar recognizes the same lightweight level format parsed by the game and highlighted by the IntelliJ plugin:

- Section/object categories: `cards`, `decorations`, `background_decorations`, `enemy_brigades`, `bosses`, plus parser aliases `enemies` and `boss`.
- Key names before `:`, including common level, model, color, card, enemy, boss, and decoration fields.
- Operation enum values: `plus`, `minus`, `div`, `times`.
- Param enum values: `manpower`, `firepower`, `bulletpower`, `bullet_power`, `soldierlife`, and `soldier_life`.
- Number values: signed integer and decimal values, with optional `f`/`F` suffix.
- Hex colors: `#RRGGBB` and `#RRGGBBAA`.
- Path references: slash-separated paths, backslash-separated paths, and `.obj` files.
- Comments: only `# ` through end of line, so color values remain valid tokens.
- Separators and punctuation: `:`, `,`, `-`, `(`, and `)`.

## Build

The module is Node/npm based and does not depend on the game Gradle build.

```bash
cd level-vscode-extension
npm install
npm run package -- --out ../dist/crowd-master-arcade-level-vscode-extension.vsix
```

The package command uses `@vscode/vsce`.

## CI

The plugin GitHub Actions workflow builds both editor support packages:

- IntelliJ plugin ZIP
- VS Code extension VSIX

Both artifacts are uploaded to the workflow run and to the GitHub release selected by the manual tag input.
