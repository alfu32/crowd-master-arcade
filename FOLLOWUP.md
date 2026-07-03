# Follow-Up: Campaign Menu and Level Editor

This file tracks the larger follow-up work requested after the 2026-07-04 gameplay fixes.

## Immediate Next Milestone

1. Add VisUI dependency and initialize VisUI skin safely on desktop and Android.
2. Introduce app-level screens/states:
   - main menu
   - gameplay
   - level editor
3. Replace the temporary HUD renderer with a VisUI stage overlay while preserving the single-line HUD content.
4. Extend `CampaignStats` to persist:
   - completion/won flag
   - best points per level
   - possible points per level
   - percentage achieved

Already implemented:

- last selected/active level is persisted in `.crowdmaster/campaign-state.properties`

## Campaign Menu

Required actions:

- `Play`: continue campaign from the selected level.
- `Test`: run only the selected level.
- `Edit`: open editor for selected level.
- `Create`: create a new user level.
- `Delete`: delete selected user level after confirmation.
- `Reset Data Home`: delete `.crowdmaster` and reseed from packaged assets.

Menu table columns:

- level number
- level name
- user points
- total possible points
- percentage achieved
- access/completion state

Input:

- arrows move selection
- Enter activates default/proceed action
- mouse/touch selects rows and buttons

## Level Editor

Scene:

- orthographic camera
- same model loading and formation layout logic as gameplay
- selected object highlighted by a bounding box

Panels:

- right prototype list: cards, enemy brigades, decorations, bosses
- left properties table: name/value rows
- command buttons: save, undo, redo, delete selected, exit

Property editors:

- numbers: VisUI numeric/text fields
- text: VisUI text fields
- color: `#RRGGBBAA` text field plus VisUI color picker
- model path: text field plus system file picker

Path handling:

- if chosen OBJ is under `.crowdmaster`, save a path relative to `.crowdmaster`
- otherwise save the absolute path

Behavior:

- one selected object at a time
- clicking empty scene selects scene properties
- Delete key deletes selected object, except scene
- unsaved exit requires blocking confirmation
- property changes rebuild preview after an 800 ms debounce

## Risks

- VisUI dependency/version compatibility with libGDX 1.13.5 needs verification.
- Native/system file picker behavior differs by desktop, Android, Snap, and MSIX packaging.
- Editor picking requires a robust ray-to-world/object hit test; keep it separate from gameplay collision.
- Reset data home is destructive and must require explicit confirmation.
