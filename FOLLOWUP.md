# Follow-Up: Campaign Menu and Level Editor

This file tracks the larger follow-up work requested after the 2026-07-04 gameplay fixes.

## Issue #3: VisUI Campaign Menu

Status: implemented for menu/HUD foundation.

Implemented:

- VisUI dependency and skin initialization.
- VisUI HUD stage with the required single-line HUD content.
- VisUI startup campaign menu.
- Level table with name, points, total possible points, percentage, and state.
- Arrow-key selection and Enter-to-play.
- Mouse/touch row selection and action buttons.
- Play, Test, and Reset Data Home actions.
- Exit action.
- Last selected/active level persistence in `.crowdmaster/campaign-state.properties`.
- Edit, Create, and Delete are wired to the issue #4 editor flow.
- Gameplay `Esc` returns to the main screen; main-screen `Esc` exits.
- Level rows are selectable across their full width and highlighted when selected.
- Level rows use a tabular multi-column layout rather than proportional formatted text.
- Locked levels remain selectable while `Play` is disabled.

Remaining refinements:

- refine campaign unlock/access policy if needed
- improve visual layout polish after editor UI is introduced

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

Status: issue #4, first implementation complete.

Implemented:

- `Edit` opens the selected `.crowdmaster` level file.
- `Create` creates a new user level file in `.crowdmaster` and opens it.
- `Delete` confirms and deletes the selected user level file.
- Editor screen uses VisUI command, property, and prototype panels.
- Right prototype palette supports cards, enemy brigades, decorations, and bosses.
- Left property table supports scene and selected object properties.
- Text/number fields write through to the editable level draft.
- Color fields support `#RRGGBBAA` text plus VisUI color picker.
- Model fields support text plus VisUI file chooser.
- Paths under `.crowdmaster` are saved relative to `.crowdmaster`.
- Save writes the `.level` file through `LevelTextWriter`.
- Undo/redo works through serialized level snapshots.
- Delete key and Delete Selected remove the selected object.
- Unsaved exit prompts before leaving the editor.
- The preview rebuilds through `AppModelFactory`, reusing gameplay model loading and formation layout.
- Editor preview renders through a dedicated orthographic `WorldRenderer` mode.
- Scene clicks use a camera ray projected onto the road plane.
- Object picking tests the ray-plane point against card, brigade, decoration, and boss footprints.
- Selected objects are highlighted with model-space yellow bounding boxes.
- Property changes rebuild the preview after the requested 800 ms debounce.
- Editor camera supports pan, zoom, and rotate controls.
- Editor camera target is constrained to the ground/causeway area.
- Editor render path includes the same shadow-map lighting pass as gameplay.
- Editor renders a translucent road extension past the declared road length when placed content extends beyond it.
- Editor picking uses the projected 3D selection boxes, improving selection of cards, bosses, and compact formations.
- Selection box depth is drawn around the selected object instead of offset in front of it.
- Selection boxes are drawn as explicit 3D edges.
- Editor camera scroll/drag input is routed before VisUI scene focus, preserving zoom after text-field edits.
- Delete-key object deletion is suppressed while a VisUI text field has keyboard focus.

Remaining refinements:

- Improve model-space box height by reading full model Y bounds for all custom model types.
- Improve row/table sizing and visual polish.

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
