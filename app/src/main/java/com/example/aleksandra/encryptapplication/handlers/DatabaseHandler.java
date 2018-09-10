package com.example.aleksandra.encryptapplication.handlers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.example.aleksandra.encryptapplication.encrypt.RSA;
import com.example.aleksandra.encryptapplication.model.message.view.Message;
import com.example.aleksandra.encryptapplication.model.message.websocket.MessageModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleksandra on 2018-01-15.
 */
public class DatabaseHandler extends SQLiteOpenHelper {


    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "messagesManager";
    private static final String TABLE_MESSAGES = "messages";
    private static final String KEY_ID = "_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_MESSAGE_CODE = "message_code";
    private static final String KEY_CONVERSATION_CODE = "conversation_code";
    private static final String KEY_IMAGE = "is_image";
    private static final String KEY_TIMESTAMP = "timestamp";
    public static final String CREATE_MESSAGE_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
            + KEY_ID + " INTEGER PRIMARY KEY," + KEY_USERNAME + " TEXT,"
            + KEY_MESSAGE + " TEXT," + KEY_MESSAGE_CODE + " TEXT," + KEY_CONVERSATION_CODE
            + " TEXT," + KEY_IMAGE + " INTEGER," + KEY_TIMESTAMP
            + " DATETIME DEFAULT CURRENT_TIMESTAMP" + ");";
    private static DatabaseHandler databaseHandler;

    private DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static DatabaseHandler getDatabaseHandler(Context context) {
        if (databaseHandler == null) {
            databaseHandler = new DatabaseHandler(context);
        }
        return databaseHandler;
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DatabaseHandler.CREATE_MESSAGE_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);

        onCreate(db);
    }

    public synchronized long addRow(MessageModel message, String conversationCode) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, message.getUsername());
        values.put(KEY_MESSAGE, message.getMessage());
        values.put(KEY_MESSAGE_CODE, message.getMessageCode());
        values.put(KEY_CONVERSATION_CODE, conversationCode);
        values.put(KEY_IMAGE, message.isImage() ? 1 : 0);
        long id = db.insert(TABLE_MESSAGES, null, values);
        db.close();
        return id;
    }

    public List<Message> getMessagesFromConversation(String conversationCode) {
        List<Message> messageList = new ArrayList<>();

        String selectQuery =
                "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + KEY_CONVERSATION_CODE + " = '"
                        + conversationCode + "' ORDER BY " + KEY_TIMESTAMP + " ASC";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        int id;
        String username;
        String messageString;
        boolean isImage;
        Message message;
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                id = Integer.parseInt(cursor.getString(0));
                username = cursor.getString(1);
                messageString = cursor.getString(2);
                isImage = cursor.getInt(5) != 0;
                Message.Builder messageBuilder = new Message.Builder(Message.TYPE_MESSAGE).id(id)
                        .username(username);
                if (isImage) {
                    byte[] decode = Base64.decode(decryptMessage(messageString), Base64.DEFAULT);
                    messageBuilder.image(BitmapFactory.decodeByteArray(decode, 0, decode.length));
                } else {
                    messageBuilder.message(decryptMessage(messageString));
                }
                message = messageBuilder.build();
                messageList.add(message);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return messageList;
    }

    private String decryptMessage(String message) {
        return RSA.getRSAInstance().decrypt(message);
    }

    public Message getMessageByUUID(String uuid) {
        String selectQuery =
                "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + KEY_MESSAGE_CODE + " = '" + uuid
                        + "'";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        int id;
        String username;
        String messageString;
        String messageCode;
        Message message = null;
        if (cursor.moveToFirst()) {
            id = Integer.parseInt(cursor.getString(0));
            username = cursor.getString(1);
            messageString = cursor.getString(2);
            messageCode = cursor.getString(3);
            message = new Message.Builder(Message.TYPE_MESSAGE).id(id).username(username).message(
                    messageString).codeMessage(messageCode).build();
        }
        cursor.close();
        return message;
    }

    public int updateMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_MESSAGE, message.getMessage());

        // updating row
        return db.update(TABLE_MESSAGES, values, KEY_ID + " = ?",
                new String[]{String.valueOf(message.getId())});
    }

    public void deleteMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, KEY_ID + " = ?",
                new String[]{String.valueOf(message.getId())});
        db.close();
    }

    public void dropTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
    }

    public void recreateTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(DatabaseHandler.CREATE_MESSAGE_TABLE);
    }
}