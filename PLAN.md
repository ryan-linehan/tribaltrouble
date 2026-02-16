# Plan: Unit Settings Tab in Game Creation Menu

## What's Requested (Issue #136 - Unit Settings Subset)

Players want adjustable per-unit-type stats as game creation settings, exposed in a new tab alongside the existing "Standard Options" and "Advanced Options" tabs. The specific stats requested:

| Unit Type | Configurable Stats |
|---|---|
| **Warriors (Rock/Iron/Rubber, both races)** | HP, speed, damage, defense chance (dodge %) |
| **Iron Warrior specifically** | Melee vs ranged toggle (currently all warriors are ranged/throwing) |
| **Peons** | HP, speed, damage, defense chance |
| **Chieftains** | HP, speed, damage, defense chance |
| **Buildings (Quarters/Armory/Tower)** | HP |

## Current Architecture

### How settings flow today:
1. **TerrainMenu.java** - UI with 2 tabs: "Standard Options" (size, terrain, players) and "Advanced Options" (hills/vegetation/supplies sliders, map code)
2. **PanelGroup** - Tab container. Takes a `Panel[]` array, trivially extensible to 3 tabs
3. **Game.java** - Serializable settings object sent over the network for multiplayer
4. **WorldParameters.java** - Passed to the game engine at startup
5. **RacesResources.java** - Where all unit templates are constructed with hardcoded stats (HP, speed, weapon factories, etc.)
6. **UnitTemplate.java** - Immutable value object holding a unit's stats (meters_per_second, max_hit_points, weapon_factory, etc.)

### Current unit defaults:
Warriors have only 1 HP but survive through `defense_chance` (dodge probability) defined in `Template.java`. This is the primary survivability mechanic for warriors — not HP.

| Unit | HP | Defense Chance | Speed | Damage | Effective Survivability |
|---|---|---|---|---|---|
| Rock Warrior | 1 | 50% | 4 m/s | 1 | ~2 hits to kill |
| Iron Warrior | 1 | 70% | 4 m/s | 2 | ~3.3 hits to kill |
| Rubber Warrior | 1 | 70% | 4 m/s | 2 | ~3.3 hits to kill |
| Peon | 1 | 0% | 5 m/s | 1 (melee) | 1 hit to kill |
| Viking Chieftain | 60 | 50% | 4 m/s | 3 (melee) | Very tanky |
| Native Chieftain | 40 | 50% | 4 m/s | 3 (melee) | Tanky |
| Quarters | 200 | — | — | — | — |
| Armory | 200 | — | — | — | — |
| Tower | 100 | — | — | — | — |

## Implementation Plan

### Tier 1: Simple (slider-based numeric stats)

These are straightforward because they're just numbers plugged into `UnitTemplate` constructors:

**1. Create `UnitSettings.java`** - A new data class holding all configurable unit parameters with defaults matching current hardcoded values. Fields are simple primitives (int/float/boolean). Must be `Serializable` for network play.

**2. Create a "Unit Settings" Panel in `TerrainMenu.java`** - A third tab added to the `PanelGroup`. Contains sliders (reusing existing `Slider` widget) for:
  - Warrior HP (shared across rock/iron/rubber, per race or global - recommend global for simplicity)
  - Warrior Speed
  - Warrior Damage multiplier (or per-tier)
  - Warrior Defense Chance (dodge %) — this is the primary survivability stat for warriors since base HP is 1
  - Peon HP, Speed, Defense Chance
  - Chieftain HP (Viking), Chieftain HP (Native), Defense Chance
  - Building HP multiplier

  The existing slider infrastructure (`Slider` class, 0-N range, value listeners) is directly reusable.

**3. Thread `UnitSettings` through the startup path:**
  - `TerrainMenu` → `WorldParameters` (add UnitSettings field) → `RacesResources` (read values instead of hardcoded constants)
  - `RacesResources.load()` currently takes `(RenderQueues queues)` - would need to also accept `UnitSettings`

**4. Wire into `Game.java` for multiplayer sync** - Add UnitSettings to the Game object so it's transmitted to all players. This touches the network serialization.

### Tier 2: Moderate Complexity

**5. Melee vs Ranged toggle for Iron Warriors** - Currently all warriors have `Abilities.THROW`. Making iron warriors melee means:
  - Changing their `Abilities` to remove `THROW`
  - Swapping their `WeaponFactory` from `IronAxeWeapon`/`IronSpearWeapon` (throwing) to an `InstantHitFactory` (melee)
  - This is a boolean toggle, not a slider, so use a `CheckBox` widget (already exists in the UI toolkit)
  - Moderate because it changes combat behavior, not just a number

**6. Per-race differentiation** - The issue wants Vikings and Natives to potentially have different stats. This doubles the number of sliders but is architecturally the same.

### Tier 3: More Work Required

**7. Map code encoding** - Currently the map code encodes all settings into a BigInteger. Adding unit settings would either:
  - Extend the BigInteger encoding (breaks backward compatibility with old map codes)
  - Or keep unit settings separate from map code (simpler, recommended)

**8. Attack speed / throw range** - These are embedded deeper in weapon factory classes (`ThrowingFactory`, `InstantHitFactory`). Adjustable attack speed means parameterizing the weapon animation timing. Throw range is a constant (`THROW_RANGE = 6f`) that could be made configurable but affects AI behavior.

**9. Rubber/Chicken warrior bounce behavior** - The issue mentions 100% bounce chance for Vikings and no bounce for Natives. This is in `RubberAxeWeapon`/`RubberSpearWeapon` and would need the bounce probability to be configurable rather than hardcoded.

## Recommended Approach

Start with **Tier 1** items (1-4): a `UnitSettings` data class, a new UI tab with sliders for HP/speed/damage, and threading it through to `RacesResources`. This gives players the most impactful controls with the least complexity.

Key design decisions:
- **Global vs per-race sliders**: Start with global multipliers (1 slider affects both races equally), add per-race later
- **Map code**: Don't encode unit settings into map code. Keep them as a separate config that's part of the Game object but not the map code string
- **Defaults**: All sliders default to current hardcoded values so existing gameplay is unchanged
- **Slider ranges**: Use multiplier-style sliders (e.g., 0.5x to 3x) rather than absolute values, so the UI is intuitive regardless of the base stat

### Files to create/modify:
| File | Action |
|---|---|
| `UnitSettings.java` (new) | Data class for all configurable unit params |
| `TerrainMenu.java` | Add third "Unit Settings" tab with sliders |
| `WorldParameters.java` | Add UnitSettings field |
| `RacesResources.java` | Read from UnitSettings instead of hardcoded constants |
| `Game.java` | Add UnitSettings for multiplayer serialization |
| `TerrainMenu.properties` | Add i18n strings for new UI labels |
