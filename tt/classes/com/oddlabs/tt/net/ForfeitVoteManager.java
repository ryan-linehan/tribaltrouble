package com.oddlabs.tt.net;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final strictfp class ForfeitVoteManager {
    private static final long VOTE_TIMEOUT_MILLIS = 60000; // 60 seconds
    private static final long VOTE_COOLDOWN_MILLIS = 600000; // 10 minutes

    private final World world;
    private final Map<Integer, TeamVoteState> team_votes = new HashMap<>();

    public ForfeitVoteManager(World world) {
        this.world = world;
    }

    /**
     * Initiates a forfeit vote for a team.
     *
     * @return true if vote was initiated, false if on cooldown
     */
    public boolean initiateVote(int team) {
        TeamVoteState state = team_votes.get(team);
        long current_time = System.currentTimeMillis();

        // Check cooldown
        if (state != null && state.last_vote_initiation_time > 0) {
            long time_since_last_vote = current_time - state.last_vote_initiation_time;
            if (time_since_last_vote < VOTE_COOLDOWN_MILLIS) {
                return false; // Still on cooldown
            }
        }

        // Create new vote state
        TeamVoteState new_state = new TeamVoteState();
        new_state.vote_start_time = current_time;
        new_state.last_vote_activity_time = current_time;
        new_state.last_vote_initiation_time = current_time;
        team_votes.put(team, new_state);

        return true;
    }

    /**
     * Records a forfeit vote from a player.
     *
     * @return VoteResult indicating the outcome
     */
    public VoteResult castVote(String player_name, int team) {
        TeamVoteState state = team_votes.get(team);

        // No active vote
        if (state == null || state.vote_start_time == 0) {
            return VoteResult.NO_ACTIVE_VOTE;
        }

        // Check if vote has timed out
        long current_time = System.currentTimeMillis();
        if (current_time - state.last_vote_activity_time > VOTE_TIMEOUT_MILLIS) {
            // Vote timed out
            clearVote(team);
            return VoteResult.VOTE_TIMED_OUT;
        }

        // Player already voted
        if (state.voters.contains(player_name)) {
            return VoteResult.ALREADY_VOTED;
        }

        // Record the vote
        state.voters.add(player_name);
        state.last_vote_activity_time = current_time; // Reset timeout

        // Check if vote passes (unanimous)
        int team_size = getTeamSize(team);
        if (state.voters.size() >= team_size) {
            clearVote(team);
            return VoteResult.VOTE_PASSED;
        }

        return VoteResult.VOTE_RECORDED;
    }

    /** Gets the current vote progress for a team. */
    public VoteProgress getVoteProgress(int team) {
        TeamVoteState state = team_votes.get(team);
        if (state == null || state.vote_start_time == 0) {
            return null;
        }

        long current_time = System.currentTimeMillis();
        if (current_time - state.last_vote_activity_time > VOTE_TIMEOUT_MILLIS) {
            clearVote(team);
            return null;
        }

        VoteProgress progress = new VoteProgress();
        progress.votes_cast = state.voters.size();
        progress.votes_needed = getTeamSize(team);
        progress.time_remaining =
                VOTE_TIMEOUT_MILLIS - (current_time - state.last_vote_activity_time);
        return progress;
    }

    /** Gets cooldown remaining in milliseconds, or 0 if no cooldown. */
    public long getCooldownRemaining(int team) {
        TeamVoteState state = team_votes.get(team);
        if (state == null || state.last_vote_initiation_time == 0) {
            return 0;
        }

        long current_time = System.currentTimeMillis();
        long time_since_last_vote = current_time - state.last_vote_initiation_time;
        long remaining = VOTE_COOLDOWN_MILLIS - time_since_last_vote;

        return remaining > 0 ? remaining : 0;
    }

    private void clearVote(int team) {
        TeamVoteState state = team_votes.get(team);
        if (state != null) {
            state.vote_start_time = 0;
            state.last_vote_activity_time = 0;
            state.voters.clear();
            // Keep last_vote_initiation_time for cooldown tracking
        }
    }

    private int getTeamSize(int team) {
        Player[] players = world.getPlayers();
        int count = 0;
        for (int i = 0; i < players.length; i++) {
            Player player = players[i];
            if (player != null && !player.isSpectator()) {
                PlayerInfo info = player.getPlayerInfo();
                if (info.getTeam() == team) {
                    count++;
                }
            }
        }
        return count;
    }

    private static final class TeamVoteState {
        long vote_start_time = 0;
        long last_vote_activity_time = 0;
        long last_vote_initiation_time = 0;
        Set<String> voters = new HashSet<>();
    }

    public static final class VoteProgress {
        public int votes_cast;
        public int votes_needed;
        public long time_remaining;
    }

    public enum VoteResult {
        NO_ACTIVE_VOTE,
        VOTE_TIMED_OUT,
        ALREADY_VOTED,
        VOTE_RECORDED,
        VOTE_PASSED
    }
}
