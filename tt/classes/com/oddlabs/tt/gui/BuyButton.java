package com.oddlabs.tt.gui;

import com.oddlabs.tt.form.QuestionForm;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;

import java.util.ResourceBundle;

public final strictfp class BuyButton extends HorizButton {
    public BuyButton(GUIRoot gui_root, String caption, int width) {
        super(caption, width);
        addListener(gui_root, this);
    }

    public static final void addListener(GUIRoot gui_root, ButtonObject button) {
        button.addMouseClickListener(new BuyListener(gui_root));
    }

    private static final strictfp class BuyListener implements MouseClickListener {
        private final GUIRoot gui_root;

        public BuyListener(GUIRoot gui_root) {
            this.gui_root = gui_root;
        }

        public final void mouseClicked(int button, int x, int y, int clicks) {
            if (Settings.getSettings().buy_now_only_quit) {
                Renderer.shutdown();
            } else {
                ResourceBundle bundle = ResourceBundle.getBundle(BuyButton.class.getName());
                String buy_quit_message = Utils.getBundleString(bundle, "buy_quit_message");
                gui_root.addModalForm(new QuestionForm(buy_quit_message, new ActionBuyListener()));
            }
        }
    }

    private static final strictfp class ActionBuyListener implements MouseClickListener {
        public final void mouseClicked(int button, int x, int y, int clicks) {
            Renderer.shutdown();
        }
    }
}
