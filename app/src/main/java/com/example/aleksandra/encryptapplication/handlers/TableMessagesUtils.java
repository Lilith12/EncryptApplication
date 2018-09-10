package com.example.aleksandra.encryptapplication.handlers;

import com.example.aleksandra.encryptapplication.model.message.view.Message;
import com.example.aleksandra.encryptapplication.model.message.websocket.MessageModel;

import java.util.Optional;

public class TableMessagesUtils {
    private TableMessagesUtils(){

    }

    public static void addToDatabase(MessageModel model, DatabaseHandler db) {
        long id;
        if (model.isWasEdited() && model.getPosition() != null) {
            Message editedMessage = db.getMessageByUUID(model.getMessageCode());
            if (editedMessage != null) {
                db.updateMessage(editedMessage);
            }
        } else {
            id = db.addRow(model, Optional.ofNullable(model.getRoomName()).orElse(model.getUsername()));
        }
    }
}
