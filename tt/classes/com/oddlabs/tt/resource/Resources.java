package com.oddlabs.tt.resource;

import java.util.HashMap;
import java.util.Map;

public final strictfp class Resources {
    private static final Map loaded_resources = new HashMap();

    public static final Object findResource(ResourceDescriptor resdesc) {
        Object result = loaded_resources.get(resdesc);
        if (result == null) {
            result = resdesc.newInstance();
            loaded_resources.put(resdesc, result);
        }
        return result;
    }
}
