package com.oddlabs.tt.resource;

import java.util.ArrayList;
import java.util.List;

public abstract strictfp class NativeResource {
    private static final Object list_lock = new Object();
    private static final List finalized_resources = new ArrayList();
    private static int count;

    public NativeResource() {
        count++;
    }

    protected final void finalize() {
        synchronized (list_lock) {
            finalized_resources.add(this);
        }
    }

    public static final void deleteFinalized() {
        synchronized (list_lock) {
            for (int i = 0; i < finalized_resources.size(); i++) {
                NativeResource r = (NativeResource) finalized_resources.get(i);
                count--;
                r.doDelete();
            }
            finalized_resources.clear();
        }
    }

    public static final void gc() {
        System.gc();
        Runtime.getRuntime().runFinalization();
        deleteFinalized();
    }

    public static final int getCount() {
        return count;
    }

    protected abstract void doDelete();
}
