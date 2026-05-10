# Custom Map Hosting & Download

Design for hand-authored maps that a host can publish to a multiplayer lobby
and that joiners download on demand. Targets the `steam` branch but the
transport works for non-Steam players too.

## Goals

- Players can create maps in an in-game editor and save them locally.
- A host can pick one of their saved maps when creating a multiplayer game.
- A joiner sees the map name/size in the lobby, and the map is transferred
  to them automatically if they don't already have it.
- Joiners' downloaded maps are cached by content hash so the second join
  is instant.
- No new server-side storage; transfer is host->joiner over the existing
  game socket. Works whether or not Steam is present.

## Non-goals

- The map editor UX itself is out of scope here — this doc covers only the
  file format it must produce, plus the hosting/transfer/load path.
- No Steam Workshop integration in v1 (can layer on later).
- No server-hosted map repository.
- No backwards compatibility with the seed/mapcode flow — both paths coexist;
  custom maps are a new alternative, not a replacement.

## High-level flow

1. Player A opens the map editor, saves `Skull Island` to
   `<userdir>/maps/skull-island.ttmap`. The editor also writes a sidecar
   `skull-island.ttmap.meta` with name, author, hash, size, thumbnail bytes.
2. Player A creates a multiplayer game and selects "Custom map → Skull
   Island". The lobby `Game` payload now carries `mapHash`, `mapName`,
   `mapSize`, and a 64x64 thumbnail (a few KB), instead of a mapcode.
3. Player B sees the game in the listing with the thumbnail and "Skull
   Island (1.4 MB)". On join:
   - Look up `mapHash` in the local cache (`<userdir>/maps/cache/<hash>.ttmap`).
   - If hit → ready. If miss → request bytes from the host over the existing
     game socket. Show a progress bar in the lobby.
4. When all joiners have the file, the host hits "Start". `WorldParameters`
   now carries `mapFile` instead of (or in addition to) `mapCode`. The
   load path calls `Landscape.load(file)` instead of `new Landscape(seed,...)`.

## Map file format (`.ttmap`)

Single binary file, little-endian, versioned. Goal: contains exactly the
data the existing `Landscape` already produces, so the load path drops
straight into `WorldInfo` without re-running procedural generation.

```
Header
  magic        = "TTMAP"  (5 bytes)
  version      = u16        (start at 1)
  flags        = u32        (reserved)
  meters_per_world = u16    (256/512/1024/2048)
  terrain_type = u8         (0 NATIVE, 1 VIKING)
  num_players  = u8         (max slots authored into the map)
  seed_hint    = u32        (used only by procedural fills/plants/structure noise)

Sections (TLV: u16 type, u32 length, bytes)
  HEIGHTMAP    float[][]                 // see Landscape.getHeight()
  ACCESS_GRID  bitmap                    // boolean[][], packed 1 bit/cell
  BUILD_GRID   byte[][]                  // byte[][]
  TREES        list<{u16 x,u16 y}>
  PALMS        list<{u16 x,u16 y}>
  ROCKS        list<{u16 x,u16 y}>
  IRON         list<{u16 x,u16 y}>
  PLANTS       float[][]                 // optional; can be regenerated from seed_hint
  STARTS       list<{f32 x,f32 y}>       // player starting locations
  BLEND_INFO   array of BlendInfo records
  THUMBNAIL    PNG bytes (64x64)
  META         JSON (name, author, description, created_at)
```

Notes:
- The format is verbatim what `Landscape` already exposes via its
  `getHeight`, `getTrees`, `getStartingLocations`, etc. (see
  `tt/src/main/java/com/oddlabs/tt/procedural/Landscape.java` lines
  1161-1218). Loading is just deserialization into a `WorldInfo`.
- `BlendInfo`/textures are derived during `Landscape` construction today;
  we either persist them or rebuild them deterministically from
  `seed_hint + terrain_type` at load time. **Decision: rebuild them**
  to keep file size down. The texture-blend code in `Landscape` does not
  depend on hills/vegetation/supplies, only on terrain type and a seed.
- Plants are visual-only; persist them or regen — TBD. Recommend regen.
- File size estimate at 1024x1024 heightmap: ~4 MB raw floats; with
  zlib it'll be 1-2 MB. We zlib-compress section payloads.

### Why not just serialize `WorldInfo`?

`WorldInfo` references `Texture` (GPU-side) and `BlendInfo`/`Maps` records
that aren't trivially `Serializable`. A purpose-built format is simpler,
versionable, and avoids accidentally shipping GPU-resident data.

## Code changes

### New module: `com.oddlabs.tt.map`

- `MapFile` — record holding parsed `.ttmap` bytes.
- `MapFileReader`, `MapFileWriter` — TLV codec.
- `MapStore` — manages `<userdir>/maps/` (user-authored) and
  `<userdir>/maps/cache/<hash>.ttmap` (downloaded). Exposes:
  - `listAuthoredMaps() : List<MapMeta>`
  - `findByHash(hash) : Optional<Path>`
  - `saveAuthored(MapFile) : Path`
  - `cacheDownloaded(hash, bytes) : Path`
- `MapMeta` — `{ hash, name, sizeBytes, thumbnailPng, metersPerWorld,
  terrainType }`. Lightweight; safe to put in `Game`.

User-dir prefix follows the same convention as
`Globals.getSavegamesFileName()` so it Steam-Cloud-syncs automatically
for authored maps (not the cache — cache is per-machine).

### `common/src/main/java/com/oddlabs/matchmaking/Game.java`

Add a new constructor / field set for custom-map games:

```java
private final @Nullable MapMeta customMap;  // null for procedural games
```

Bump `serialVersionUID` to 4. Keep the existing mapcode constructor for
procedural games. `Game.isValid()` rejects games that have neither a
mapcode nor a customMap.

This means a `MapMeta` (including the small thumbnail PNG) travels through
the matchmaker server. The thumbnail is bounded at 64x64 PNG (~3 KB);
the rest is small. No actual map bytes go through the matchmaker.

### `tt/src/main/java/com/oddlabs/tt/landscape/WorldParameters.java`

Add `@Nullable Path customMapFile`. Constructor variants:

```java
public WorldParameters(int speed, MapMeta meta, Path file, int initialUnits, int maxUnits)
public WorldParameters(int speed, String mapcode, int initialUnits, int maxUnits, int mapSize) // existing
```

`getCustomMapFile()` returns the path; null = use mapcode procedural path.

### `tt/src/main/java/com/oddlabs/tt/resource/WorldGenerator.java`

New implementor `CustomMapWorldGenerator` that returns a `WorldInfo`
constructed from a parsed `.ttmap` instead of running `Landscape`. It
still rebuilds the blend textures (terrain-type only, deterministic).

The existing seed-based generator stays for procedural games.

### `tt/src/main/java/com/oddlabs/tt/net/` — host->joiner transfer

A new pre-game step before `WorldStarter.load`. Today, the host calls
`sendWorldParams` only at start time (see `WorldStarter.java` line 83).
We add an earlier step: when a joiner connects, the host sends the
`MapMeta`; the joiner either acks "have it" or requests the bytes.

Three new wire messages on the existing game socket
(`Network.getMatchmakingClient()` peer connection):

- `MapAnnounce { hash, sizeBytes, name }` — host → joiner on join.
- `MapRequest { hash, offset, length }` — joiner → host. Joiner pulls
  chunks (64 KB) so it can show progress; host streams them out.
- `MapChunk { hash, offset, bytes }` — host → joiner.

Pull-based (joiner asks) is simpler than push-based; the joiner controls
pacing and the host doesn't need to track per-joiner send buffers.

Multiple joiners → host serves each independently. Bandwidth cap is a
config knob (default e.g. 1 MB/s/joiner) to avoid starving game traffic
in a future where the lobby and the running game share a connection.

Lobby UI: a per-joiner progress bar. "Start" disabled until all joiners
report ready.

### Map editor (sketch only — separate work)

The editor produces `.ttmap` files. It needs:

- Heightmap painter (raise/lower/smooth brushes, plus flat-fill).
- Texture/blend painter (sand/dirt/grass/snow tools per terrain type).
- Object placement (trees, palms, rocks, iron — same lists `Landscape`
  produces).
- Player start markers (1-4 slots).
- "Recompute access grid" — derives access/build grids from heightmap.
- Test-launch ("play this map vs. AI").
- Save / load / thumbnail capture.

Editor lives under `tt/src/main/java/com/oddlabs/tt/editor/` and reuses
the in-engine renderer with a different input controller. Out of scope
for the hosting/download work; this design only assumes its output
format.

## Cache & disk layout

```
<userdir>/maps/                              # authored, Steam-Cloud-synced
  skull-island.ttmap
  skull-island.ttmap.meta                    # JSON, just for editor browsing
<userdir>/maps/cache/                        # downloaded; not synced
  3f2a...e8.ttmap                            # filename = sha256 hash
  index.json                                 # hash → {name, lastUsed}
```

LRU eviction on `cache/` when total exceeds a cap (default 500 MB).

## Integrity & safety

- Joiner verifies the SHA-256 of received bytes matches the announced
  `mapHash` before loading. Mismatch → discard and re-request once,
  then fail the join.
- Hard cap on map file size (e.g. 25 MB) enforced by the host when
  authoring and by joiners when accepting transfers.
- Parser is bounds-checked; reject truncated/oversized sections.
- The TLV parser refuses unknown REQUIRED section types but tolerates
  unknown OPTIONAL ones (top bit of type id signals optional).

## Open questions

1. Should `BlendInfo` / textures be persisted in the file or rebuilt at
   load? Doc proposes rebuild for size; needs confirmation that nothing
   in the blend pipeline depends on per-cell terrain-painting state we
   haven't accounted for.
2. Should authored maps Steam-Cloud-sync (per `Globals.steamPrefixed`)?
   Probably yes, but the cache dir explicitly should not.
3. Steam P2P: skip for v1 (works on game socket). Worth revisiting if
   throughput on the existing socket is poor.
4. Versioning: how to handle an old client joining a host that has a
   newer `.ttmap` version? Proposal: reject join with a clear message
   ("requires client update").
5. Cheating: a malicious host could send a map whose access grid lies
   about walkability vs. heightmap. Out of scope; same trust level as
   today (host already controls RNG seed).

## Rollout

1. Land file format + `MapStore` + parser/writer + tests (no UI yet).
2. Land `CustomMapWorldGenerator` and `WorldParameters.customMapFile`
   plumbing; verify a hand-crafted test `.ttmap` loads into a playable
   world (skip editor, write the file from a unit test).
3. Land transfer protocol + lobby UI for custom-map games.
4. Land editor in a separate track once 1-3 are stable.

Order matters: stages 1-3 can ship and be tested with synthetic
`.ttmap` files before the editor exists.
