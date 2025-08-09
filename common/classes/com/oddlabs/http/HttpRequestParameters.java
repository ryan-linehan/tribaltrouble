package com.oddlabs.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

public final strictfp class HttpRequestParameters {
    final String url;
    public final Map parameters;

    public HttpRequestParameters(String url, Map parameters) {
        this.url = url;
        this.parameters = parameters;
    }

    String createQueryString() {
        if (parameters == null) return "";
        StringBuffer buffer = new StringBuffer();
        Iterator parameter_entries = parameters.entrySet().iterator();
        try {
            while (parameter_entries.hasNext()) {
                Map.Entry parameter = (Map.Entry) parameter_entries.next();
                buffer.append((String) parameter.getKey());
                buffer.append('=');
                buffer.append(URLEncoder.encode((String) parameter.getValue(), "UTF-8"));
                if (parameter_entries.hasNext()) buffer.append('&');
            }
            return buffer.toString();
        } catch (UnsupportedEncodingException e) {
            System.out.println("Exception: " + e);
            throw new RuntimeException(e);
        }
    }
}
