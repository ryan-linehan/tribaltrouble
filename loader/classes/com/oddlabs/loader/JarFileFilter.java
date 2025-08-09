package com.oddlabs.loader;

import java.io.File;
import java.io.FileFilter;

public final strictfp class JarFileFilter implements FileFilter {
    public final boolean accept(File file) {
        return file.isFile() && file.getName().endsWith(".jar.svn-base");
    }
}
