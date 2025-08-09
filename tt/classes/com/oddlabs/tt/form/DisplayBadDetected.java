package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseClickListener;

public final strictfp class DisplayBadDetected extends Form {
    public DisplayBadDetected() {

        // Build string
        String message =
                String.format(
                        "Bad resolution or refresh rate was detected and automatically switched to"
                                + " the best available.");

        LabelBox info_label = new LabelBox(message, Skin.getSkin().getEditFont(), 500);
        addChild(info_label);
        HorizButton now_button = new HorizButton("Ok", 120);
        addChild(now_button);
        now_button.addMouseClickListener(new NowListener());

        // Place objects
        info_label.place();
        now_button.place(ORIGIN_BOTTOM_RIGHT);

        compileCanvas();
        centerPos();
    }

    private final strictfp class NowListener implements MouseClickListener {
        public final void mouseClicked(int button, int x, int y, int clicks) {
            remove();
        }
    }
}
