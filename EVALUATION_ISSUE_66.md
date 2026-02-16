# Issue #66: Click + Drag Selection Preference — Implementation Evaluation

## Summary

This issue requests a selection filtering feature: when a player click+drags to select units, only units matching the current "selection preference" are included. A hotkey cycles through preference options (All, Peon, Warriors + Chief, Rock warrior, Iron warrior, Chicken warrior, Chief), and optionally a separate hotkey resets to "All".

## Complexity Assessment

**Overall Complexity: Medium**

The feature touches well-encapsulated parts of the codebase and doesn't require changes to networking, game simulation determinism, or rendering pipelines. The main challenge is cleanly integrating the filter into the existing selection flow and providing clear visual feedback.

## Architecture Analysis

### Current Selection Flow

1. **`SelectionDelegate.mousePressed()`** (`tt/classes/com/oddlabs/tt/delegate/SelectionDelegate.java:421`) — records drag start coordinates
2. **`SelectionDelegate.mouseDragged()`** (`:398`) — updates drag endpoint
3. **`SelectionDelegate.mouseClicked()`** (`:338`) — calls `Picker.pickBoxed()` which returns all `Selectable[]` in the box
4. **`SelectionDelegate.mouseClicked()`** (`:356-374`) — sorts the picked results into `friendly_units`, `friendly_building`, `enemy`; then calls `replaceSelection()` or `updateSelection()`
5. **`replaceSelection()`** (`:323`) — clears current selection and adds all friendly units

The filtering logic should be inserted at **step 4**, after `pickBoxed()` returns results but before they're added to the selection. This is the cleanest interception point.

### Unit Type Identification

Units don't store a type index. The only way to determine type is by comparing a `Unit`'s `UnitTemplate` against the player's `Race` templates:

```java
Race race = unit.getOwner().getRace();
UnitTemplate template = ((Unit) selectable).getUnitTemplate();
// Compare: template == race.getUnitTemplate(Race.UNIT_WARRIOR_ROCK), etc.
```

The five unit types from `Race.java:16-20`:
| Constant | Index | Issue Name |
|---|---|---|
| `UNIT_WARRIOR_ROCK` | 0 | Rock warrior |
| `UNIT_WARRIOR_IRON` | 1 | Iron warrior |
| `UNIT_WARRIOR_RUBBER` | 2 | Chicken warrior |
| `UNIT_PEON` | 3 | Peon |
| `UNIT_CHIEFTAIN` | 4 | Chief |

The `Abilities` bitmask can distinguish broad categories (Peon has `BUILD`, warriors have `THROW`, Chieftain has `MAGIC`) but cannot distinguish between warrior subtypes. Template comparison is required for full type discrimination.

### Hotkey Infrastructure

Keyboard handling lives in `SelectionDelegate.keyPressed()` (`:74`). The key codes are in `Keyboard.java`. Many keys are already used for game actions. Good candidates for the cycle hotkey: **KEY_V** or **KEY_GRAVE** (backtick/tilde). These are currently unused and ergonomically accessible.

### HUD Feedback

The `InfoPrinter` class (`tt/classes/com/oddlabs/tt/gui/InfoPrinter.java`) can display temporary text messages on screen. It's accessible via `getViewer().getGUIRoot().getInfoPrinter()` and has a `print(String text)` method that shows text for 8 seconds. This could serve as the simplest notification mechanism when cycling preferences. A more polished approach would be a persistent small label rendered by `SelectionDelegate.render2D()`.

## Implementation Plan

### Files to Modify

| File | Change |
|---|---|
| `tt/classes/com/oddlabs/tt/delegate/SelectionDelegate.java` | Add preference state, hotkey handling, filtering logic, HUD rendering |
| `tt/classes/com/oddlabs/tt/model/Race.java` | Add helper method `getUnitTypeIndex(UnitTemplate)` |
| `tt/classes/com/oddlabs/tt/delegate/SelectionDelegate.properties` | Add localized strings for preference names |

### New Files (Optional)

| File | Purpose |
|---|---|
| `tt/classes/com/oddlabs/tt/viewer/SelectionPreference.java` | Encapsulate preference state and filtering logic (optional, could be inline in SelectionDelegate) |

### Step-by-Step Implementation

#### 1. Add helper to `Race.java` to identify unit types by template

```java
public final int getUnitTypeIndex(UnitTemplate template) {
    for (int i = 0; i < units.length; i++) {
        if (units[i] == template) return i;
    }
    return -1;
}
```

This enables O(1) type checking per unit by comparing the template reference.

#### 2. Define selection preference constants and state in `SelectionDelegate`

Add to `SelectionDelegate.java`:

```java
// Selection preference modes
private static final int PREF_ALL = 0;
private static final int PREF_PEON = 1;
private static final int PREF_WARRIORS_AND_CHIEF = 2;
private static final int PREF_WARRIOR_ROCK = 3;
private static final int PREF_WARRIOR_IRON = 4;
private static final int PREF_WARRIOR_RUBBER = 5;
private static final int PREF_CHIEF = 6;
private static final int NUM_PREFERENCES = 7;
private static final String[] PREF_NAMES = {
    "All", "Peon", "Warriors + Chief",
    "Rock Warrior", "Iron Warrior", "Chicken Warrior", "Chief"
};

private int selection_preference = PREF_ALL;
```

#### 3. Add hotkey handling in `keyPressed()`

Add a case to the switch in `SelectionDelegate.keyPressed()`:

```java
case Keyboard.KEY_V:  // or another available key
    if (!map_mode && !observer) {
        selection_preference = (selection_preference + 1) % NUM_PREFERENCES;
        // Show feedback via InfoPrinter or a persistent label
        getViewer().getGUIRoot().getInfoPrinter()
            .print("Selection: " + PREF_NAMES[selection_preference]);
    }
    break;
```

Optionally, a reset hotkey (e.g., `CTRL+V`) could reset to `PREF_ALL`.

#### 4. Add filtering logic

Create a filter method in `SelectionDelegate`:

```java
private boolean matchesPreference(Selectable selectable) {
    if (selection_preference == PREF_ALL) return true;
    if (!(selectable instanceof Unit)) return selection_preference == PREF_ALL;
    Unit unit = (Unit) selectable;
    Race race = unit.getOwner().getRace();
    int type = race.getUnitTypeIndex(unit.getUnitTemplate());

    switch (selection_preference) {
        case PREF_PEON:
            return type == Race.UNIT_PEON;
        case PREF_WARRIORS_AND_CHIEF:
            return type == Race.UNIT_WARRIOR_ROCK
                || type == Race.UNIT_WARRIOR_IRON
                || type == Race.UNIT_WARRIOR_RUBBER
                || type == Race.UNIT_CHIEFTAIN;
        case PREF_WARRIOR_ROCK:
            return type == Race.UNIT_WARRIOR_ROCK;
        case PREF_WARRIOR_IRON:
            return type == Race.UNIT_WARRIOR_IRON;
        case PREF_WARRIOR_RUBBER:
            return type == Race.UNIT_WARRIOR_RUBBER;
        case PREF_CHIEF:
            return type == Race.UNIT_CHIEFTAIN;
        default:
            return true;
    }
}
```

#### 5. Apply the filter in `mouseClicked()`

Modify the loop in `mouseClicked()` at line 359 where picked units are categorized:

```java
for (int i = 0; i < picked.length; i++) {
    Selectable selectable = picked[i];
    if (selectable != null) {
        if (selectable.getOwner() == getViewer().getLocalPlayer()) {
            if (selectable instanceof Building) friendly_building = selectable;
            else if (selectable instanceof Unit) {
                if (matchesPreference(selectable)) {  // <-- add this check
                    friendly_units.add(selectable);
                }
            }
            else throw new RuntimeException();
        } else {
            enemy = selectable;
        }
    }
}
```

Note: The filter should only apply to **drag selection** (box select), not single-click selection. Since single clicks go through `createSinglePick()` in `Picker.java` and the threshold check (`abs < SELECTION_THRESHOLD`), we can check `abs(x1-x2) >= SELECTION_THRESHOLD || abs(y1-y2) >= SELECTION_THRESHOLD` before applying the filter, or simply always apply it (including for single-click) which may also be desirable UX.

#### 6. Visual feedback (critical — not optional)

A filter that silently changes selection behavior is a serious UX hazard. If a player
accidentally hits the cycle key, drag-select will appear broken with no explanation.
The game already has this problem with `aggressive_units` (Ctrl+A), which only shows
a transient 8-second InfoPrinter message. A selection filter would be far worse because
it makes the primary interaction — drag-selecting units — seem non-functional.

**Three layers of feedback are recommended (implement at least the first two):**

##### Layer 1: Persistent on-screen label (required)

Follow the existing **observer mode label** pattern (`SelectionDelegate.java:29-72`).
When in observer mode, a `Label` is added as a persistent child of the delegate, using
`Skin.getSkin().getHeadlineFont()`, centered at the top of the screen. This label is
always visible until the mode is exited.

```java
// In the constructor, create the label (hidden initially):
private Label selection_filter_label;

// In constructor:
this.selection_filter_label = new Label("", Skin.getSkin().getHeadlineFont());

// When preference changes:
if (selection_preference == PREF_ALL) {
    selection_filter_label.remove();
} else {
    // Use BackgroundLabelBox for a visible background behind the text
    selection_filter_label.remove();
    selection_filter_label = new Label(
        "Selection Filter: " + PREF_NAMES[selection_preference],
        Skin.getSkin().getHeadlineFont());
    addChild(selection_filter_label);
}

// Position in displayChangedNotify():
selection_filter_label.setPos(
    (width - selection_filter_label.getWidth()) / 2,
    height - selection_filter_label.getHeight() - 5);
```

For even better visibility, wrap in a `BackgroundLabelBox` (used by `InfoPrinter`) which
renders text over a semi-transparent background box via `Skin.getSkin().getBackgroundBox()`.

**Estimated code: ~15 lines.**

##### Layer 2: Selection rectangle color change (required)

The drag-selection rectangle is currently always blue (`r=0.3, g=1.0, b=0.0, a=1.0`).
When a filter is active, tint it a different color — this provides feedback exactly where
the player is already looking (at their drag box). For example, orange for filtered mode:

```java
public final void render2D() {
    if (selection) {
        if (selection_preference != PREF_ALL) {
            // Orange box when filter is active
            GW.renderRect(selection_x1, selection_x2, selection_y1, selection_y2,
                          0f, 1f, .6f, 0f, 1f);
        } else {
            // Default blue box
            GW.renderRect(selection_x1, selection_x2, selection_y1, selection_y2,
                          0f, .3f, 1f, 0f, 1f);
        }
    }
}
```

**Estimated code: ~5 lines (an if/else around the existing renderRect call).**

##### Layer 3: Transient notification on toggle (nice-to-have)

An `InfoPrinter.print()` message when cycling provides immediate confirmation of the
action but disappears after 8 seconds. Useful as supplementary feedback, but must not
be the only indicator.

```java
getViewer().getGUIRoot().getInfoPrinter()
    .print("Selection Filter: " + PREF_NAMES[selection_preference]);
```

**Estimated code: 1 line.**

##### Why all three layers matter

| Scenario | Layer 1 (Label) | Layer 2 (Box color) | Layer 3 (Toast) |
|---|---|---|---|
| Player cycles preference intentionally | Confirms current mode | Reinforces during drag | Immediate feedback |
| Player accidentally hits hotkey | **Catches the mistake** — always visible | Noticeable during next drag | May already have faded |
| Player returns after AFK | **Still visible** | Visible on next drag | Gone |
| Player is focused on the battlefield, not HUD | May miss it | **Catches attention** — right at the cursor | May miss it |

No single layer covers all cases. The persistent label is the most important (covers the
"accidental press" and "returned from AFK" cases), the box color change is the most
noticeable during active gameplay, and the toast is the most immediately responsive.

## Design Considerations

### Should the filter apply to single-click selection?
**Recommendation: No.** Single-click should always select whatever is clicked, regardless of preference. Only drag/box selection should filter. This avoids confusion when a player clicks directly on a unit and nothing happens.

### Should the filter apply to SHIFT+drag (additive selection)?
**Recommendation: Yes.** When shift-dragging to add units, the filter should still apply to the newly added units. The existing selection should not be modified.

### Should the preference persist across games?
**Recommendation: No.** The preference should reset to "All" at the start of each game. It's a transient tactical tool, not a persistent setting.

### Should the filter affect Ctrl+number group recall?
**Recommendation: No.** Saved army groups (Ctrl+1-9) should recall all units regardless of preference. The preference only affects new drag selections.

### What happens when filtering results in zero units selected?
**Recommendation: No selection change.** If the filter causes the drag to select nothing (e.g., preference is "Peon" but only warriors are in the box), the selection should remain unchanged (or clear, matching current behavior when dragging over empty terrain). The current `replaceSelection()` already handles empty `friendly_units` list correctly.

### Keyboard shortcut choice
Several keys are available. Recommended: **V** (for "filter" — F is taken). Alternatives: **GRAVE/TILDE** (`~`), **BACKSLASH** (`\`), or **SEMICOLON** (`;`). The key should be easy to reach during gameplay.

### Interaction with double-click select-all-of-type
Currently, double-clicking a warrior selects all warriors on screen (via `Abilities.THROW`), and double-clicking a peon selects all peons (`Abilities.HARVEST`). The selection preference feature is complementary — it filters drag selection, while double-click is template/ability-based. These can coexist without conflict.

## Testing Considerations

- This game has **no automated test suite** — verification must be manual
- Test all 7 preference modes with drag selection
- Test single-click is unaffected by preference
- Test SHIFT+drag with active preference
- Test preference cycling wraps correctly
- Test preference doesn't affect Ctrl+number group recall
- Test with mixed unit compositions (peons + multiple warrior types + chieftain)
- Test against enemy units (enemy should still be selectable when no friendly units match)
- Test in observer mode (preferences should be inactive)
- Test in map mode (preferences should be inactive)
- Build verification: `ant compile` from root, `ant build-linux` from `tt/`

## Estimated Scope

- **Lines of code changed**: ~100-140 lines across 2-3 files (includes visual feedback)
- **Risk level**: Low — changes are isolated to client-side selection logic with no impact on game simulation determinism, networking, or rendering pipelines
- **No new dependencies** required
- **No database changes** required
- **No new assets** required (unless a dedicated icon is desired for the HUD indicator)
