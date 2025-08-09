package com.oddlabs.tt.vbo;

import com.oddlabs.tt.util.Utils;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final strictfp class FloatVBO extends VBO {
    private FloatBuffer saved_buffer = null;

    //	private FloatBuffer mapped_buffer = null;

    public FloatVBO(int usage, int size) {
        super(GL15.GL_ARRAY_BUFFER, usage, size << 2);
        ByteBuffer buffer = getSavedBuffer();
        if (buffer != null) saved_buffer = buffer.asFloatBuffer();
    }

    public FloatVBO(int usage, FloatBuffer initial_data) {
        this(usage, initial_data.remaining());
        put(initial_data);
    }

    public FloatVBO(int usage, float[] initial_data) {
        this(usage, initial_data.length);
        put(initial_data);
    }

    /*	public final void map(int access) {
    		if (!doMap(access))
    			saved_buffer = getMappedBuffer().asFloatBuffer();
    		mapped_buffer = saved_buffer;
    	}

    	public final boolean unmap() {
    		saved_buffer.clear();
    		mapped_buffer = null;
    		return doUnmap();
    	}

    	public final FloatBuffer buffer() {
    		return mapped_buffer;
    	}
    */
    public final void vertexPointer(int size, int stride, int index) {
        if (!use_vbo) {
            saved_buffer.position(index);
            GL11.glVertexPointer(size, GL11.GL_FLOAT, stride, saved_buffer);
        } else {
            makeCurrent();
            GL11.glVertexPointer(size, GL11.GL_FLOAT, stride, index << 2);
        }
    }

    public final void texCoordPointer(int size, int stride, int index) {
        if (!use_vbo) {
            saved_buffer.position(index);
            GL11.glTexCoordPointer(size, GL11.GL_FLOAT, stride, saved_buffer);
        } else {
            makeCurrent();
            GL11.glTexCoordPointer(size, GL11.GL_FLOAT, stride, index << 2);
        }
    }

    public final void normalPointer(int stride, int index) {
        if (!use_vbo) {
            saved_buffer.position(index);
            GL11.glNormalPointer(GL11.GL_FLOAT, stride, saved_buffer);
        } else {
            makeCurrent();
            GL11.glNormalPointer(GL11.GL_FLOAT, stride, index << 2);
        }
    }

    public final void colorPointer(int size, int stride, int index) {
        if (!use_vbo) {
            saved_buffer.position(index);
            GL11.glColorPointer(size, GL11.GL_FLOAT, stride, saved_buffer);
        } else {
            makeCurrent();
            GL11.glColorPointer(size, GL11.GL_FLOAT, stride, index << 2);
        }
    }

    public final void put(FloatBuffer buffer) {
        putSubData(0, buffer);
    }

    public final void put(float[] buffer) {
        putSubData(0, Utils.toBuffer(buffer));
        //		do {
        //			map(GL15.GL_WRITE_ONLY);
        //			buffer().put(buffer);
        //		} while (!unmap());
    }

    public final void putSubData(int index, FloatBuffer buffer) {
        if (!use_vbo) {
            saved_buffer.position(index);
            saved_buffer.put(buffer);
        } else {
            makeCurrent();
            GL15.glBufferSubData(getTarget(), index << 2, buffer);
            buffer.position(buffer.limit());
        }
    }

    public final int capacity() {
        return getSize() >> 2;
    }
}
