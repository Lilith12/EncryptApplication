package com.example.aleksandra.encryptapplication;

import android.app.Application;

import io.socket.client.Socket;

/**
 * Created by Aleksandra on 2016-11-14.
 */
public class EncryptAppSocket extends Application {

    private String username;

    public EncryptAppSocket(){
        // Class with application needs public constructor
    }

    public Socket getSocket() {
        return SocketConnect.getSocket();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
