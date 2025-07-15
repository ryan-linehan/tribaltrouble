package com.oddlabs.tt.render;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL11.*;

import com.oddlabs.tt.input.Keyboard;
import com.oddlabs.tt.input.Mouse;

import java.nio.IntBuffer;

public final strictfp class Display {
    private static long window;
    private static boolean created = false;
    private static boolean fullscreen = false;
    private static String title = "Tribal Trouble";
    private static int width = 800;
    private static int height = 600;
    private static boolean vsyncEnabled = false;
    
    // Callbacks
    private static GLFWKeyCallback keyCallback;
    private static GLFWCursorPosCallback cursorPosCallback;
    private static GLFWMouseButtonCallback mouseButtonCallback;
    private static GLFWScrollCallback scrollCallback;
    private static GLFWCharCallback charCallback;
    
    // Input state
    private static boolean[] keys = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private static boolean[] mouseButtons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];
    private static double mouseX, mouseY;
    private static double scrollX, scrollY;
    
    public static void setTitle(String title) {
        Display.title = title;
        if (created) {
            GLFW.glfwSetWindowTitle(window, title);
        }
    }
    
    public static void setFullscreen(boolean fullscreen) {
        Display.fullscreen = fullscreen;
        if (created) {
            // TODO: Implement fullscreen switching
        }
    }
    
    public static void setVSyncEnabled(boolean enabled) {
        vsyncEnabled = enabled;
        if (created) {
            GLFW.glfwSwapInterval(enabled ? 1 : 0);
        }
    }
    
    public static boolean isCreated() {
        return created;
    }
    
    public static void create() {
        if (created) {
            return;
        }

        if (!GLFW.glfwInit()) {
            System.out.println("Unable to initialize GLFW");
            return;
        }
        
        long monitor = GLFW.glfwGetPrimaryMonitor();
        GLFW.glfwWindowHint(GLFW.GLFW_REFRESH_RATE, GLFW.glfwGetVideoMode(monitor).refreshRate());
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        
       /* GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);*/
        
        GLFWVidMode videoMode = GLFW.glfwGetVideoMode(monitor);

        width = videoMode.width();
        height = videoMode.height();

        window = GLFW.glfwCreateWindow(
            videoMode.width(), videoMode.height(),
            "Fullscreen", monitor, 0
        );

        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);

        if (window == 0) {
            throw new RuntimeException("Failed to create window");
        }

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        created = true;
        
        Mouse.create();
        Keyboard.create();
    }
    
    public static void destroy() {
        if (created) {
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
            created = false;
        }
    }
    
    public static void update() {
        if (created) {
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }
    
    public static boolean shouldClose() {
        return created && GLFW.glfwWindowShouldClose(window);
    }
 
    public static boolean isCloseRequested() {
        return shouldClose();
    }
    
    public static void makeCurrent() {
        if (created) {
            GLFW.glfwMakeContextCurrent(window);
        }
    }
   
    public static int getWidth() {
        return width;
    }
    
    public static int getHeight() {
        return height;
    }
    
    public static boolean isKeyDown(int key) {
        return keys[key];
    }
    
    public static boolean isMouseButtonDown(int button) {
        return mouseButtons[button];
    }
    
    public static double getMouseX() {
        return mouseX;
    }
    
    public static double getMouseY() {
        return mouseY;
    }
    
    public static void setKeyCallback(GLFWKeyCallback callback) {
        keyCallback = callback;
        if (created) {
            GLFW.glfwSetKeyCallback(window, callback);
        }
    }
    
    public static void setCursorPosCallback(GLFWCursorPosCallback callback) {
        cursorPosCallback = callback;
        if (created) {
            GLFW.glfwSetCursorPosCallback(window, callback);
        }
    }
    
    public static void setMouseButtonCallback(GLFWMouseButtonCallback callback) {
        mouseButtonCallback = callback;
        if (created) {
            GLFW.glfwSetMouseButtonCallback(window, callback);
        }
    }
    
    public static void setScrollCallback(GLFWScrollCallback callback) {
        scrollCallback = callback;
        if (created) {
            GLFW.glfwSetScrollCallback(window, callback);
        }
    }
    
    public static void setCharCallback(GLFWCharCallback callback) {
        charCallback = callback;
        if (created) {
            GLFW.glfwSetCharCallback(window, callback);
        }
    }
    
    public static String getClipboard() {
        return "";//glfwGetClipboardString(window);
    }

    public static long getWindow() {
        return window;
    }

    public static boolean isDirty() {
        return true;
    }

    public static boolean isALCreated() {
        return true;
    }
} 
