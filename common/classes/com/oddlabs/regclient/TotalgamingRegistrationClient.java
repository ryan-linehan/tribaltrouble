package com.oddlabs.regclient;

import com.oddlabs.http.HttpRequestParameters;
import com.oddlabs.net.TaskThread;
import com.oddlabs.util.WindowsRegistryInterface;

import java.io.File;

public final strictfp class TotalgamingRegistrationClient extends RegistrationClient {

    public TotalgamingRegistrationClient(
            TaskThread task_thread, File registration_file, HttpRequestParameters parameters) {
        super(task_thread, registration_file, parameters, CLIENT_TYPE_OFFLINE);
        String reg_key;
        try {
            reg_key =
                    WindowsRegistryInterface.queryRegistrationKey(
                            "HKEY_LOCAL_MACHINE",
                            "SOFTWARE\\Stardock\\ComponentManager\\Drengin.net\\trib",
                            "Serial No");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            reg_key = null;
            System.out.println("Failed to read key from reg db, e = " + e);
        }
        reg_key = (String) task_thread.getDeterministic().log(reg_key);
        if (reg_key != null) setKey(reg_key);
    }
}
