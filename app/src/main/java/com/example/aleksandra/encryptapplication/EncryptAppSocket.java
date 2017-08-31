package com.example.aleksandra.encryptapplication;

import android.app.Application;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Created by Aleksandra on 2016-11-14.
 */
public class EncryptAppSocket extends Application {

    final private String hostname = "http://ec2-35-163-196-156.us-west-2.compute.amazonaws.com:8085";
    final private String localhostHostname = "http://192.168.0.103:8085";

    private Socket mSocket;
    private String username;

    {
        try {
            mSocket = IO.socket(hostname);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return mSocket;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
