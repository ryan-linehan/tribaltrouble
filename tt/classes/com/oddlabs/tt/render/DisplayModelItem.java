package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Settings;
import org.lwjgl.glfw.GLFWVidMode;


public class DisplayModelItem {
    private int width;
    private int height;
    private int refreshRate;

    public DisplayModelItem(int _width, int _height, int _refreshRate) {
        width = _width;
        height = _height;
        refreshRate = _refreshRate;
    }

    public DisplayModelItem(GLFWVidMode mode) {
        width = mode.width();
        height = mode.height();
        refreshRate = mode.refreshRate();
    }

    public DisplayModelItem() {
        loadFromConfig();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj.getClass() != this.getClass())
            return false;
        
        DisplayModelItem item = (DisplayModelItem) obj;

        if(item.width() == width && item.height() == height && item.refreshRate() == refreshRate)
            return true;
        return false;
    }

    public boolean resolution_equals(DisplayModelItem item) {
        if (item == null)
            return false;

        if(item.width() == width && item.height() == height)
            return true;
        return false;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int refreshRate() {
        return refreshRate;
    }

    public void setWidth(int new_width) {
        width = new_width;
    }

    public void setHeight(int new_height) {
        height = new_height;
    }

    public void setRefreshRate(int new_refreshRate) {
        refreshRate = new_refreshRate;
    }

    public void setToConfig() {
        Settings.getSettings().new_view_width = width;
		Settings.getSettings().new_view_height = height;
		Settings.getSettings().new_view_freq = refreshRate;
		Settings.getSettings().view_width = width;
		Settings.getSettings().view_height = height;
		Settings.getSettings().view_freq = refreshRate;
    }

    private void loadFromConfig() {
        width = Settings.getSettings().view_width;
        height = Settings.getSettings().view_height;
        refreshRate = Settings.getSettings().view_freq;
    }
}
