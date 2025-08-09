package com.oddlabs.regclient;

import com.oddlabs.net.TaskThread;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

public final strictfp class ReflexiveRegistrationClient extends RegistrationClient {
    private final boolean registered;

    public ReflexiveRegistrationClient(
            TaskThread task_thread, int appid, String channelid, String appname, String price) {
        super(task_thread);
        File result = new File("return.txt");
        result.delete();
        Random r = new Random();
        int session_id = r.nextInt(1000000);
        boolean registered_temp = false;
        try {
            Runtime.getRuntime()
                    .exec(
                            new String[] {
                                "ReflexiveArcade.exe",
                                "/appid",
                                "" + appid,
                                "/channelid",
                                channelid,
                                "/appname",
                                appname,
                                "/price",
                                price,
                                "/sessionid",
                                "" + session_id,
                                "/cmd",
                                "ispurchased"
                            });
            do {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    System.out.println("Exception: " + e);
                    // ignore
                }
            } while (!result.exists());
            BufferedReader br = new BufferedReader(new FileReader(result));
            try {
                String line = br.readLine();
                try {
                    long number = Long.parseLong(line);

                    if (number == (appid * 13 + session_id) / 7) registered_temp = true;
                } catch (NumberFormatException e) {
                    System.out.println("Exception: " + e);
                    // ignore
                }
            } finally {
                br.close();
            }
        } catch (IOException e) {
            System.out.println("Exception: " + e);
            // ignore;
        }
        registered = registered_temp;
    }

    public final boolean isRegistered() {
        return registered;
    }
}
