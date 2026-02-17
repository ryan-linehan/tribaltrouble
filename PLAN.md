# Plan: Unit Settings Tab in Game Creation Menu

## What's Requested (Issue #136 - Unit Settings Subset)

Players want adjustable per-unit-type stats as game creation settings, exposed in a new tab alongside the existing "Standard Options" and "Advanced Options" tabs. The specific stats requested:

| Unit Type | Configurable Stats |
|---|---|
| **Warriors (Rock/Iron/Rubber, both races)** | HP, speed (per-tier, per-race), damage, defense chance (dodge %), attack range, attack speed |
| **Viking Iron Warrior (Berserker)** | Melee vs ranged toggle, 1.5x speed |
| **Native Iron Warrior (Assassin)** | Attack range (11 vs 7), 0.6x attack speed |
| **Viking Rubber Warrior (Chicken Punisher)** | Bounce chance (100% first bounce) |
| **Native Rubber Warrior (Chicken Striker)** | No bounce, 3x attack speed |
| **Peons** | HP, speed, damage, defense chance, aggro range |
| **Chieftains** | HP, speed, damage, defense chance, HP regeneration, aggro range |
| **Buildings (Quarters/Armory/Tower)** | HP |
| **Towers** | Continuous targeting (no pause after kill) |
| **Tower build cost** | Peons needed (10 vs 20) |
| **Chicken warrior cost** | Resource cost (2w+1c vs 2w+1r+1i+1c) |

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

**2. Create a "Unit Settings" Panel in `TerrainMenu.java`** - A third tab added to the `PanelGroup`. The tab needs sub-sections since there are many settings. Uses sliders (reusing existing `Slider` widget) and checkboxes:

  **Warriors section** (per-race where TT2 differs):
  - Rock Warrior HP multiplier
  - Rock Warrior Speed multiplier
  - Iron Warrior HP multiplier
  - Iron Warrior Speed multiplier (TT2 Viking: 1.5x, Native: ~1.25x)
  - Iron Warrior melee toggle (Viking only in TT2 — checkbox)
  - Iron Warrior attack range (TT2 Native Assassin: 11 grid squares vs default 7)
  - Iron Warrior attack speed (TT2 Native Assassin: 0.6x)
  - Rubber Warrior bounce chance (TT2 Viking Chicken Punisher: 100%, Native Chicken Striker: 0%)
  - Rubber Warrior attack speed (TT2 Native Chicken Striker: 3x)
  - Warrior Defense Chance (dodge %) — primary survivability stat since base HP is 1
  - Warrior Damage multiplier

  **Peons section:**
  - HP, Speed, Defense Chance
  - Aggro range multiplier

  **Chieftains section** (per-race):
  - Viking Chieftain HP, Native Chieftain HP
  - Speed, Defense Chance, Damage
  - HP regeneration rate (TT2: slow regen; TT1: none)
  - Aggro range multiplier

  **Buildings section:**
  - Building HP multiplier
  - Tower continuous targeting toggle (TT2: no pause after kill)
  - Tower build cost — peons needed (TT2: 10 vs TT1: 20)

  **Economy section:**
  - Chicken warrior resource cost toggle (TT2: 2w+1c; TT1: 2w+1r+1i+1c)

  The existing slider infrastructure (`Slider` class, 0-N range, value listeners) is directly reusable.

**3. Thread `UnitSettings` through the startup path:**
  - `TerrainMenu` → `WorldParameters` (add UnitSettings field) → `RacesResources` (read values instead of hardcoded constants)
  - `RacesResources.load()` currently takes `(RenderQueues queues)` - would need to also accept `UnitSettings`

**4. Wire into `Game.java` for multiplayer sync** - Add UnitSettings to the Game object so it's transmitted to all players. This touches the network serialization.

### Tier 2: Moderate Complexity (combat behavior changes)

**5. Melee vs Ranged toggle for Iron Warriors** - Currently all warriors have `Abilities.THROW`. Making Viking iron warriors melee (Berserker) means:
  - Changing their `Abilities` to remove `THROW`
  - Swapping their `WeaponFactory` from `IronAxeWeapon` (throwing) to an `InstantHitFactory` (melee)
  - Melee units deal more damage to towers (already in engine?)
  - This is a boolean toggle, not a slider, so use a `CheckBox` widget (already exists in the UI toolkit)

**6. Per-race differentiation** - TT2 had asymmetric races. This requires per-race fields in UnitSettings rather than global multipliers. The key asymmetries:
  - Viking Iron (Berserker): melee, 1.5x speed
  - Native Iron (Assassin): 11 range, 0.6x attack speed
  - Viking Rubber (Chicken Punisher): 100% first bounce
  - Native Rubber (Chicken Striker): 0% bounce, 3x attack speed
  - All Native warriors and Native Chieftain: faster movement (= peon speed)

**7. Attack range** - `THROW_RANGE = 6f` in `RacesResources.java`. Making this per-unit-type for the Assassin (11 grid squares). Affects AI targeting decisions.

**8. Attack speed multiplier** - Embedded in weapon factory classes (`ThrowingFactory`, `InstantHitFactory`). Parameterizing weapon animation timing for Assassin (0.6x) and Chicken Striker (3x).

**9. Rubber/Chicken warrior bounce behavior** - `RubberAxeWeapon`/`RubberSpearWeapon` have hardcoded bounce probability. Need per-race configurable: Viking 100% first bounce, Native 0% bounce.

### Tier 3: Deeper Engine Changes

**10. Map code encoding** - Currently the map code encodes all settings into a BigInteger. Adding unit settings would either:
  - Extend the BigInteger encoding (breaks backward compatibility with old map codes)
  - Or keep unit settings separate from map code (simpler, recommended)

**11. Chieftain HP regeneration** - TT2 chieftains slowly regenerated HP. TT1 has no HP regen mechanic at all. This requires a new periodic heal tick in the unit update loop.

**12. Aggro range for peons and chieftains** - TT2 had smaller aggro range. This is likely in the AI/targeting code and affects when units auto-engage enemies.

**13. Tower continuous targeting** - In TT1, units in towers pause ~1-2 seconds after each kill before targeting the next enemy. TT2 removed this pause so towers immediately switch targets. This is in the tower attack/targeting logic.

**14. Tower build cost** - TT2 required only 10 peons with wood to fully build a tower (TT1: 20). This is in building construction logic.

**15. Chicken warrior resource cost** - TT2: 2 wood + 1 chicken. TT1: 2 wood + 1 rock + 1 iron + 1 chicken. This is in the armory/weapon crafting recipe definitions.

## TT2 Feature Mapping (from Issue #136)

These are the TT2 changes compared to TT1, with original TT2 unit names. The goal is to make all of these configurable as game creation settings so players can toggle "TT2 mode."

### Viking units:
- **Rock Warrior → Axeman**: More HP
- **Iron Warrior → Berserker**: More HP, melee attacks, 1.5x movement speed (faster than peons), more tower damage from melee
- **Rubber Warrior → Chicken Punisher**: 100% chance for 1st bounce (TT1: sometimes hits only 1 enemy)

### Native units:
- **Rock Warrior → Spearman**: More HP (same as Axeman)
- **Iron Warrior → Assassin**: 11 grid square attack range (TT1: 7), 0.6x attack speed
- **Rubber Warrior → Chicken Striker**: No bounce, 3x attack speed
- All Native warriors and Native Chieftain have faster movement (= peon speed)

### Other:
- All buildings and chieftains have more HP
- Chieftains regenerate HP at a slow rate
- Peons and chieftains have smaller aggro range
- Towers: 10 peons to build (TT1: 20), continuous targeting (no 1-2s pause after kill)
- Chicken warrior cost: 2 wood + 1 chicken (TT1: 2 wood + 1 rock + 1 iron + 1 chicken)

## Recommended Approach

Start with **Tier 1** items (1-4): a `UnitSettings` data class, a new UI tab with sliders for HP/speed/damage/defense, and threading it through to `RacesResources`. Then add Tier 2 (combat behavior: melee toggle, per-race stats, attack range/speed, bounce). Tier 3 (regen, aggro, tower targeting, build costs) can follow.

A "TT2 Mode" preset button could set all sliders to TT2 values at once, so players don't have to manually configure 20+ settings.

Key design decisions:
- **Per-race from the start**: Since TT2's core identity is asymmetric races (Berserker vs Assassin), UnitSettings needs per-race fields, not global multipliers
- **Map code**: Don't encode unit settings into map code. Keep them as a separate config that's part of the Game object but not the map code string
- **Defaults**: All settings default to current TT1 hardcoded values so existing gameplay is unchanged
- **Slider ranges**: Use multiplier-style sliders (e.g., 0.5x to 3x) rather than absolute values, so the UI is intuitive regardless of the base stat

### Files to create/modify:
| File | Action |
|---|---|
| `UnitSettings.java` (new) | Data class for all configurable unit params (per-race) |
| `TerrainMenu.java` | Add third "Unit Settings" tab with sliders/checkboxes |
| `WorldParameters.java` | Add UnitSettings field |
| `RacesResources.java` | Read from UnitSettings instead of hardcoded constants |
| `Game.java` | Add UnitSettings for multiplayer serialization |
| `TerrainMenu.properties` | Add i18n strings for new UI labels |
| `RubberAxeWeapon.java` / `RubberSpearWeapon.java` | Parameterize bounce chance |
| `ThrowingFactory.java` / weapon factories | Parameterize attack speed and range |
| Tower targeting logic | Continuous targeting toggle |
| Building construction logic | Tower build cost |
| Armory/weapon recipe logic | Chicken warrior resource cost |
