package com.oddlabs.tt.input;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.render.Renderer;

public final strictfp class KeyboardInput {
    private static final int LITTLE_WARP = 1000;
    public static final int MEDIUM_WARP = 10000;
    public static final int LARGE_WARP = 100000;
    private static final int GOTO_END_OF_LOG_WARP = Integer.MAX_VALUE / 2;

    private static final KeyboardInput instance;

    private boolean shift_down;
    private boolean control_down;
    private boolean menu_down;

    static {
        instance = new KeyboardInput();
    }

    public static final void reset() {
        while (Keyboard.isCreated() && Keyboard.next())
            ;
    }

    public static final boolean isMenuDown() {
        return instance.menu_down;
    }

    private final boolean checkMagicKey(
            Deterministic deterministic,
            boolean event_key_state,
            int event_key,
            boolean override,
            boolean repeat) {
        boolean keys_enabled =
                Settings.getSettings().inDeveloperMode() && control_down && shift_down && !repeat;
        if (event_key_state && (keys_enabled || override)) {
            // check for special events that shouldn't generate events
            switch (event_key) {
                case Keyboard.KEY_RIGHT:
                    AnimationManager.warpTime(LITTLE_WARP);
                    break;
                case Keyboard.KEY_UP:
                    AnimationManager.warpTime(MEDIUM_WARP);
                    break;
                case Keyboard.KEY_PRIOR:
                    AnimationManager.warpTime(LARGE_WARP);
                    break;
                case Keyboard.KEY_Q:
                    System.out.println("Exit forced with ctrl+Q");
                    Renderer.shutdown();
                    break;
                case Keyboard.KEY_SPACE:
                    AnimationManager.toggleTimeStop();
                    break;
                case Keyboard.KEY_END:
                    System.out.println("WARP UNTIL END OF EVENT LOG");
                    AnimationManager.warpTime(GOTO_END_OF_LOG_WARP);
                    break;
                default:
                    break;
            }
        }
        return keys_enabled;
    }

    public static final void checkMagicKeys() {
        instance.doCheckMagicKeys();
    }

    public final void doCheckMagicKeys() {
        Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
        if (deterministic.isPlayback()) {
            Keyboard.poll();
            while (Keyboard.next()) {
                int event_key = Keyboard.getEventKey();
                boolean event_key_state = Keyboard.getEventKeyState();
                checkMagicKey(
                        deterministic, event_key_state, event_key, true, Keyboard.isRepeatEvent());
            }
        }
    }

    public static final boolean poll(GUIRoot gui_root) {
        return instance.doPoll(gui_root);
    }

    public final boolean doPoll(GUIRoot gui_root) {
        Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
        LocalInput local_input = LocalInput.getLocalInput();
        boolean result = false;
        Keyboard.poll();
        while (deterministic.log(Keyboard.next())) {
            result = true;
            int event_key = deterministic.log(Keyboard.getEventKey());
            boolean event_key_state = deterministic.log(Keyboard.getEventKeyState());
            char event_character = deterministic.log(Keyboard.getEventCharacter());
            boolean repeat_event = deterministic.log(Keyboard.isRepeatEvent());
            switch (event_key) {
                case Keyboard.KEY_LSHIFT:
                case Keyboard.KEY_RSHIFT:
                    shift_down = event_key_state;
                    break;
                case Keyboard.KEY_LCONTROL:
                case Keyboard.KEY_RCONTROL:
                    control_down = event_key_state;
                    break;
                case Keyboard.KEY_LMENU:
                case Keyboard.KEY_RMENU:
                    menu_down = event_key_state;
                    break;
            }
            if (checkMagicKey(deterministic, event_key_state, event_key, false, repeat_event))
                continue;
            if (event_key == Keyboard.KEY_NONE) {
                local_input.keyTyped(gui_root, event_key, event_character);
            } else if (event_key_state) {
                local_input.keyPressed(
                        gui_root,
                        event_key,
                        event_character,
                        shift_down,
                        control_down,
                        menu_down,
                        repeat_event);
            } else {
                local_input.keyReleased(
                        gui_root, event_key, event_character, shift_down, control_down, menu_down);
            }
        }
        return result;
    }
}
