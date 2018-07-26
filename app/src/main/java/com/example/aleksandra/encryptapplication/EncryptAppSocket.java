package com.example.aleksandra.encryptapplication;

import android.app.Application;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Created by Aleksandra on 2016-11-14.
 */
public class EncryptAppSocket extends Application {

    private final String hostname = "http://ec2-18-130-182-33.eu-west-2.compute.amazonaws.com:8085";
    private final String localhostHostname = "http://192.168.1.11:8085"; //netstat -r 0.0.0.0

    private String username;
    private Socket mSocket;

    public EncryptAppSocket(){ }

    public Socket getSocket() {
        if (mSocket == null) {
            try {
                mSocket = IO.socket(hostname);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return mSocket;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
