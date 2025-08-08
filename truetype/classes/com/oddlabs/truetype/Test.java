package com.oddlabs.truetype;

import com.oddlabs.util.Utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.glu.GLU;

import java.awt.Graphics2D;
// import java.awt.Shape;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.IntBuffer;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

public final strictfp class Test {
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 1024;
    private static final int DISPLAY_WIDTH = 1280;
    private static final int DISPLAY_HEIGHT = 1024;
    private static final float RENDER_SIZE = WIDTH * 1f;

    private static final BufferedImage image =
            new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    private static final Graphics2D g2d = (Graphics2D) image.getGraphics();

    public static final void main(String[] args) {
        try {
            Display.setDisplayMode(new DisplayMode(DISPLAY_WIDTH, DISPLAY_HEIGHT));
            Display.create();
            initGL();

            // Load font
            InputStream font_is = Utils.makeURL("/fonts/tahoma.ttf").openStream();
            java.awt.Font src_font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, font_is);
            java.awt.Font font = src_font.deriveFont(14f);

            // Load text
            InputStreamReader text_is =
                    new InputStreamReader(Utils.makeURL("/test_text.txt").openStream());
            StringBuffer str_buffer = new StringBuffer();
            int c = text_is.read();
            do {
                str_buffer.append((char) c);
                c = text_is.read();
            } while (c != -1);
            String str = str_buffer.toString();

            // Build texture
            int[] pixels = new int[WIDTH * HEIGHT];
            //			ByteBuffer pixel_data = ByteBuffer.wrap(pixels);						// NEW
            //			pixelDataFromString(WIDTH, HEIGHT, str, font, pixels);					// NEW
            IntBuffer pixel_data = BufferUtils.createIntBuffer(WIDTH * HEIGHT); // OLD
            pixel_data.put(pixels); // OLD
            pixel_data.rewind();

            int texture_handle = createTexture(WIDTH, HEIGHT, pixel_data);

            FontRenderContext frc = g2d.getFontRenderContext();
            AttributedString att_str = new AttributedString(str);
            att_str.addAttribute(TextAttribute.FONT, font);
            AttributedCharacterIterator iterator = att_str.getIterator();
            LineBreakMeasurer measurer = new LineBreakMeasurer(iterator, frc);

            while (!Display.isCloseRequested()) {
                long start_time = System.currentTimeMillis();
                for (int i = 0; i < 10; i++) {
                    pixelDataFromString(WIDTH, HEIGHT, str, font, pixels, measurer);
                    pixel_data.put(pixels); // OLD
                    pixel_data.rewind();

                    // texture_handle = createTexture(WIDTH, HEIGHT, pixel_data);
                    updateTexture(WIDTH, HEIGHT, pixel_data);
                    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
                    GL11.glLoadIdentity();

                    // Background
                    /*
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glColor4f(.1f, .1f, .1f, 1f);
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glVertex3f(0f, DISPLAY_HEIGHT-RENDER_SIZE, 1f);
                    GL11.glVertex3f(RENDER_SIZE, DISPLAY_HEIGHT-RENDER_SIZE, 1f);
                    GL11.glVertex3f(RENDER_SIZE, DISPLAY_HEIGHT, 1f);
                    GL11.glVertex3f(0f, DISPLAY_HEIGHT, 1f);
                    GL11.glEnd();
                    */

                    // Text
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    GL11.glColor4f(1f, 1f, 1f, 1f);
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glTexCoord2f(0f, 1f);
                    GL11.glVertex3f(0f, DISPLAY_HEIGHT - RENDER_SIZE, 1f);
                    GL11.glTexCoord2f(1f, 1f);
                    GL11.glVertex3f(RENDER_SIZE, DISPLAY_HEIGHT - RENDER_SIZE, 1f);
                    GL11.glTexCoord2f(1f, 0f);
                    GL11.glVertex3f(RENDER_SIZE, DISPLAY_HEIGHT, 1f);
                    GL11.glTexCoord2f(0f, 0f);
                    GL11.glVertex3f(0f, DISPLAY_HEIGHT, 1f);
                    GL11.glEnd();
                    Display.update();
                }
                long total_time = System.currentTimeMillis() - start_time;
                System.out.println("total_time = " + total_time);
            }
            Display.destroy();
        } catch (Exception t) {
            t.printStackTrace();
        }
    }

    private static final void pixelDataFromString(
            int width,
            int height,
            String str,
            java.awt.Font font,
            int[] pixels,
            LineBreakMeasurer measurer) {
        measurer.setPosition(0);
        g2d.clearRect(0, 0, width, height);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        float wrapping_width = width;
        float y = 0;
        int length = str.length();
        while (measurer.getPosition() < length) {
            TextLayout layout = measurer.nextLayout(wrapping_width);
            y += (layout.getAscent());
            float x = layout.isLeftToRight() ? 0 : (wrapping_width - layout.getAdvance());

            layout.draw(g2d, x, y);
            y += layout.getDescent() + layout.getLeading();
        }
        image.getRaster().getDataElements(0, 0, image.getWidth(), image.getHeight(), pixels);
    }

    private static final void initGL() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();

        GLU.gluOrtho2D(
                0f, Display.getDisplayMode().getWidth(), 0f, Display.getDisplayMode().getHeight());
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
    }

    private static final void updateTexture(int width, int height, IntBuffer pixel_data) {
        GL11.glTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                0,
                0,
                width,
                height,
                GL11.GL_RGBA,
                GL12.GL_UNSIGNED_INT_8_8_8_8,
                pixel_data);
    }

    private static final int createTexture(int width, int height, IntBuffer pixel_data) {
        IntBuffer handle_buffer = BufferUtils.createIntBuffer(1);
        GL11.glGenTextures(handle_buffer);
        int tex_handle = handle_buffer.get(0);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex_handle);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA,
                width,
                height,
                0,
                GL11.GL_RGBA,
                GL12.GL_UNSIGNED_INT_8_8_8_8,
                pixel_data);

        return tex_handle;
    }
}
