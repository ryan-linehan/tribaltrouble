package com.oddlabs.tt.util;

import com.oddlabs.tt.net.Distributable;

public strictfp interface Target extends Distributable {
    public static final int ACTION_DEFAULT = 1;
    public static final int ACTION_MOVE = 2;
    public static final int ACTION_ATTACK = 3;
    public static final int ACTION_GATHER_REPAIR = 4;
    public static final int ACTION_DEFEND = 5;

    int getGridX();

    int getGridY();

    float getPositionX();

    float getPositionY();

    float getSize();

    boolean isDead();
}
