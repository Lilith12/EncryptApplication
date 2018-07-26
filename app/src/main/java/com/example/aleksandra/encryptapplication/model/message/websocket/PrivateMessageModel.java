package com.example.aleksandra.encryptapplication.model.message.websocket;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Data;

@Data
public class PrivateMessageModel {
    private String username;
    private String message;
    private boolean wasEdited;
    private Integer position;
    private String messageCode;

    public PrivateMessageModel(JSONObject data) throws JSONException {
        this(data.getString("username"), data.getString("message"), data.getString("wasEdited"),
                data.getString("position"), data.getString("messageCode"));
    }

    public PrivateMessageModel(String username, String message, String wasEdited, String position,
            String messageCode) {
        this.username = username;
        this.message = message;
        this.wasEdited = Boolean.parseBoolean(wasEdited);
        this.position = "null".equals(position) ? null : Integer.parseInt(
                position);
        this.messageCode = messageCode;
    }

    public PrivateMessageModel(String username, String message, boolean wasEdited, Integer position,
            String messageCode) {
        this.username = username;
        this.message = message;
        this.wasEdited = wasEdited;
        this.position = position;
        this.messageCode = messageCode;
    }
}
