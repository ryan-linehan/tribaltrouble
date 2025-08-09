package com.oddlabs.http;

import java.io.IOException;
import java.io.InputStream;

public strictfp interface HttpResponseParser {
    Object parse(InputStream in) throws IOException;
}
