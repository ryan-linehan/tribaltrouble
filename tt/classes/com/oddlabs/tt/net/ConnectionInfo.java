package com.oddlabs.tt.net;

import com.oddlabs.net.ARMIEvent;

import java.util.ArrayList;
import java.util.List;

public final strictfp class ConnectionInfo {
    private final int priority;
    private final List backlog = new ArrayList();

    public ConnectionInfo(int priority) {
        this.priority = priority;
    }

    public final int getPriority() {
        return priority;
    }

    public final void addEvent(ARMIEvent event) {
        backlog.add(event);
    }

    public final List getBackLog() {
        return backlog;
    }
}
