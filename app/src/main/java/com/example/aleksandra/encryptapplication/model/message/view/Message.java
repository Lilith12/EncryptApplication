package com.example.aleksandra.encryptapplication.model.message.view;

import android.graphics.Bitmap;

import java.io.Serializable;

/**
 * Created by Aleksandra on 2016-11-19.
 */

public class Message implements Serializable {

    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_LOG = 1;
    public static final int TYPE_ACTION = 2;

    private int mType;
    private Bitmap mImage;
    private String mMessage;
    private String mUsername;
    private long mId;
    private String mCodeMessage;
    private Boolean mIsEdited = false;

    private Message() {}

    public int getType() {
        return mType;
    }

    public String getMessage() {
        return mMessage;
    }

    public String getUsername() {
        return mUsername;
    }

    public Bitmap getImage() {
        return mImage;
    }

    public long getId() {
        return mId;
    }

    public String getCodeMessage() {
        return mCodeMessage;
    }

    public Boolean isEdited() {
        return mIsEdited;
    }


    public static class Builder {
        private long mId;
        private String mUsername;
        private String mMessage;
        private final int mType;
        private Bitmap mImage;
        private String mCodeMessage;
        private Boolean mIsEdited = false;

        public Builder(int type) {
            mType = type;
        }

        public Builder id(long id) {
            mId = id;
            return this;
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

        public Builder codeMessage(String codeMessage) {
            mCodeMessage = codeMessage;
            return this;
        }

        public Builder isEdited(boolean isEdited){
            mIsEdited = isEdited;
            return this;
        }

        public Message build() {
            Message message = new Message();
            message.mId = mId;
            message.mType = mType;
            message.mUsername = mUsername;
            message.mImage = mImage;
            message.mMessage = mMessage;
            message.mCodeMessage = mCodeMessage;
            message.mIsEdited = mIsEdited;
            return message;
        }
    }
}
