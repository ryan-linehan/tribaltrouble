package com.oddlabs.tt.delegate;

import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.render.GW;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.RenderQueues;

public abstract strictfp class Delegate extends GUIObject {
    public Delegate() {
        setPos(0, 0);
        setCanFocus(true);
        setDim(LocalInput.getViewWidth(), LocalInput.getViewHeight());
    }

    public void displayChangedNotify(int width, int height) {
        setDim(width, height);
    }

    protected void doAdd() {
        super.doAdd();
        setFocus();
    }

    public void render3D(LandscapeRenderer renderer, RenderQueues render_queues) {}

    public void render2D() {}

    protected void renderGeometry() {}

    public boolean keyboardBlocked() {
        return false;
    }

    protected final void renderBackgroundAlpha() {
        GW.renderRect(0f, getWidth(), 0f, getHeight(), 0f, 0f, 0f, 0f, .3f);
    }
}
