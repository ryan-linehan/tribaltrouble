# Spectator Mode & Rejoin Implementation Plan

## Overview

Add the ability for a player to spectate a game that has already started, and for a
disconnected player to rejoin an in-progress game. Both features share the same core
infrastructure: event replay with fast-forward catch-up.

The spectator joins mid-game, receives the world seed and recorded event history,
fast-forwards the simulation to catch up to the current tick, and then watches the game
live in a read-only observer view. A rejoining player does the same fast-forward, but
then resumes control of their original Player object instead of entering observer mode.

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
| Player slot types | `tt/net/PlayerSlot.java:22-25` | `OPEN`, `CLOSED`, `HUMAN`, `AI` — unchanged; spectators are outside the slot system |

---

## Phase 1: Spectator Connection & Pre-Game Join

**Goal:** A player can join a game lobby as a spectator before the game starts, and watch
from tick 0 in observer mode.

**Design decision:** Spectators are **completely outside the player slot system**. They
don't get a `PlayerSlot`, don't get a `Player` object in the simulation, and don't appear
in the lobby's player slot UI. The `Server` tracks them in a separate connection list.
Each spectator constructs their own independent `WorldViewer` locally (with its own
`Camera`, `SelectionDelegate`, etc.), giving every spectator fully independent camera
controls.

### 1.1 Server tracks spectator connections separately

**File:** `tt/classes/com/oddlabs/tt/net/Server.java`

- Add a `List<ClientConnection> spectator_connections` field, separate from the existing
  `connection_to_client` map (which holds game participants).
- Modify `incomingConnection()` (line 264): When a connection arrives with a spectator
  flag, add it to `spectator_connections` instead of assigning a `PlayerSlot`. Do not call
  `locateAvailableSlot()` — spectators don't consume slots.
- Modify `startServer()` (line 172): Do NOT wait for spectators to be "ready." Only count
  HUMAN players in `connection_to_client` when checking `getNumReady() == getNumClients()`.
- Modify `broadcastInits()` (line 246): Also send `startGame()` to spectator connections.
- Modify `broadcastPlayers()` (line 229): Include spectator connections so they see
  the player list updates (for display purposes, not slot assignment).

No changes to `PlayerSlot.java` or `PlayerTypes.java` — spectators don't need a slot type.

### 1.2 Client spectator connection flow

**File:** `tt/classes/com/oddlabs/tt/net/Client.java`

- Add a `boolean spectator` field, set during construction.
- `startGame()` (line 133): When `spectator == true`, pass a flag through to `WorldStarter`
  so it knows to create a spectator viewer.

### 1.3 WorldStarter creates spectator WorldViewer

**File:** `tt/classes/com/oddlabs/tt/net/WorldStarter.java`

- In `load()` (line 54): If the joining client is a spectator:
  - Construct the `World` from the seed and create all `Player` objects as normal (the
    spectator needs the full simulation to replay events).
  - Set `local_player` to `players[0]` — this is only used for camera starting position
    and rendering context, not for control.
  - Pass a `SpectatorInGameInfo` instead of the normal `ingame_info`.
  - After creating the `WorldViewer`, immediately call
    `viewer.getDelegate().setObserverMode()` to enter observer mode from tick 0.
  - Each spectator gets their own `WorldViewer` instance with its own `Camera` and
    `SelectionDelegate`, so camera controls are fully independent per spectator.

### 1.4 SpectatorInGameInfo

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

### 1.5 PeerHub spectator mode

**File:** `tt/classes/com/oddlabs/tt/net/PeerHub.java`

The spectator's `WorldViewer` creates a `PeerHub` as normal, but with an `is_spectator`
flag that changes its behavior:

- The spectator's `PeerHub` receives events via the Router and executes them locally to
  keep the simulation in sync. But the spectator has no `Player` it controls — it doesn't
  appear in `peer_index_to_peer`, `player_to_peer`, or `peer_to_player`.
- `sendChecksum()` (line 360): Compute checksums locally for its own consistency but do
  NOT send them to the Router. A spectator desync should not affect the active game.
- `doTick()` (line 321): Skip `sendStatusUpdate()`, `sendMap()`, `sendInitInfo()`,
  `sendTrees()`, `sendSpectatorInfo()` calls when `is_spectator == true` — only the host
  should send these.
- `getPlayerInterface()`: Return a no-op stub that drops all commands. This is
  defense-in-depth beyond the UI-level observer mode guard.

### 1.6 Lobby UI for spectator join

**File:** `tt/classes/com/oddlabs/tt/form/SelectGameMenu.java`

- Add a "Spectate" button alongside the "Join" button (or make it a mode toggle).
- When clicked, connect to the game host with a flag indicating spectator intent.

**File:** `tt/classes/com/oddlabs/tt/net/GameServerInterface.java`

- Add a `joinAsSpectator()` method (or add a boolean parameter to the connection protocol)
  so the server knows the connecting client is a spectator.

---

## Phase 2: Event Recording for Mid-Game Join (Spectators & Rejoin)

**Goal:** The game host records all player command events during gameplay so a spectator
or rejoining player can replay them to catch up.

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

### 3.3 ReplayWorldStarter with fast-forward

**File (new):** `tt/classes/com/oddlabs/tt/net/ReplayWorldStarter.java`

This is the core of mid-game spectating (and is reused for rejoin in Phase 5).
A `boolean rejoin` flag controls whether the result is observer mode or active play.

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
7. Once caught up:
   - If spectator: connect to the Router as a read-only peer, enter observer mode
   - If rejoin: connect to the Router at the original peer_index, resume full control
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

## Phase 5: Player Rejoin

**Goal:** A player who disconnects (network drop, crash, alt-F4) can reconnect to the
same game, fast-forward to catch up, and resume playing with their units, buildings, and
resources intact.

**Prerequisite:** Phases 2 and 3 provide all the heavy infrastructure (event recording,
mid-game connection acceptance, fast-forward replay). Phase 5 adds the logic to
re-seat a player into their original slot instead of entering observer mode.

### 5.1 Graceful disconnect: always temporary

Currently, when a player disconnects, `PeerHub.peerDisconnected()` (line 510) permanently
removes them — their peer is nulled from `peer_index_to_peer`, removed from the
player/peer maps, and the matchmaking server is notified (`gameQuitNotify`). Their units
and buildings remain in the `World` but become headless (no one controls them), and
`isAlive()` (line 282) immediately returns false because `locatePeerFromPlayer()` returns
null.

**Design decision:** A disconnected player can **always** rejoin, regardless of how long
they've been gone. There is no grace period or timeout. The game continues normally while
they are away — their units idle, buildings keep producing, and other players can attack
them freely. When the disconnected player reconnects (whether 30 seconds or 30 minutes
later), they rejoin and resume control of whatever is left.

**File:** `tt/classes/com/oddlabs/tt/net/PeerHub.java`

- Add a `Set<Player> temporarily_disconnected` field.
- Modify `peerDisconnected()` (line 510): Instead of unconditionally removing the peer
  from the maps and broadcasting "left game":
  - Add the `Player` to `temporarily_disconnected`. Null out the peer in
    `peer_index_to_peer` (so events aren't executed for them), but preserve the peer
    index mapping so we know which slot to re-seat them into. Broadcast a system chat
    message: "PlayerName disconnected."
  - Do NOT notify matchmaking server with `gameQuitNotify` — the player is still in-game.
- Modify `isAlive()` (line 282): A temporarily disconnected player should still be
  considered alive (their units shouldn't be auto-eliminated):
  ```java
  return (nonhuman_players.contains(player)
          || locatePeerFromPlayer(player) != null
          || temporarily_disconnected.contains(player))
         && player.isAlive();
  ```

### 5.2 Server accepts rejoin connections

**File:** `tt/classes/com/oddlabs/tt/net/Server.java`

- Modify `incomingConnection()` (line 264): When a connection arrives during a running
  game, check identity against disconnected players:
  - Match by `TunnelIdentifier.getProfile()` — compare the connecting player's profile
    nick and host ID against the original `PlayerSlot` entries.
  - If the player is found in the disconnected list, this is a rejoin. Send them the
    game params + event log + their original slot index.
  - If not found, treat as a spectator request (Phase 3 behavior).
- Track which player slots have disconnected players in a
  `Map<Integer, PlayerIdentity>` (slot index -> player identity).

**File:** `tt/classes/com/oddlabs/tt/net/GameClientInterface.java`

- Add: `void startRejoin(Game game, WorldGenerator generator, PlayerSlot[] players,
  UnitInfo[] unit_infos, byte[] event_log, int current_tick, short original_player_slot);`

### 5.3 Client handles rejoin

**File:** `tt/classes/com/oddlabs/tt/net/Client.java`

- Implement `startRejoin()`: Same as `startSpectating()` but passes the original player
  slot index and sets `rejoin = true` so the `ReplayWorldStarter` knows to resume control
  instead of entering observer mode.

### 5.4 ReplayWorldStarter rejoin path

**File:** `tt/classes/com/oddlabs/tt/net/ReplayWorldStarter.java` (created in Phase 3.3)

After fast-forward completes, the rejoin path differs from spectator:

```
Spectator path (existing):
  -> setObserverMode()
  -> connect to Router as read-only
  -> no-op PlayerInterface

Rejoin path (new):
  -> set corrected_player_slot = original_player_slot (not 0)
  -> connect to Router at original peer_index as a full participant
  -> active PlayerInterface (can send commands)
  -> participate in checksums
  -> re-register in PeerHub's peer_to_player / player_to_peer maps
  -> remove from temporarily_disconnected set
  -> broadcast system chat: "PlayerName has reconnected"
```

Key difference: the rejoining player's `local_player` is set to
`world.getPlayers()[original_player_slot]` — the same Player object that owned their
units and buildings. Since the fast-forward replayed all events identically, this Player
has the exact same state as on every other peer's simulation.

### 5.5 PeerHub re-registration

**File:** `tt/classes/com/oddlabs/tt/net/PeerHub.java`

- Add `rejoinPeer(int peer_index, Player player)` method:
  - Create a new `Peer` object for this player at the original `peer_index`.
  - Re-insert into `peer_index_to_peer[peer_index]`.
  - Re-add to `player_to_peer` and `peer_to_player` maps.
  - Remove from `temporarily_disconnected`.
  - Broadcast system chat: "PlayerName has reconnected."
- The rejoining player's `PeerHub` on their client side is constructed fresh by
  `ReplayWorldStarter`, with `is_spectator = false` and full checksum/command
  participation.

### 5.6 Router session re-entry

**File:** `common/classes/com/oddlabs/router/Session.java`

The Router's `Session` tracks a `Set players` and uses `num_participants` for checksum
coordination. When a player disconnects, `removePlayer()` (line 41) removes them from
the set. For rejoin, we need to add them back.

- Modify `Session.addPlayer()` (line 100): Currently calls `start()` when
  `num_participants == players.size()`. For a rejoin, the session is already started
  (`isComplete() == true`). Add a branch: if the session is already complete, accept the
  new player without re-triggering `start()`. Send them a `start()` signal immediately
  so their `PeerHub` begins ticking.
- The rejoining `RouterClient` must connect with the **same `client_id`** they originally
  had, so events route correctly. Pass the original `client_id` during Router connection.

**File:** `common/classes/com/oddlabs/router/RouterClient.java`

- Modify `close()` (line 161): Always broadcast `playerDisconnected` immediately so all
  peers know to move the player to `temporarily_disconnected`. Add a corresponding
  `playerReconnected(int client_id)` message to `RouterClientInterface` that peers handle
  by re-activating the peer slot when the player rejoins.

**File:** `common/classes/com/oddlabs/router/RouterClientInterface.java`

- Add: `void playerReconnected(int client_id);`

### 5.7 PeerHub handles reconnection notification

**File:** `tt/classes/com/oddlabs/tt/net/PeerHub.java`

- Implement `playerReconnected(int client_id)` (new method in `RouterHandler`):
  - Re-activate the peer at `peer_index_to_peer[client_id]`.
  - Move the player from `temporarily_disconnected` back to the active peer maps.
  - Broadcast system chat: "PlayerName has reconnected."

### 5.8 Matchmaking server: disconnect vs quit

**File:** `server/classes/com/oddlabs/matchserver/TimestampedGameSession.java`

Currently has participant states: `PARTICIPANT_UNKNOWN`, `PARTICIPANT_JOINED`,
`PARTICIPANT_FREE_QUIT`, `PARTICIPANT_QUIT`, `PARTICIPANT_LOST`, `PARTICIPANT_WON`.

- Add `PARTICIPANT_DISCONNECTED` state.
- Modify `gameQuit()` (line 248): Don't transition from `PARTICIPANT_JOINED` to
  `PARTICIPANT_QUIT` immediately on disconnect. Instead, transition to
  `PARTICIPANT_DISCONNECTED`. The player remains in this state until they rejoin or the
  game ends.
- Add `participantRejoined()`: Transition from `PARTICIPANT_DISCONNECTED` back to
  `PARTICIPANT_JOINED`.
- When the game ends, any player still in `PARTICIPANT_DISCONNECTED` transitions to
  `PARTICIPANT_QUIT` (unrated) or `PARTICIPANT_LOST` (rated).

**File:** `common/classes/com/oddlabs/matchmaking/MatchmakingServerInterface.java`

- Add: `void playerDisconnectedNotify(String nick);`
- Add: `void playerReconnectedNotify(String nick);`

### 5.9 Headless units during disconnect

While a player is temporarily disconnected, their units and buildings exist but receive
no commands. This is actually fine for the game's design:

- **Peons** that were harvesting continue their current task (behavior controllers persist
  in the simulation).
- **Warriors** that were moving/attacking continue their current action.
- **Buildings** that were producing continue producing.
- No NEW commands are issued, so idle units stay idle.

**Optional enhancement (deferred):** Have a basic AI take over for the disconnected
player. This would use the existing `PassiveAI` or `AdvancedAI` classes:
- On disconnect: `player.setAI(new AdvancedAI(player, unit_info, AdvancedAI.DIFFICULTY_EASY))`
- On rejoin: `player.setAI(null)` to return to human control.

This is a nice-to-have but adds complexity (the AI's commands need to be recorded in the
event log and replayed by the rejoining player). Recommend deferring to a future
iteration.

### 5.10 Rejoin UI

**Client-side reconnect flow:**

When a player is disconnected (network error, `PeerHub.routerFailed()` at line 188, or
`closeNetwork()` at line 559):

- Instead of immediately returning to main menu, show a "Disconnected — Reconnect?"
  dialog.
- If the player clicks "Reconnect", attempt to reconnect to the matchmaking server, then
  request a rejoin for the same game session.
- If the player clicks "Leave", proceed to main menu with normal quit behavior.
- The player can return to this dialog later from the main menu (e.g., a "Rejoin Game"
  button) as long as the game is still running.

**File (new or modify):** `tt/classes/com/oddlabs/tt/form/ReconnectForm.java`

- Show disconnect reason, "Reconnect" and "Leave" buttons.
- On "Reconnect": Create a new `Client` with `rejoin = true` targeting the same game.

### 5.11 Rated game rejoin rules

**File:** `tt/classes/com/oddlabs/tt/net/Server.java`

- Rejoin should be allowed in rated games (it prevents unfair losses from network issues).
- No special restrictions — a disconnected player can always rejoin regardless of game
  type.

**File:** `server/classes/com/oddlabs/matchserver/TimestampedGameSession.java`

- If the game ends while a player is still in `PARTICIPANT_DISCONNECTED` state in a rated
  game, treat it as a loss for rating purposes.

---

## Phase 6: Late Join (Human Takes Over AI Slot)

**Goal:** A human player can join a running game and take control of a slot currently
occupied by an AI, inheriting all of the AI's units, buildings, and resources.

**Prerequisite:** Phases 2 and 3 (event recording + fast-forward catch-up). Phase 6
reuses `ReplayWorldStarter` but adds a synchronized AI-to-human transition that must
happen at the exact same tick on every peer.

### How AI Players Work Today

Understanding this is critical to the design:

1. **AI doesn't use the network.** The `AI` class (`tt/player/AI.java:27`) is an
   `Animated` object registered with `world.getAnimationManagerRealTime()` (line 54).
   It calls `Player` methods directly — `owner.setTarget()`, `owner.buildBuilding()`,
   etc. — without going through the Router.

2. **Every peer runs the AI independently.** Since all game logic uses `strictfp` and the
   world's seeded `Random` (`AI.java:293`), every peer computes the same AI decisions and
   reaches the same state. This is already validated by the checksum system.

3. **AI players are in `nonhuman_players`.** In `PeerHub` constructor (line 131-136),
   non-HUMAN slots are added to a `Set nonhuman_players` and get no `Peer` object. They
   have no Router presence at all.

4. **`player.setAI(ai)` / `player.setAI(null)`.** `Player.java:265-270` has a simple
   setter. Setting AI to null stops AI decisions; setting it to an AI instance starts
   them. The AI self-registers with the animation manager on construction.

### The Synchronization Challenge

This is the hardest part of late join and what makes it different from spectator/rejoin:

When a human takes over an AI slot, **every peer must simultaneously**:
- Stop running the AI for that player (`player.setAI(null)`)
- Remove the player from `nonhuman_players`
- Create a new `Peer` for this player
- Start routing commands through the Router

If this transition doesn't happen at the **exact same tick** on every peer, determinism
breaks: one peer would execute an AI command that another peer skips (or vice versa),
causing an immediate checksum mismatch.

**Solution:** Use a synchronized game event. The host broadcasts a `takeoverSlot` event
through the Router, timestamped to a specific tick. When each peer processes this event
at that tick, they all perform the AI->Human transition atomically.

### 6.1 Takeover event protocol

**File:** `tt/classes/com/oddlabs/tt/net/PeerHubInterface.java`

- Add: `void takeoverSlot(int slot_index);`

This is a new peer hub event (like `chat` and `beacon`) that flows through the Router to
all peers. When executed at the designated tick:
1. `player.getAI()` is unregistered from the animation manager and set to null
2. The player is removed from `nonhuman_players`
3. A new `Peer` is created for this player at an appropriate `peer_index`
4. The `peer_index_to_peer`, `player_to_peer`, and `peer_to_player` maps are updated

### 6.2 PeerHub handles takeover

**File:** `tt/classes/com/oddlabs/tt/net/PeerHub.java`

- Add `handleTakeoverSlot(int slot_index)` method, called when the `takeoverSlot` event
  is executed at its designated tick:
  ```
  1. Player player = world.getPlayers()[slot_index]
  2. AI ai = player.getAI()
  3. if (ai != null):
       world.getAnimationManagerRealTime().removeAnimation(ai)
       player.setAI(null)
  4. nonhuman_players.remove(player)
  5. Allocate a new peer_index for this player
  6. Create a new Peer and insert into peer_index_to_peer
  7. Add to player_to_peer / peer_to_player maps
  8. If this is the local joining player:
       - Set local_player = player
       - Exit observer mode, enable full controls
  9. Broadcast system chat: "PlayerName has taken over from AI"
  ```

- **Key detail:** The `takeoverSlot` event must be processed *before* `world.tick()`
  in `doTick()`. Currently events are executed at lines 345-355, then `world.tick(t)` at
  line 356. Since `takeoverSlot` is a `PeerHubInterface` event (not a `PlayerInterface`
  event), it goes through `receiveEvent()` (line 208) rather than
  `receiveGameStateEvent()` (line 222). These are processed in `Peer.executeEvents()`
  which runs before `world.tick()` — so the ordering is already correct.

  But actually, `takeoverSlot` isn't a per-peer event — it's a global event that all
  peers must execute. It should flow through `relayGameStateEvent()` (timestamped) rather
  than `relayEvent()` (untimestamped), so it's executed at a specific tick.

  **Alternative:** Add a new event type to `PlayerInterface`:
  `void takeoverSlot(int slot_index);` — since `PlayerInterface` events are already
  timestamped and executed synchronously at specific ticks via the `Peer.executeEvents()`
  mechanism. This is cleaner because it reuses the existing synchronization infrastructure.

### 6.3 Server accepts late-join connections

**File:** `tt/classes/com/oddlabs/tt/net/Server.java`

- Modify `incomingConnection()`: When a mid-game connection arrives that is neither a
  rejoin (no matching disconnected player) nor explicitly a spectator request, check if
  there are AI slots eligible for takeover.
- Track which slots are AI-controlled and whether the host has enabled late join
  (new `boolean allow_late_join` on `Game`).
- Send the late-joiner the same catch-up data as spectator/rejoin: world seed + event log
  + target slot index.

**File:** `tt/classes/com/oddlabs/tt/net/GameClientInterface.java`

- Add: `void startLateJoin(Game game, WorldGenerator generator, PlayerSlot[] players,
  UnitInfo[] unit_infos, byte[] event_log, int current_tick, short takeover_slot);`

### 6.4 Client handles late join

**File:** `tt/classes/com/oddlabs/tt/net/Client.java`

- Implement `startLateJoin()`: Invoke `ReplayWorldStarter` with `mode = LATE_JOIN` and
  the target `takeover_slot`.

### 6.5 ReplayWorldStarter late-join path

**File:** `tt/classes/com/oddlabs/tt/net/ReplayWorldStarter.java` (created in Phase 3.3)

During fast-forward, the AI runs normally as part of `world.tick()` — the AI is an
`Animated` registered with `animationManagerRealTime`, and `World.tick()` (line 159-165)
runs both game-time and real-time animation managers. So during catch-up, the AI produces
the same commands it did on every other peer. No special handling needed.

After fast-forward completes:

```
1. Connect to the Router (same as spectator/rejoin)
2. Host broadcasts takeoverSlot(takeover_slot) event through the Router
3. All peers (including the late-joiner) process the event at the designated tick:
   - AI is stopped
   - Player removed from nonhuman_players
   - New Peer created
4. Late-joiner's local_player is set to world.getPlayers()[takeover_slot]
5. Late-joiner exits observer mode, gets full controls
6. Late-joiner gets an active PlayerInterface (can send commands)
```

**Gap handling:** Same as spectator Phase 3.4 — buffer Router events during catch-up,
process them after fast-forward.

### 6.6 AI cleanup on takeover

**File:** `tt/classes/com/oddlabs/tt/player/AI.java`

The AI registers itself with the real-time animation manager in its constructor (line 54):
```java
owner.getWorld().getAnimationManagerRealTime().registerAnimation(this);
```

When the takeover happens:
- Call `world.getAnimationManagerRealTime().removeAnimation(ai)` to stop the AI from
  ticking.
- Call `player.setAI(null)` to clear the reference.
- The AI's internal state (sleep timer, construction flags) is abandoned — it's no longer
  needed.

**No cleanup of AI-issued commands needed.** Commands the AI already issued (e.g., "build
weapons infinitely") persist as normal game state. The human player inherits exactly what
the AI was doing: units mid-task, buildings mid-production, everything. This is
intentional — the human picks up where the AI left off.

### 6.7 Player name and identity

When a human takes over an AI slot, the `PlayerInfo` for that slot still has the AI's
name (e.g., "Rolf", "Bjorn" from `ai_names` in `Server.java:56`).

Options:
- **Option A (simpler):** Keep the AI's name. The player is known by the AI's name for
  the rest of the game. Chat and stats use this name.
- **Option B:** Update `PlayerInfo.name` to the human's nick. This requires broadcasting
  the name change to all peers. Since `PlayerInfo` is used in the `Player` object
  everywhere, this is straightforward but requires a new event type.

Recommend **Option B** for clarity. Add a `renamePlayer(int slot, String name)` event to
`PeerHubInterface`, broadcast alongside `takeoverSlot`.

### 6.8 Lobby UI for late join

**File:** `tt/classes/com/oddlabs/tt/form/SelectGameMenu.java`

- When listing in-progress games (Phase 4.4's `TYPE_SPECTATABLE_GAME`), indicate which
  games have AI slots available for takeover.
- Add a "Join" option (distinct from "Spectate") for games with available AI slots.
- If a game has both AI slots and spectating enabled, show both options.

**File:** `common/classes/com/oddlabs/matchmaking/Game.java`

- Add `boolean allow_late_join` field.
- Matchmaking server includes AI slot count in game listing data.

### 6.9 Rated game restrictions

Late join should NOT be allowed in rated games. The rating system assumes a fixed set of
players from game start. Replacing an AI mid-game introduces rating complications:
- The AI's early-game performance affects the late-joiner's rating outcome.
- The game was balanced for a specific number of human players.

For unrated/casual games, late join adds a fun social dynamic — a friend can jump into
your game and take over a bot.

### 6.10 Multiple AI slots

If a game has multiple AI slots, late join can happen for each one independently. Each
`takeoverSlot` event is a separate synchronized event at potentially different ticks.

The `Server` should track which AI slots have been taken over and which are still
available. A second late-joiner can take a different AI slot.

### 6.11 Team considerations

The late-joining human inherits the AI's team assignment. This means:
- They can only join on a team that currently has an AI player.
- If the game is 2v2 with 2 humans vs 2 AIs, a late-joiner takes an AI slot on the AI
  team.
- The host could optionally allow the late-joiner to pick which AI slot to take (if
  multiple are available), presented as a slot selection in the join UI.

---

## File Change Summary

### New Files

| File | Purpose |
|------|---------|
| `tt/classes/com/oddlabs/tt/viewer/SpectatorInGameInfo.java` | InGameInfo implementation for spectator mode |
| `tt/classes/com/oddlabs/tt/net/EventLog.java` | Records and serializes game events for catch-up replay |
| `tt/classes/com/oddlabs/tt/net/ReplayWorldStarter.java` | Fast-forward world construction for mid-game spectate, rejoin, and late join |
| `tt/classes/com/oddlabs/tt/form/ReconnectForm.java` | Disconnect dialog with reconnect/leave options |

### Modified Files

| File | Changes |
|------|---------|
| `tt/net/Server.java` | Accept spectator/rejoin/late-join connections; track spectators in separate `spectator_connections` list; allow mid-game join; match rejoin identity; track AI slots for late join |
| `tt/net/Client.java` | Add `spectator`/`rejoin`/`late_join` flags; handle `startSpectating()`/`startRejoin()`/`startLateJoin()` |
| `tt/net/GameClientInterface.java` | Add `startSpectating()`, `startRejoin()`, and `startLateJoin()` methods |
| `tt/net/GameServerInterface.java` | Add spectator join method |
| `tt/net/WorldStarter.java` | Support spectator viewer creation with observer mode from tick 0 |
| `tt/net/PeerHub.java` | Add `is_spectator` flag; skip checksum sending; no-op player interface; event logging on host; `temporarily_disconnected` set (no timeout — always rejoinable); `rejoinPeer()` method; `playerReconnected()` handler; `handleTakeoverSlot()` for AI->human transition |
| `tt/net/PeerHubInterface.java` | Add `takeoverSlot(int slot_index)` and optionally `renamePlayer(int slot, String name)` |
| `tt/net/Peer.java` | No changes needed (events execute the same way) |
| `tt/player/AI.java` | No code changes needed (cleanup via existing `removeAnimation` + `setAI(null)`) |
| `tt/delegate/SelectionDelegate.java` | No changes needed (observer mode already works) |
| `tt/viewer/WorldViewer.java` | Minor: accept spectator flag for PeerHub construction |
| `common/matchmaking/MatchmakingServerInterface.java` | Add `TYPE_SPECTATABLE_GAME`; add `playerDisconnectedNotify()`/`playerReconnectedNotify()` |
| `common/matchmaking/Game.java` | Add `allow_spectators` and `allow_late_join` fields |
| `common/router/Session.java` | Support adding a player to an already-started session |
| `common/router/RouterClient.java` | Handle rejoin reconnection; optionally defer `playerDisconnected` broadcast |
| `common/router/RouterClientInterface.java` | Add `playerReconnected(int client_id)` |
| `server/matchserver/TimestampedGameSession.java` | Add `PARTICIPANT_DISCONNECTED` state; `participantRejoined()` method |
| `server/matchserver/MatchmakingServer.java` | Serve spectatable game list; handle disconnect/reconnect notifications; include AI slot info in listings |
| `tt/form/SelectGameMenu.java` | Add "Spectate" and "Join" options for in-progress games |

---

## Implementation Order

```
Phase 1 (Pre-game spectating — foundation)
  1.1  Server.java spectator connection tracking (separate from player slots)
  1.2  Client.java spectator flag
  1.3  WorldStarter spectator path (independent WorldViewer per spectator)
  1.4  SpectatorInGameInfo
  1.5  PeerHub spectator mode (is_spectator flag, no-op PlayerInterface)
  1.6  Lobby UI (spectate button)

Phase 2 (Event recording — prerequisite for mid-game spectate & rejoin)
  2.1  EventLog class + host-side recording in PeerHub
  2.2  Store world generation params for late joiners

Phase 3 (Mid-game spectator join)
  3.1  Server accepts mid-game spectator connections
  3.2  Client handles startSpectating()
  3.3  ReplayWorldStarter with fast-forward (shared with Phase 5)
  3.4  Catch-up to live transition
  3.5  Router integration for spectator peers

Phase 4 (Spectator polish)
  4.1  Spectator disconnect handling
  4.2  Spectator chat
  4.3  Game-over for spectators
  4.4  Matchmaking server game listing
  4.5  Max spectator count
  4.6  Rated game considerations

Phase 5 (Player rejoin — builds on Phases 2 & 3)
  5.1  Graceful disconnect (temporarily_disconnected set in PeerHub; always rejoinable)
  5.2  Server accepts rejoin connections (identity matching)
  5.3  Client handles startRejoin()
  5.4  ReplayWorldStarter rejoin path (resume control vs observer)
  5.5  PeerHub re-registration (rejoinPeer method)
  5.6  Router session re-entry (add player to started session)
  5.7  PeerHub handles playerReconnected notification
  5.8  Matchmaking server: PARTICIPANT_DISCONNECTED state
  5.9  Headless units during disconnect (no-op; optional AI deferred)
  5.10 Rejoin UI (ReconnectForm)
  5.11 Rated game rejoin rules

Phase 6 (Late join: human takes over AI slot — builds on Phases 2 & 3)
  6.1  takeoverSlot event protocol (PeerHubInterface or PlayerInterface)
  6.2  PeerHub handleTakeoverSlot (synchronized AI->human transition)
  6.3  Server accepts late-join connections (AI slot matching)
  6.4  Client handles startLateJoin()
  6.5  ReplayWorldStarter late-join path (fast-forward with AI, then takeover)
  6.6  AI cleanup on takeover (removeAnimation + setAI(null))
  6.7  Player name and identity (renamePlayer event)
  6.8  Lobby UI for late join (show AI slots in game listings)
  6.9  Rated game restrictions (disallow late join in rated)
  6.10 Multiple AI slots (independent takeover per slot)
  6.11 Team considerations (inherit AI's team)
```

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Deterministic replay diverges from live game | Spectator/rejoin sees wrong state | Use existing `strictfp` math guarantees; spectators don't participate in checksums; rejoin player verifies first checksum after catch-up |
| Event log grows unboundedly for long games | Memory pressure on host | Cap at ~5MB (sufficient for 4+ hours); compress with gzip |
| Router doesn't support dynamic participant changes | Can't add spectators/rejoin mid-game | Use host-relay approach as fallback (host forwards events over separate connections) |
| Fast-forward is slower than expected | Long wait for spectators/rejoin | Show progress bar; profile and optimize tight loop; consider skipping non-essential per-tick work during catch-up |
| Spectators leak game information in rated games | Competitive integrity | Add optional spectator delay (30-60 second buffer); allow hosts to disable spectating for rated games |
| Network bandwidth for sending event log | Slow spectator/rejoin connect | Event logs are small (~1MB for 30-min game); compress before sending |
| Rejoin player desyncs after catch-up | Player kicks or game corruption | Rejoin player computes checksum after fast-forward and compares against host's checksum at same tick before connecting to Router; abort rejoin if mismatch |
| Router `Session` re-entry breaks checksum coordination | Checksum comparisons fail during rejoin transition | Skip checksum comparison for one cycle after rejoin; rejoin player sends first checksum only after processing at least one full live tick |
| Multiple disconnects/rejoins by same player | Event log ordering issues; slot confusion | Preserve original peer_index across all reconnections; each rejoin replays from tick 0 so state is always consistent |
| Disconnected player's units get destroyed while away | Poor experience on rejoin | Intentional design — this is part of the game. The game continues normally; the player's units idle and can be attacked. The player can rejoin at any time and resume control of whatever remains |
| AI->human transition desync (Phase 6) | All peers must stop AI at exact same tick | Use timestamped Router event (`takeoverSlot`) so all peers execute the transition at the same tick; this is the same mechanism used for all other synchronized commands |
| AI random state diverges during fast-forward (Phase 6) | Late-joiner's AI produces different commands than live peers' AI | AI uses `world.getRandom()` which is deterministic from the world seed; `World.tick()` runs both animation managers, so AI ticks identically during fast-forward; already proven correct by existing checksum system |
| Late-joiner inherits bad AI state | Frustrating experience (AI spent all resources, built poorly) | Intentional — this is the trade-off for joining late. Show the AI's current unit/building count in the join UI so players can make an informed choice |
