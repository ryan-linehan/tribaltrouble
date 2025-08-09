package com.oddlabs.tt.net;

public final strictfp class Network {
    private static final ChatHub chat_hub = new ChatHub();
    private static final MatchmakingClient matchmaking_client = new MatchmakingClient();
    private static MatchmakingListener matchmaking_listener;

    public static final MatchmakingListener getMatchmakingListener() {
        return matchmaking_listener;
    }

    public static final void setMatchmakingListener(MatchmakingListener listener) {
        matchmaking_listener = listener;
    }

    public static final void closeMatchmakingClient() {
        matchmaking_listener = null;
        matchmaking_client.close();
    }

    public static ChatHub getChatHub() {
        return chat_hub;
    }

    public static MatchmakingClient getMatchmakingClient() {
        return matchmaking_client;
    }
}
