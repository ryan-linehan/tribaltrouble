package com.oddlabs.tt.util;

import org.lwjgl.opengl.GL11;
import com.oddlabs.tt.render.PixelFormat;

import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.GLIntImage;

public final strictfp class OffscreenRendererFactory {

	public final OffscreenRenderer createRenderer(int width, int height, PixelFormat format, boolean use_copyteximage) {
		OffscreenRenderer renderer = doCreateRenderer(width, height, format, use_copyteximage);
System.out.println("Creating renderer = " + renderer);
		return renderer;
	}

	public final OffscreenRenderer doCreateRenderer(int width, int height, PixelFormat format, boolean use_copyteximage) {
		try {
			return new FramebufferTextureRenderer(width, height, format.getAlphaBits() > 0, use_copyteximage);
		} catch (Exception e) {
			System.out.println("Failed to create FramebufferRenderer: " + e);
		}
		return new BackBufferRenderer(width, height, use_copyteximage);
	}
}
