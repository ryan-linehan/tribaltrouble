package com.oddlabs.tt.camera;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.landscape.World;

public final strictfp class MenuCamera extends Camera {
    private static final float ANGLE_DELTA = 0.020f;
    private static final float RADIUS = 176f;
    private static final float HEIGHT = 0f;
    private static final float LANDSCAPE_OFFSET = 5f;
    private static final float CENTER_X = 128f;
    private static final float CENTER_Y = 128f;
    private static final float CENTER_Z = 128f; // NOT HEIGHT!

    private final World world;
    private final AnimationManager manager;
    private float center_angle;

    public MenuCamera(World world, AnimationManager manager) {
        super(world.getHeightMap(), new CameraState());
        this.world = world;
        this.manager = manager;
        reset();
    }

    private void reset() {
        center_angle = 1;
        getState().setCurrentVertAngle(-(float) StrictMath.atan((HEIGHT - CENTER_Z) / RADIUS));
        updatePos(0f);
    }

    private final void updatePos(float t) {
        center_angle = (center_angle + ANGLE_DELTA * t) % (2 * (float) StrictMath.PI);
        getState().setCurrentX(CENTER_X + RADIUS * (float) StrictMath.cos(center_angle));
        getState().setCurrentY(CENTER_Y + RADIUS * (float) StrictMath.sin(center_angle));
        getState().setCurrentHorizAngle((float) StrictMath.PI * .925f + center_angle);
        getState().setCurrentZ(LANDSCAPE_OFFSET);
    }

    public final void doAnimate(float t) {
        updatePos(t);
        world.tick(t);
        manager.runAnimations(t);
    }
}
