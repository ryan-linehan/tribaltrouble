package com.oddlabs.tt.util;

import com.oddlabs.tt.render.Display;

public final strictfp class BackBufferRenderer extends OffscreenRenderer {
    private static boolean back_buffer_dirty = false;

    public static final boolean isBackBufferDirty() {
        boolean result = back_buffer_dirty;
        back_buffer_dirty = false;
        return result;
    }

    protected BackBufferRenderer(int width, int height, boolean use_copyteximage) {
        super(width, height, use_copyteximage);
        pushGLState();
        back_buffer_dirty = true;
        Display.isDirty();
        init();
    }

    public final boolean isLost() {
        return Display.isDirty();
    }

    protected final void finish() {
        popGLState();
    }
}
