package com.oddlabs.tt.render;

abstract strictfp class WhiteModelVisitor extends ModelVisitor {
    private static final float[] COLOR_TEAM = {1f, 1f, 1f};

    public final float[] getSelectionColor(ElementRenderState render_state) {
        return COLOR_TEAM;
    }

    public final float[] getTeamColor(ElementRenderState render_state) {
        return COLOR_TEAM;
    }
}
