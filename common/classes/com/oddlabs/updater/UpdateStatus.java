package com.oddlabs.updater;

import java.io.Serializable;

public final strictfp class UpdateStatus implements Serializable {
    private static final long serialVersionUID = 3;

    public static final int LOG = 1;
    public static final int NO_UPDATES = 2;
    public static final int ERROR = 3;
    public static final int UPDATE_COMPLETE = 4;
    public static final int EOF = 5;

    // subtypes
    public static final int DELETING = 10;
    public static final int COPYING = 11;
    public static final int UPDATED = 12;
    public static final int UPDATING = 13;
    public static final int CHECKED = 14;
    public static final int INIT = 15;
    public static final int CHECKING = 16;
    public static final int COPIED = 17;
    public static final int NONE = 18;

    private final String message;
    private final Throwable exception;
    private final int kind;
    private final int sub_type;

    public UpdateStatus(Throwable t) {
        this(ERROR, NONE, null, t);
    }

    public UpdateStatus(int kind, String message) {
        this(kind, NONE, message);
    }

    public UpdateStatus(int kind, int sub_type, String message) {
        this(kind, sub_type, message, null);
    }

    public UpdateStatus(int kind, int sub_type, String message, Throwable t) {
        assert kind != LOG || sub_type != NONE;
        this.kind = kind;
        this.sub_type = sub_type;
        this.message = message;
        this.exception = t;
    }

    public int getSubType() {
        return sub_type;
    }

    public int getKind() {
        return kind;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getException() {
        return exception;
    }
}
