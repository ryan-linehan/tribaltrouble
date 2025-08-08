package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.CancelListener;
import com.oddlabs.tt.gui.DoNowListener;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.render.DisplayModel;
import com.oddlabs.tt.render.DisplayModelItem;

public final strictfp class DisplayApplyForm extends Form {
    private final DoNowListener donow_listener;
    private final HorizButton later_button;

    public DisplayApplyForm(DoNowListener donow_listener) {
        this.donow_listener = donow_listener;

        // Build string
        DisplayModelItem item = DisplayModel.getCurrentResolution();
        String message =
                String.format(
                        "Changing resolution to:\n"
                                + "Width: %d\n"
                                + "Height: %d\n"
                                + "Refresh rate: %d\n"
                                + "Fullscreen: %s\n\n"
                                + "Restart after applying and if you have issues, tell the"
                                + " maintainers.",
                        item.width(),
                        item.height(),
                        item.refreshRate(),
                        DisplayModel.inFullscreen() ? "Yes" : "No");

        LabelBox info_label = new LabelBox(message, Skin.getSkin().getEditFont(), 500);
        addChild(info_label);
        HorizButton now_button = new HorizButton("Apply", 120);
        addChild(now_button);
        now_button.addMouseClickListener(new NowListener());
        later_button = new HorizButton("Cancel", 120);
        addChild(later_button);
        later_button.addMouseClickListener(new CancelListener(this));

        // Place objects
        info_label.place();
        now_button.place(ORIGIN_BOTTOM_RIGHT);
        later_button.place(now_button, LEFT_MID);

        compileCanvas();
        centerPos();
    }

    public final void setFocus() {
        later_button.setFocus();
    }

    private final strictfp class NowListener implements MouseClickListener {
        public final void mouseClicked(int button, int x, int y, int clicks) {
            remove();
            donow_listener.doChange(true);
        }
    }

    protected final void doCancel() {
        donow_listener.doChange(false);
    }
}
