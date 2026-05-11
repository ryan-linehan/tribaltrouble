# Custom Maps — GitHub Issue Drafts

Piecemeal breakdown of the work covered by
[`map-hosting-and-download.md`](./map-hosting-and-download.md). Each entry
is sized to be a single PR. Two natural tracks (Editor and Multiplayer)
share a small foundation and can proceed in parallel after Issue 3.

## Milestones

- **Custom Maps — Foundation:** #1, #2, #3
- **Map Editor:** #4, #5, #6, #7, #8
- **Custom Maps — Multiplayer:** #9, #10, #11
- **Workshop:** #12, #13, #14

## Dependency map

```
1 ─► 2 ─► 3 ─┬─► 4 ─► 5
             │    └► 6 ─► 7 ─► 8 ──────► 13 ─► 14
             └─► 9 ─► 10 ─► 11 ─► 12
```

## Suggested labels

- `enhancement` on all
- One of: `area:maps`, `area:editor`, `area:menu`, `area:multiplayer`, `area:workshop`
- `foundation` on #1–#3

---

## Issue 1 — Foundation: define and implement `.ttmap` file format + codec

**Summary.** Introduce a versioned binary file format for hand-authored
maps, plus reader/writer with golden-file tests. No UI, no networking.

**Scope**
- Header (`"TTMAP"` magic, version, flags, meters_per_world,
  terrain_type, num_players, seed_hint).
- TLV sections: HEIGHTMAP, ACCESS_GRID, BUILD_GRID, TREES, PALMS,
  ROCKS, IRON, PLANTS (optional), STARTS, BLEND_INFO (optional),
  THUMBNAIL (PNG), META (JSON).
- zlib-compressed section payloads with a per-section
  decompressed-size cap.
- Parser bounds-checks every length field before allocating.
- Reject files whose magic/version don't match before any further
  parsing.

**Out of scope.** Filesystem layout, engine integration, UI.

**Acceptance criteria**
- Round-trip test: synthetic `MapFile` → bytes → parsed back → equal.
- Golden-file test: a small `.ttmap` checked into `test/resources`
  parses cleanly.
- Parser rejects: wrong magic, future version, oversize section,
  truncated section, bad CRC (if used).
- Unit-test coverage on length-clamping (e.g.,
  `num_trees = Integer.MAX_VALUE` → rejected, not OOM).

**Files / entry points.** New package `tt/.../map/`: `MapFile`,
`MapMeta`, `MapFileReader`, `MapFileWriter`, `MapFormatException`.

**Depends on.** Nothing.

---

## Issue 2 — Foundation: MapStore for authored and cached maps

**Summary.** Add a filesystem abstraction for the two map locations:
user-authored maps and downloaded cache.

**Scope**
- Authored maps live at `<userdir>/maps/*.ttmap` (Steam-Cloud-syncs
  via `Globals.steamPrefixed`).
- Downloaded maps live at `<userdir>/maps/cache/<sha256>.ttmap`
  (hash-only filenames; not cloud-synced).
- `MapStore` API: `listAuthoredMaps()`, `findByHash(hash)`,
  `saveAuthored(MapFile)`, `cacheDownloaded(hash, bytes)`.
- Cache writes refuse any input whose magic bytes aren't `"TTMAP"`.
- Returns sealed result types; never throws on missing files.

**Out of scope.** LRU eviction (Issue 11), engine integration, UI.

**Acceptance criteria**
- Listing skips files that fail format magic-bytes check.
- Cache writes are atomic (write to tmp + rename).
- `findByHash` is O(1) (just a path check), not a directory scan.
- Hash inputs are validated to match `^[0-9a-f]{64}$` before being
  joined to the cache path.

**Files / entry points.** `tt/.../map/MapStore.java`.

**Depends on.** #1.

---

## Issue 3 — Engine: CustomMapWorldGenerator + WorldParameters plumbing

**Summary.** Teach the engine to load a `.ttmap` into a playable world.
End state: drop a hand-rolled `.ttmap` into `<userdir>/maps/` and start
a game vs. AI from a debug command or test.

**Scope**
- Add `@Nullable Path customMapFile` (and `@Nullable MapMeta`) to
  `WorldParameters`.
- New `CustomMapWorldGenerator implements WorldGenerator` that builds
  a `WorldInfo` from a parsed `MapFile`.
- Rebuild blend textures from terrain type at load time (do not
  persist).
- Procedural path (existing `WorldGenerator`) untouched.

**Out of scope.** Editor, multiplayer transfer, UI to pick the map.

**Acceptance criteria**
- A unit/integration test writes a synthetic `.ttmap` via
  `MapFileWriter`, loads it via `CustomMapWorldGenerator`, and asserts
  the resulting `WorldInfo` matches the input (heightmap, starts,
  object counts).
- Loading is decoupled from procedural generation — `Landscape` is not
  invoked when `customMapFile != null`.
- A debug entry point launches a single-player game vs. AI from a
  chosen `.ttmap` (CLI flag or dev-only menu item — does not need to
  be polished).

**Files / entry points.**
`tt/.../landscape/WorldParameters.java`,
`tt/.../resource/WorldGenerator.java`, new
`tt/.../resource/CustomMapWorldGenerator.java`.

**Depends on.** #1, #2.

---

## Issue 4 — Editor: scaffolding + save/load

**Summary.** Stand up the map editor as a launchable screen. Open
existing `.ttmap` files or create a new map either blank or seeded
from a procedural mapcode/seed. Save/load round-trip works. No
editing tools yet.

**Scope**
- New screen `MapEditor` reusing the in-engine renderer with an
  editor-specific input controller.
- "New Map" dialog with source choice:
  - **Blank** — flat terrain at chosen size + terrain type.
  - **From seed / mapcode** — reuses the existing `TerrainMenu` /
    `MapcodeForm` controls (seed, hills, vegetation, supplies, terrain
    type, size) and invokes `new Landscape(...)` to produce the
    starting heightmap, access/build grids, object lists, and starting
    locations. The editor snapshots that output as its working state;
    the resulting map is no longer tied to the seed (edits diverge).
  - Both flows accept map size and terrain type.
- "Open" reads an existing `.ttmap` via `MapFileReader`.
- "Save" writes through `MapStore.saveAuthored`.
- Exit-to-menu prompts on unsaved changes.
- Header `seed_hint` is set to the originating seed when the map
  starts from one (used later for plant/structure noise regeneration);
  0 otherwise.

**Out of scope.** Brushes, painting, object placement, thumbnail,
test-play.

**Acceptance criteria**
- "New → Blank" produces a flat terrain of the chosen size/type; can
  be saved and reopened identically.
- "New → From seed" with a given mapcode produces a working state
  byte-equivalent to launching that mapcode in a normal game
  (heightmap, object placements, starts).
- Editing a seed-started map and saving produces a `.ttmap` that, when
  reloaded, no longer regenerates from the seed — it loads the stored
  data verbatim.
- Camera pan/zoom/rotate work; no game-logic side effects.
- Closing with unsaved changes shows a confirm dialog.

**Files / entry points.** New `tt/.../editor/` package; new
`MapEditor` screen and `NewMapDialog`. Reuses
`tt/.../form/TerrainMenu.java` and `tt/.../form/MapcodeForm.java` for
the seed-source UI. Invokes `tt/.../procedural/Landscape.java`.

**Depends on.** #1, #2, #3.

---

## Issue 5 — Menu: add "Map Editor" entry to the main menu

**Summary.** Wire a Map Editor entry into the main menu so the editor
is reachable from a fresh launch.

**Scope**
- New "Map Editor" item in the main menu (placement to match existing
  menu style — likely under or near the singleplayer entries).
- Opens the `MapEditor` screen from Issue 4.
- Localization keys added to `*.properties` for all supported
  languages (English populated; other locales fall back to English
  string with a TODO).
- Disabled-with-tooltip if the editor isn't available (e.g., during
  initial loading).

**Out of scope.** Map browser, Workshop browser, lobby-side "Custom
Map" picker (lives in Issue 9).

**Acceptance criteria**
- From a fresh launch, main menu shows "Map Editor"; clicking it opens
  the editor.
- Back/Esc returns to main menu.
- New string keys exist in `MainMenu.properties` (and locale variants).

**Files / entry points.** `tt/.../form/MainMenu*.java`,
`tt/src/main/resources/com/oddlabs/tt/form/MainMenu*.properties`.

**Depends on.** #4.

---

## Issue 6 — Editor: heightmap tools

**Summary.** Sculpt terrain with raise/lower/smooth/flat brushes.

**Scope**
- Brush size and strength UI.
- Tools: raise, lower, smooth, flat-to-cursor-height.
- Live preview cursor on the terrain surface.
- Undo/redo stack for heightmap edits (per-brush-stroke granularity).

**Out of scope.** Texture painting, objects, starts.

**Acceptance criteria**
- Each tool changes the heightmap visibly and persists across
  save/load.
- Undo restores prior state byte-exact.
- Brush respects map bounds (no out-of-range writes).

**Files / entry points.** `tt/.../editor/tools/HeightmapTool*.java`.

**Depends on.** #4.

---

## Issue 7 — Editor: terrain painting + object placement

**Summary.** Paint terrain layers and place world objects (trees,
palms, rocks, iron).

**Scope**
- Layer brushes for the active terrain type (NATIVE:
  sand/dirt/grass; VIKING: gravel/soil/grass/snow).
- Object placement tools: tree, palm, rock, iron — single-click and
  drag-paint with density.
- Object eraser.
- Undo/redo extended to cover painting and object edits.

**Out of scope.** Player starts, access/build grid, thumbnail,
test-play.

**Acceptance criteria**
- Painted layers persist round-trip.
- Object counts saved and reloaded match exactly.
- Objects can't be placed on water or out of bounds.

**Files / entry points.** `tt/.../editor/tools/PaintTool*.java`,
`ObjectTool*.java`.

**Depends on.** #6.

---

## Issue 8 — Editor: player starts + access/build grid + thumbnail + Test Play

**Summary.** Finalize the editor so a map is fully playable.

**Scope**
- Place 1–4 player start markers; max enforced by header `num_players`.
- "Recompute grids" derives access/build grids from heightmap +
  objects.
- "Capture thumbnail" renders a 64×64 top-down PNG and stores it in
  the file.
- "Test Play" button: save (if dirty), then launch a singleplayer
  game vs. AI using the current map.

**Out of scope.** Multiplayer hosting, Workshop publish.

**Acceptance criteria**
- Test Play boots directly into a playable game using the file you
  were just editing.
- Missing/insufficient starts blocks Test Play with a clear error.
- Thumbnail is captured at save and embedded in the `.ttmap` (visible
  in later listings).

**Files / entry points.** `tt/.../editor/StartsTool.java`,
`tt/.../editor/GridRecompute.java`,
`tt/.../editor/ThumbnailCapture.java`.

**Depends on.** #6, #7.

---

## Issue 9 — Multiplayer: lobby advertises custom maps (no transfer)

**Summary.** Host can create a multiplayer game backed by a custom
map. Joiners who already have the map (matching hash) can play.
Joiners without it get a clear error — actual transfer comes in
Issue 10.

**Scope**
- New "Custom Map" tab/path in the host-game flow that lists
  `MapStore.listAuthoredMaps()`.
- `Game` (matchmaking) gains optional `MapMeta` (hash, name,
  sizeBytes, thumbnail PNG, metersPerWorld, terrainType). Bump
  `serialVersionUID`.
- Lobby UI shows custom-map name and thumbnail when present.
- On join, client calls `MapStore.findByHash`; if missing, displays
  "You don't have this map — auto-download not yet available."
- `Game.isValid()` requires either `mapcode` or `customMap`, not
  both null.

**Out of scope.** Transfer protocol, progress bar, security
hardening.

**Acceptance criteria**
- Two clients with the same `.ttmap` can play a custom-map game
  end-to-end.
- A joiner without the file is blocked at the lobby with a clear
  message.
- Procedural (mapcode) games still work unchanged.

**Files / entry points.** `common/.../matchmaking/Game.java`,
`tt/.../form/*HostGame*`, `tt/.../form/*Lobby*`.

**Depends on.** #3.

---

## Issue 10 — Multiplayer: host→joiner transfer with progress + hash verify

**Summary.** When a joiner doesn't have the announced map, request it
from the host over the existing game socket, with a progress bar and
integrity check.

**Scope**
- Three wire messages: `MapAnnounce`, `MapRequest{offset,length}`,
  `MapChunk{offset,bytes}`. 64 KB chunks. Pull-based by the joiner.
- Hard size cap (25 MB) at the network layer.
- Magic-byte check on first chunk; abort and discard if not `"TTMAP"`.
- Final SHA-256 must match announced hash; otherwise discard and
  surface error.
- Per-joiner progress bar in the lobby. Host's "Start" disabled until
  all joiners ready.
- Multiple joiners served independently.

**Out of scope.** LRU eviction, cancel button, Workshop.

**Acceptance criteria**
- A joiner without the map sees it download and then proceeds to the
  game.
- Corrupted bytes (manually flipped in a test) cause a clean failure,
  not a crash.
- Two joiners can download from the same host concurrently.

**Files / entry points.** New messages under `tt/.../net/`, lobby UI
changes.

**Depends on.** #9.

---

## Issue 11 — Polish: cache LRU eviction + transfer cancel/timeout

**Summary.** Keep the cache directory bounded and make transfers
cancellable/timeout-safe.

**Scope**
- LRU cap (default 500 MB) on `<userdir>/maps/cache/`.
  Evict-before-write.
- Per-chunk and total-transfer timeouts. Joiner can press Cancel to
  abort and leave the lobby.
- Index file (`cache/index.json`) tracks `{hash → lastUsed}` for
  eviction.

**Out of scope.** Workshop.

**Acceptance criteria**
- Filling the cache past the cap evicts the oldest entries before
  completing the next write.
- Hanging the host mid-transfer (test by pausing the sender) trips
  the timeout and surfaces an error.
- Cancel during a transfer returns to lobby and cleans up the partial
  file.

**Depends on.** #10.

---

## Issue 12 — Workshop: load subscribed maps

**Summary.** `MapStore` discovers and lists maps the user has
subscribed to via Steam Workshop, alongside locally authored ones.

**Scope**
- Detect Workshop install path via steamworks4j
  (`ISteamUGC.GetItemInstallInfo`).
- Treat subscribed Workshop items as read-only `MapStore` entries;
  same hash-based identity as local files.
- React to subscribe/unsubscribe callbacks (rescan).
- Falls back gracefully when Steam isn't running.

**Out of scope.** Publishing, in-game browsing.

**Acceptance criteria**
- A subscribed Workshop map appears in the host's custom-map list.
- Two players each subscribed to the same Workshop map join without
  any P2P transfer (hash hit).
- A non-Steam launch continues to work — Workshop sources simply
  aren't listed.

**Files / entry points.** `tt/.../map/MapStore.java`,
`tt/.../steam/SteamManager.java`.

**Depends on.** #9. Verify steamworks4j has `ISteamUGC` bindings
first.

---

## Issue 13 — Workshop: publish from editor

**Summary.** "Publish to Workshop" button in the editor; uploads the
`.ttmap`, thumbnail, and metadata; supports updating an existing item.

**Scope**
- Publish dialog: title, description, tags, visibility, change-notes
  (for updates).
- Uses `ISteamUGC.CreateItem` / `SubmitItemUpdate`.
- Stores returned `publishedFileId` in the local `.ttmap.meta` so
  subsequent saves can update the same item.
- Error/progress UI; cancellable.

**Out of scope.** In-game browser.

**Acceptance criteria**
- A new map publishes and shows up on the user's Steam Workshop page
  with the correct thumbnail.
- Re-publishing the same file updates the existing Workshop item
  rather than creating a duplicate.
- Publishing fails clearly when Steam isn't running.

**Files / entry points.** `tt/.../editor/PublishDialog.java`,
`tt/.../steam/SteamManager.java`.

**Depends on.** #8, #12.

---

## Issue 14 — Workshop: in-game browser (optional polish)

**Summary.** Browse, subscribe, and unsubscribe to Workshop maps
without leaving the game.

**Scope**
- Query Workshop via `ISteamUGC` queries; paginated list of maps with
  title, author, rating, thumbnail.
- Subscribe/Unsubscribe buttons trigger Steam download; refreshed
  list reflects new state.
- Search and tag filters.

**Out of scope.** Comments, reporting (Steam overlay already does
these well).

**Acceptance criteria**
- Browsing returns results and shows correct thumbnails.
- Subscribing causes the map to appear in the host's custom-map list
  within a reasonable delay after download.

**Depends on.** #12.
