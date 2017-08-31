package com.example.aleksandra.encryptapplication;

import android.graphics.Bitmap;

/**
 * Created by Aleksandra on 2016-11-19.
 */

public class Message {

    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_LOG = 1;
    public static final int TYPE_ACTION = 2;

    private int mType;
    private Bitmap mImage;
    private String mMessage;
    private String mUsername;

    private Message() {}

    public int getType() {
        return mType;
    };

    public String getMessage() {
        return mMessage;
    };

    public String getUsername() {
        return mUsername;
    };

    public Bitmap getImage() {
        return mImage;
    };



    public static class Builder {
        private final int mType;
        private Bitmap mImage;
        private String mUsername;
        private String mMessage;

        public Builder(int type) {
            mType = type;
        }

        public Builder username(String username) {
            mUsername = username;
            return this;
        }

        public Builder message(String message) {
            mMessage = message;
            return this;
        }

        public Builder image(Bitmap image) {
            mImage = image;
            return this;
        }

        public Message build() {
            Message message = new Message();
            message.mType = mType;
            message.mUsername = mUsername;
            message.mImage = mImage;
            message.mMessage = mMessage;
            return message;
        }
    }
}
