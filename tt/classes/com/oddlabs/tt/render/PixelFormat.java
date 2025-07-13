package com.oddlabs.tt.render;

public final strictfp class PixelFormat {
    private int redBits = 8;
    private int greenBits = 8;
    private int blueBits = 8;
    private int alphaBits = 8;
    private int depthBits = 24;
    private int stencilBits = 8;
    private int samples = 0;
    private boolean doubleBuffered = true;
    private boolean stereo = false;

    public PixelFormat() {
    }

    public PixelFormat(int alpha, int depth, int stencil) {
        this.alphaBits = alpha;
        this.depthBits = depth;
        this.stencilBits = stencil;
    }

    public PixelFormat(int alpha, int depth, int stencil, int samples) {
        this.alphaBits = alpha;
        this.depthBits = depth;
        this.stencilBits = stencil;
        this.samples = samples;
    }

    public PixelFormat(int red, int green, int blue, int alpha, int depth) {
        this.redBits = red;
        this.greenBits = green;
        this.blueBits = blue;
        this.alphaBits = alpha;
        this.depthBits = depth;
    }

    public PixelFormat withRedBits(int redBits) {
        this.redBits = redBits;
        return this;
    }

    public PixelFormat withGreenBits(int greenBits) {
        this.greenBits = greenBits;
        return this;
    }

    public PixelFormat withBlueBits(int blueBits) {
        this.blueBits = blueBits;
        return this;
    }

    public PixelFormat withAlphaBits(int alphaBits) {
        this.alphaBits = alphaBits;
        return this;
    }

    public PixelFormat withDepthBits(int depthBits) {
        this.depthBits = depthBits;
        return this;
    }

    public PixelFormat withStencilBits(int stencilBits) {
        this.stencilBits = stencilBits;
        return this;
    }

    public PixelFormat withSamples(int samples) {
        this.samples = samples;
        return this;
    }

    public PixelFormat withDoubleBuffered(boolean doubleBuffered) {
        this.doubleBuffered = doubleBuffered;
        return this;
    }

    public PixelFormat withStereo(boolean stereo) {
        this.stereo = stereo;
        return this;
    }

    // Getters for compatibility
    public int getRedBits() {
        return redBits;
    }

    public int getGreenBits() {
        return greenBits;
    }

    public int getBlueBits() {
        return blueBits;
    }

    public int getAlphaBits() {
        return alphaBits;
    }

    public int getDepthBits() {
        return depthBits;
    }

    public int getStencilBits() {
        return stencilBits;
    }

    public int getSamples() {
        return samples;
    }

    public boolean isDoubleBuffered() {
        return doubleBuffered;
    }

    public boolean isStereo() {
        return stereo;
    }

    // Legacy method names for compatibility
    public PixelFormat setRedBits(int redBits) {
        return withRedBits(redBits);
    }

    public PixelFormat setGreenBits(int greenBits) {
        return withGreenBits(greenBits);
    }

    public PixelFormat setBlueBits(int blueBits) {
        return withBlueBits(blueBits);
    }

    public PixelFormat setAlphaBits(int alphaBits) {
        return withAlphaBits(alphaBits);
    }

    public PixelFormat setDepthBits(int depthBits) {
        return withDepthBits(depthBits);
    }

    public PixelFormat setStencilBits(int stencilBits) {
        return withStencilBits(stencilBits);
    }

    public PixelFormat setSamples(int samples) {
        return withSamples(samples);
    }

    public PixelFormat setDoubleBuffered(boolean doubleBuffered) {
        return withDoubleBuffered(doubleBuffered);
    }

    public PixelFormat setStereo(boolean stereo) {
        return withStereo(stereo);
    }
} 