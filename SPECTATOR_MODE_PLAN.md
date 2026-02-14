# Spectator Mode Implementation Plan

## Overview

Add the ability for a player to spectate a game that has already started. The spectator
joins mid-game, receives the world seed and recorded event history, fast-forwards the
simulation to catch up to the current tick, and then watches the game live in a read-only
observer view.

**Approach:** Event replay with fast-forward catch-up (not state snapshots). The game
already uses deterministic lockstep simulation, so replaying the same events at the same
ticks produces the exact same game state. This is simpler, safer, and leverages existing
infrastructure.

**Estimated catch-up time:** 6-45 seconds for a typical 10-30 minute game (running the
simulation at ~2,000-5,000 ticks/second without rendering).

---

## Existing Infrastructure We Build On

| Component | File | What it gives us |
|-----------|------|-----------------|
| Observer mode UI/input | `tt/delegate/SelectionDelegate.java:68-72` | Disables selection, commands, building; keeps camera and map mode working |
| Deterministic event logging | `common/event/SaveDeterministic.java` | Delta-compressed recording of non-deterministic values |
| Deterministic event playback | `common/event/LoadDeterministic.java` | Replay from recorded file, supports gzip |
| Spectator info protocol | `common/matchmaking/MatchmakingServerInterface.java:72` | `updateSpectatorInfo()` already defined |
| Server spectator file writing | `server/matchserver/TimestampedGameSession.java:53-55` | Writes game events to `/var/games/{id}` |
| World generation from seed | `tt/resource/WorldGenerator.java` | Deterministic world from seed — no terrain serialization needed |
| InGameInfo abstraction | `tt/viewer/InGameInfo.java` | Clean place to add `SpectatorInGameInfo` |
| Player slot types | `tt/net/PlayerSlot.java:22-25` | `OPEN`, `CLOSED`, `HUMAN`, `AI` — add `SPECTATOR` |

---

## Phase 1: Spectator Player Slot & Pre-Game Join

**Goal:** A player can join a game lobby as a spectator before the game starts, and watch
from tick 0 in observer mode.

### 1.1 Add SPECTATOR slot type

**File:** `tt/classes/com/oddlabs/tt/net/PlayerSlot.java`

- Add `public static final int SPECTATOR = 5;` alongside the existing slot types (line 25)
- Update `isValidType()` (line 41) to accept `SPECTATOR`
- Add `getPlayerType()` mapping for `SPECTATOR` — return a new `PlayerTypes.Spectator` enum value

**File:** `common/classes/com/oddlabs/matchmaking/PlayerTypes.java`

- Add `Spectator` enum value

### 1.2 Server accepts spectator connections

**File:** `tt/classes/com/oddlabs/tt/net/Server.java`

- Modify `incomingConnection()` (line 264): Add a separate path for spectator connections.
  Spectators should NOT consume a regular player slot. Instead, track them in a separate
  `List<ClientConnection> spectator_connections` field.
- Modify `startServer()` (line 172): Do NOT wait for spectators to be "ready." Only count
  HUMAN players when checking `getNumReady() == getNumClients()`.
- Modify `broadcastInits()` (line 246): Also send `startGame()` to spectator connections.
- Modify `broadcastPlayers()` (line 229): Include spectator connection clients so they see
  the player list updates.
- Add `locateAvailableSpectatorSlot()`: Spectators don't take player slots. Instead, assign
  them a slot index >= MAX_PLAYERS (or -1) so they never map to a game `Player`.

### 1.3 Client spectator connection flow

**File:** `tt/classes/com/oddlabs/tt/net/Client.java`

- Add a `boolean spectator` field, set during construction.
- `startGame()` (line 133): When `spectator == true`, pass a flag through to `WorldStarter`
  so it knows to create a spectator viewer.

### 1.4 WorldStarter creates spectator WorldViewer

**File:** `tt/classes/com/oddlabs/tt/net/WorldStarter.java`

- In `load()` (line 54): If the joining player is a spectator (`player_slot == -1` or a
  spectator flag is set):
  - Set `corrected_player_slot` to 0 (spectator views from first player's perspective for
    world construction, but doesn't control them).
  - Pass a `SpectatorInGameInfo` instead of the normal `ingame_info`.
  - After creating the `WorldViewer`, immediately call
    `viewer.getDelegate().setObserverMode()` to enter observer mode from tick 0.

### 1.5 SpectatorInGameInfo

**File (new):** `tt/classes/com/oddlabs/tt/viewer/SpectatorInGameInfo.java`

Create a new `InGameInfo` implementation:

```java
public final strictfp class SpectatorInGameInfo implements InGameInfo {
    // isMultiplayer() -> true (needs network sync)
    // isRated() -> false (spectators don't affect ratings)
    // addGUI() -> add only spectator-relevant controls (no unit/building panels)
    // addGameOverGUI() -> show stats + "Main Menu" button (no "Observer Mode" button
    //                     since already observing; no "Replay" button)
    // abort() -> close viewer and return to menu
    // close() -> clean up resources
    // getRandomStartPosition() -> delegate to the game's setting
}
```

### 1.6 PeerHub handles spectator peers

**File:** `tt/classes/com/oddlabs/tt/net/PeerHub.java`

- In the constructor (line 88): When building the peer list, skip `SPECTATOR` slots.
  Spectators should NOT be added to `peer_index_to_peer`, `player_to_peer`, or
  `peer_to_player`. They are passive recipients.
- The spectator's local `PeerHub` will receive events via the Router but will never send
  game state events (no `PlayerInterface` commands).
- `sendChecksum()` (line 360): Spectator's PeerHub should still compute checksums locally
  for its own consistency but should NOT send them to the Router. If a spectator desyncs,
  it should not affect the active game. Add an `is_spectator` flag to control this.
- `doTick()` (line 321): Skip `sendStatusUpdate()`, `sendMap()`, `sendInitInfo()`,
  `sendTrees()`, `sendSpectatorInfo()` calls when `is_spectator == true` — only the host
  should send these.

### 1.7 Lobby UI for spectator join

**File:** `tt/classes/com/oddlabs/tt/form/SelectGameMenu.java`

- Add a "Spectate" button alongside the "Join" button (or make it a mode toggle).
- When clicked, connect to the game host with a flag indicating spectator intent.

**File:** `tt/classes/com/oddlabs/tt/net/GameServerInterface.java`

- Add a `joinAsSpectator()` method (or add a boolean parameter to the connection protocol)
  so the server knows the connecting client is a spectator.

### 1.8 Prevent spectator commands at the network level

**File:** `tt/classes/com/oddlabs/tt/net/PeerHub.java`

- When `is_spectator == true`, the `player_interface` proxy should be a no-op
  implementation. Override `getPlayerInterface()` to return a stub that drops all commands.
  This is defense-in-depth beyond the UI-level observer mode guard.

---

## Phase 2: Event Recording for Mid-Game Join

**Goal:** The game host records all player command events during gameplay so a spectator
joining mid-game can replay them to catch up.

### 2.1 Record game events on the host

**File:** `tt/classes/com/oddlabs/tt/net/PeerHub.java`

- Add an `EventLog` (new class) field to the host's `PeerHub`.
- In `receiveGameStateEvent()` (line 222): After validating the event, append it to the
  `EventLog` with its tick number and client ID.
- In `doTick()` (line 321): Also log any locally-generated events (AI commands already
  run through the deterministic system, so they replay automatically — but we need the
  human peer events logged).

**File (new):** `tt/classes/com/oddlabs/tt/net/EventLog.java`

```java
public final strictfp class EventLog {
    private final List<LoggedEvent> events = new ArrayList<>();

    public void record(int tick, int client_id, ARMIEvent event) {
        events.add(new LoggedEvent(tick, client_id, event));
    }

    public List<LoggedEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public byte[] serialize() {
        // Serialize to byte array for network transfer
    }
}
```

### 2.2 Store world generation parameters

The host already has the `WorldGenerator` and `PlayerSlot[]` from game setup. These must
be available to send to late-joining spectators. Store references in the `PeerHub` or a
new `GameSession` object on the host side.

**Data needed for catch-up:**
- `WorldGenerator` (contains seed, terrain type — everything needed to reconstruct terrain)
- `WorldParameters` (initial unit count, game speed, etc.)
- `PlayerSlot[]` (player names, teams, races, AI difficulties)
- `UnitInfo[]` (starting units per player)
- `int current_tick` (how far the game has progressed)
- `EventLog` (all player command events from tick 0 to current_tick)

---

## Phase 3: Mid-Game Spectator Join

**Goal:** A spectator can connect to a game that is already running, receive catch-up
data, fast-forward to the current tick, and begin watching live.

### 3.1 Server accepts connections during SYNCHRONIZING/RUNNING state

**File:** `tt/classes/com/oddlabs/tt/net/Server.java`

- Modify `incomingConnection()` (line 264): Currently rejects connections if
  `state != NEGOTIATING`. Add a branch: if the connection is a spectator request AND
  `state != CLOSED`, accept it.
- For mid-game spectator connections, send a different init message than `startGame()`.
  Define a new client interface method: `startSpectating(game_params, event_log_data, current_tick)`.

**File:** `tt/classes/com/oddlabs/tt/net/GameClientInterface.java`

- Add: `void startSpectating(Game game, WorldGenerator generator, PlayerSlot[] players,
  UnitInfo[] unit_infos, byte[] event_log, int current_tick);`

### 3.2 Client handles spectator start

**File:** `tt/classes/com/oddlabs/tt/net/Client.java`

- Implement `startSpectating()`: Store the received data and invoke a
  `SpectatorWorldStarter` instead of `WorldStarter`.

### 3.3 SpectatorWorldStarter with fast-forward

**File (new):** `tt/classes/com/oddlabs/tt/net/SpectatorWorldStarter.java`

This is the core of mid-game spectating:

```
1. Construct the World from the WorldGenerator seed (same as normal game start)
2. Create all Players from PlayerSlot[] and UnitInfo[] (same as normal game start)
3. Deserialize the EventLog from byte[]
4. Create a headless PeerHub (no Router connection yet)
5. Fast-forward loop:
   for tick = 0 to current_tick:
       execute all events queued at this tick
       world.tick(1.0f)
   (No rendering, no network, no input polling, no checksum sending)
6. Show progress bar during fast-forward ("Catching up: tick 45000/90000")
7. Once caught up, connect to the Router as a read-only peer
8. Switch to normal spectator mode (live event receiving + rendering)
```

**Key consideration:** During fast-forward, skip all of these (they only matter for
live play):
- `network.tick()` — no network during catch-up
- `PointerInput.poll()` / `KeyboardInput.poll()` — no input during catch-up
- `sendChecksum()` — no sync verification during catch-up
- `sendStatusUpdate()` — not relevant
- `sendSpectatorInfo()` — spectators don't send this
- All rendering — this is what makes catch-up fast

**Progress UI:** Use `ProgressForm` (already used by `WorldStarter` for loading) to show
catch-up progress. Update it every 1000 ticks.

### 3.4 Transition from catch-up to live

After fast-forward completes:

1. Connect the spectator's `PeerHub` to the Router with the current `SessionID`
2. The Router begins forwarding live game events to the spectator
3. Events arrive with tick timestamps. The spectator's simulation is now at `current_tick`.
   Events for ticks > `current_tick` are queued normally.
4. The normal `PeerHub.animate()` loop takes over — the spectator ticks forward in sync
   with the game.

**Gap handling:** Between the time the host serialized the event log and the spectator
finishes catching up, new events may have occurred. The host should continue appending to
the event log while the spectator catches up. Two options:
- **Option A (simpler):** Buffer all events received from the Router during catch-up, then
  process them after fast-forward completes.
- **Option B:** Have the host send a "delta" event log covering the gap.

Option A is recommended — the Router already queues events, and we just need to defer
processing until catch-up is done.

### 3.5 Router integration

**File:** `tt/classes/com/oddlabs/tt/net/RouterClient.java`

The spectator needs to connect to the Router to receive live events. The Router must be
told that this new client is a spectator (read-only, no checksum participation).

**File:** `common/router/SessionInfo.java`

- May need to support a larger participant count or a separate spectator count so the
  Router knows how many non-spectator peers to expect for checksums.

**Alternative approach:** If modifying the Router is too invasive, the host can relay
events to spectators directly (host acts as a proxy). This is simpler but adds latency
and load to the host.

---

## Phase 4: Polish & Edge Cases

### 4.1 Spectator disconnect handling

**File:** `tt/classes/com/oddlabs/tt/net/PeerHub.java`

- When a spectator disconnects, no gameplay impact. Don't trigger "player left" messages
  for spectators. Add a `if (!is_spectator)` guard in `peerDisconnected()` (line 510).
- Optionally show a system chat message: "Spectator X stopped watching."

### 4.2 Spectator chat

Spectators should be able to read game chat but have restricted sending:
- **Option A:** Spectators can only chat with other spectators (separate channel).
- **Option B:** Spectators can chat with everyone but messages are tagged "[Spectator]".
- **Option C:** Spectators are read-only for chat too.

Recommend Option B for the initial implementation. Modify `PeerHub.sendChat()` (line 528)
to tag spectator messages and broadcast them.

### 4.3 Game-over for spectators

When the game ends (all players on one side eliminated), spectators should see the
`GameStatsDelegate` with stats for all players and a "Main Menu" button. Since they're
already in observer mode, no "Observer Mode" button is needed.

**File:** `tt/classes/com/oddlabs/tt/viewer/SpectatorInGameInfo.java`

- `addGameOverGUI()`: Show stats + "Main Menu" button only.

### 4.4 Matchmaking server: list spectatable games

**File:** `common/classes/com/oddlabs/matchmaking/MatchmakingServerInterface.java`

- Add `public static final int TYPE_SPECTATABLE_GAME = 3;` (alongside existing TYPE_GAME,
  TYPE_CHAT_ROOM, etc.)
- `requestList(TYPE_SPECTATABLE_GAME, ...)` returns in-progress games that allow spectating.

**File:** `server/classes/com/oddlabs/matchserver/MatchmakingServer.java`

- Track active game sessions (already done via `TimestampedGameSession`).
- Respond to `TYPE_SPECTATABLE_GAME` list requests with games currently in progress.

**File:** `common/classes/com/oddlabs/matchmaking/Game.java`

- Add `boolean allow_spectators` field (host can opt-in/out).

### 4.5 Maximum spectator count

Add a configurable maximum (e.g., 8 spectators per game) to prevent overloading the host
or Router. Track count in `Server.java`.

### 4.6 Rated game restrictions

**File:** `tt/classes/com/oddlabs/tt/net/Server.java`

- Spectators should be allowed in rated games (they don't affect gameplay).
- But consider: in rated games, spectators could feed information to players via external
  communication. Add an option for rated games to disable spectating, or add a delay
  (e.g., spectators see events 30 seconds behind live).

---

## File Change Summary

### New Files

| File | Purpose |
|------|---------|
| `tt/classes/com/oddlabs/tt/viewer/SpectatorInGameInfo.java` | InGameInfo implementation for spectator mode |
| `tt/classes/com/oddlabs/tt/net/EventLog.java` | Records and serializes game events for catch-up replay |
| `tt/classes/com/oddlabs/tt/net/SpectatorWorldStarter.java` | Fast-forward world construction for mid-game join |

### Modified Files

| File | Changes |
|------|---------|
| `tt/net/PlayerSlot.java` | Add `SPECTATOR = 5` type constant |
| `common/matchmaking/PlayerTypes.java` | Add `Spectator` enum value |
| `tt/net/Server.java` | Accept spectator connections; track separately from players; allow mid-game join |
| `tt/net/Client.java` | Add `spectator` flag; handle `startSpectating()` |
| `tt/net/GameClientInterface.java` | Add `startSpectating()` method |
| `tt/net/GameServerInterface.java` | Add spectator join method |
| `tt/net/WorldStarter.java` | Support spectator viewer creation with observer mode from tick 0 |
| `tt/net/PeerHub.java` | Add `is_spectator` flag; skip checksum sending; no-op player interface; event logging on host |
| `tt/net/Peer.java` | No changes needed (events execute the same way) |
| `tt/delegate/SelectionDelegate.java` | No changes needed (observer mode already works) |
| `tt/viewer/WorldViewer.java` | Minor: accept spectator flag for PeerHub construction |
| `common/matchmaking/MatchmakingServerInterface.java` | Add `TYPE_SPECTATABLE_GAME` |
| `common/matchmaking/Game.java` | Add `allow_spectators` field |
| `server/matchserver/MatchmakingServer.java` | Serve spectatable game list |
| `tt/form/SelectGameMenu.java` | Add "Spectate" button/tab for in-progress games |

---

## Implementation Order

```
Phase 1 (Pre-game spectating — foundation)
  1.1  PlayerSlot.SPECTATOR type + PlayerTypes.Spectator
  1.2  Server.java spectator connection handling
  1.3  Client.java spectator flag
  1.4  WorldStarter spectator path
  1.5  SpectatorInGameInfo
  1.6  PeerHub spectator peer handling
  1.7  Lobby UI (spectate button)
  1.8  No-op PlayerInterface for spectators

Phase 2 (Event recording — prerequisite for mid-game)
  2.1  EventLog class + host-side recording in PeerHub
  2.2  Store world generation params for late joiners

Phase 3 (Mid-game join — the full feature)
  3.1  Server accepts mid-game spectator connections
  3.2  Client handles startSpectating()
  3.3  SpectatorWorldStarter with fast-forward
  3.4  Catch-up to live transition
  3.5  Router integration for spectator peers

Phase 4 (Polish)
  4.1  Spectator disconnect handling
  4.2  Spectator chat
  4.3  Game-over for spectators
  4.4  Matchmaking server game listing
  4.5  Max spectator count
  4.6  Rated game considerations
```

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Deterministic replay diverges from live game | Spectator sees wrong state | Use existing `strictfp` math guarantees; don't participate in checksums so divergence doesn't crash the game |
| Event log grows unboundedly for long games | Memory pressure on host | Cap at ~5MB (sufficient for 4+ hours); compress with gzip |
| Router doesn't support dynamic participant changes | Can't add spectators mid-game | Use host-relay approach as fallback (host forwards events to spectators over separate connections) |
| Fast-forward is slower than expected | Long wait for spectators | Show progress bar; profile and optimize tight loop; consider skipping non-essential per-tick work during catch-up |
| Spectators leak game information in rated games | Competitive integrity | Add optional spectator delay (30-60 second buffer); allow hosts to disable spectating for rated games |
| Network bandwidth for sending event log | Slow spectator connect | Event logs are small (~1MB for 30-min game); compress before sending |
