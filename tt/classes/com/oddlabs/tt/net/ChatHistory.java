package com.oddlabs.tt.net;

import java.util.LinkedList;
import java.util.List;

public abstract strictfp class ChatHistory implements ChatListener {
    private static final int MAX_HISTORY = 50;

    private final List messages = new LinkedList();

    public final void clear() {
        messages.clear();
    }

    public abstract void chat(ChatMessage message);

    protected final void addMessage(String msg) {
        messages.add(msg);
        if (messages.size() > MAX_HISTORY) messages.remove(0);
    }

    final List getMessages() {
        return messages;
    }
}
