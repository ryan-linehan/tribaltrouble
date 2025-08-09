package com.oddlabs.http;

final strictfp class OkResponse implements HttpResponse {
    private final Object result;

    OkResponse(Object result) {
        this.result = result;
    }

    public final void notify(HttpCallback callback) {
        callback.success(result);
    }
}
