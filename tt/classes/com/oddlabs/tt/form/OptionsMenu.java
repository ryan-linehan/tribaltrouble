package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.GUIRoot;

public final strictfp class OptionsMenu extends AbstractOptionsMenu {
    public OptionsMenu(GUIRoot gui_root) {
        super(gui_root);
        chooseGamespeed(Globals.gamespeed);
    }
}
