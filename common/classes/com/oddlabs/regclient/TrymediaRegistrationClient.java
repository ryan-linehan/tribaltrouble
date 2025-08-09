package com.oddlabs.regclient;

import com.oddlabs.http.HttpRequestParameters;
import com.oddlabs.net.TaskThread;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public final strictfp class TrymediaRegistrationClient extends RegistrationClient {
    public TrymediaRegistrationClient(
            TaskThread task_thread,
            File registration_file,
            HttpRequestParameters parameters,
            int client_type) {
        super(task_thread, registration_file, parameters, client_type);

        File file = new File("game.ini");
        String key;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            try {
                key = br.readLine();
            } finally {
                br.close();
            }
        } catch (IOException e) {
            System.out.println("Exception: " + e);
            key = null;
            // ignore
        }
        key = (String) task_thread.getDeterministic().log(key);
        if (key != null) setKey(key);
    }
}
