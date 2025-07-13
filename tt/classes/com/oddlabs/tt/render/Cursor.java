package com.oddlabs.tt.render;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class Cursor {
    private final long handle;

    public static final int CURSOR_ONE_BIT_TRANSPARENCY = 1;

    public Cursor(int width, int height, ByteBuffer pixels, int xHot, int yHot) {
        GLFWImage image = GLFWImage.malloc();
        image.set(width, height, pixels);
        this.handle = GLFW.glfwCreateCursor(image, xHot, yHot);
        image.free();
    }

    public Cursor(int shape) {
        // For standard system cursors, e.g., GLFW.GLFW_HAND_CURSOR
        this.handle = GLFW.glfwCreateStandardCursor(shape);
    }

    public long getHandle() {
        return handle;
    }

    public void destroy() {
        if (handle != MemoryUtil.NULL) {
            GLFW.glfwDestroyCursor(handle);
        }
    }

    public static int getCapabilities() {
        return 0;
    }
}
