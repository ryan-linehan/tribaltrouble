package com.oddlabs.regclient;

import com.oddlabs.http.*;
import com.oddlabs.net.Task;
import com.oddlabs.net.TaskThread;
import com.oddlabs.util.DeterministicSerializer;
import com.oddlabs.util.DeterministicSerializerLoopbackInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

final strictfp class RegistrationHttpClient {
    private static RegistrationHttpClient current_client;

    private final Task task;

    public static final RegistrationHttpClient register(
            TaskThread task_thread,
            HttpRequestParameters parameters,
            RegistrationListener listener,
            File registration_file) {
        if (current_client != null) current_client.close();
        current_client =
                new RegistrationHttpClient(task_thread, listener, parameters, registration_file);
        return current_client;
    }

    private RegistrationHttpClient(
            final TaskThread task_thread,
            final RegistrationListener listener,
            HttpRequestParameters parameters,
            final File registration_file) {
        this.task =
                HttpRequest.doGet(
                        task_thread,
                        parameters,
                        new HttpResponseParser() {
                            public final Object parse(InputStream in) throws IOException {
                                ObjectInputStream ois = new ObjectInputStream(in);
                                try {
                                    return ois.readObject();
                                } catch (ClassNotFoundException e) {
                                    System.out.println("Exception: " + e);
                                    throw new RuntimeException(e);
                                }
                            }
                        },
                        new DefaultHttpCallback() {
                            public final void success(Object reg_info_signed) {
                                DeterministicSerializer.save(
                                        task_thread.getDeterministic(),
                                        reg_info_signed,
                                        registration_file,
                                        new DeterministicSerializerLoopbackInterface() {
                                            public final void saveSucceeded() {
                                                close();
                                                listener.registrationCompleted();
                                            }

                                            public final void loadSucceeded(Object o) {
                                                // NOP
                                            }

                                            public final void failed(Exception e) {
                                                fail(e, listener);
                                            }
                                        });
                            }

                            public final void error(IOException e) {
                                fail(e, listener);
                            }
                        });
    }

    private void fail(Exception e, RegistrationListener listener) {
        close();
        listener.registrationFailed(0, e);
    }

    public final void close() {
        task.cancel();
    }
}
