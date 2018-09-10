package com.example.aleksandra.encryptapplication.model.message.websocket;

import com.example.aleksandra.encryptapplication.encrypt.RSA;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Data;

@Data
public class MessageModel {
    private String username;
    private String message;
    private boolean wasEdited;
    private Integer position;
    private String messageCode;
    private String roomName;
    private boolean isImage;

    public MessageModel(JSONObject data) throws JSONException {
        this(data.getString("username"), data.getString("message"), data.getString("wasEdited"),
                data.getString("position"), data.getString("messageCode"), data.getString("isImage"));
    }

    private MessageModel(String username, String message, String wasEdited, String position,
            String messageCode, String isImage) {
        this.username = username;
        this.message = message;
        this.wasEdited = Boolean.parseBoolean(wasEdited);
        this.position = "null".equals(position) ? null : Integer.parseInt(
                position);
        this.messageCode = messageCode;
        this.isImage = Boolean.parseBoolean(isImage);
    }

    public MessageModel(String username, String message, boolean wasEdited, Integer position,
            String messageCode, boolean isImage) {
        this.username = username;
        this.message = message;
        this.wasEdited = wasEdited;
        this.position = position;
        this.messageCode = messageCode;
        this.isImage = isImage;
    }

    public String decryptMessage() {
        return RSA.getRSAInstance().decrypt(message);
    }
}
