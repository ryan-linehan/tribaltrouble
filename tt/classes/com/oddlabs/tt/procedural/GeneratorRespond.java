package com.oddlabs.tt.procedural;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.GLIntImage;

import org.lwjgl.opengl.GL11;

public final strictfp class GeneratorRespond extends TextureGenerator {
    private static final int COLOR = 0x80808080;

    public final Texture[] generate() {
        GLIntImage img = new GLIntImage(1, 1, GL11.GL_RGBA);
        img.putPixel(0, 0, COLOR);
        Texture[] textures = new Texture[1];
        textures[0] =
                new Texture(
                        new GLImage[] {img},
                        Globals.COMPRESSED_RGBA_FORMAT,
                        GL11.GL_NEAREST,
                        GL11.GL_NEAREST,
                        GL11.GL_REPEAT,
                        GL11.GL_REPEAT);
        return textures;
    }

    public final int hashCode() {
        return 1234;
    }
}
