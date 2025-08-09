package com.oddlabs.tt.font;

import com.oddlabs.tt.animation.*;

import org.lwjgl.opengl.*;

public final strictfp class Index implements Updatable {
    public static final int INDEX_WIDTH = 1;
    private static final float BLINK_INTERVAL = .5f;

    private static final Index index = new Index();

    private final TimerAnimation timer;
    private boolean blink_on;

    private Index() {
        timer = new TimerAnimation(this, BLINK_INTERVAL);
        timer.start();
        blink_on = true;
    }

    public static final void resetBlinking() {
        index.doResetBlinking();
    }

    private final void doResetBlinking() {
        blink_on = true;
        timer.resetTime();
    }

    public static final void renderIndex(int render_x, int render_y, Font font) {
        index.doRenderIndex(render_x, render_y, font);
    }

    private final void doRenderIndex(int render_x, int render_y, Font font) {
        if (blink_on) {
            GL11.glEnd();
            GL11.glColor3f(1f, 1f, 1f);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glLineWidth(INDEX_WIDTH);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3f(render_x, render_y + font.getHeight() - 3, 0f);
            GL11.glVertex3f(render_x, render_y + 3, 0f);
            GL11.glVertex3f(render_x + 1, render_y + 3, 0f);
            GL11.glVertex3f(render_x + 1, render_y + font.getHeight() - 3, 0f);
            GL11.glEnd();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBegin(GL11.GL_QUADS);
        }
    }

    public final void update(Object anim) {
        blink_on = !blink_on;
    }
}
