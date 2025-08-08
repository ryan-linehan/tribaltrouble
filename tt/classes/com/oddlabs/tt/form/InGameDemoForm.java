package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.GUIImage;
import com.oddlabs.tt.viewer.WorldViewer;

public final strictfp class InGameDemoForm extends DemoForm {
    private final WorldViewer viewer;

    public InGameDemoForm(WorldViewer viewer, String header, GUIImage img, String text) {
        super(viewer.getGUIRoot(), header, img, text);
        this.viewer = viewer;
    }

    protected final void doAdd() {
        super.doAdd();
        viewer.setPaused(true);
    }

    protected final void doRemove() {
        super.doRemove();
        viewer.setPaused(false);
    }
}
