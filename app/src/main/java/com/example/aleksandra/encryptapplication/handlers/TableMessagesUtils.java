package com.example.aleksandra.encryptapplication.handlers;

import com.example.aleksandra.encryptapplication.model.message.view.Message;
import com.example.aleksandra.encryptapplication.model.message.websocket.PrivateMessageModel;

public class TableMessagesUtils {
    private TableMessagesUtils(){

    }

    public static long addToDatabase(PrivateMessageModel model, DatabaseHandler db) {
        long id;
        if (model.isWasEdited() && model.getPosition() != null) {
            Message editedMessage = db.getMessageByUUID(model.getMessageCode());
            if (editedMessage != null) {
                db.updateMessage(editedMessage);
            }
            id = editedMessage.getId();
        } else {
            id = db.addRow(model, model.getUsername());
        }
        return id;
    }
}
