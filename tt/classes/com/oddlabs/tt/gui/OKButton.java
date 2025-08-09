package com.oddlabs.tt.gui;

import com.oddlabs.tt.util.Utils;

import java.util.ResourceBundle;

public final strictfp class OKButton extends HorizButton {
    public OKButton(int width) {
        super(
                Utils.getBundleString(ResourceBundle.getBundle(OKButton.class.getName()), "ok"),
                width);
    }
}
