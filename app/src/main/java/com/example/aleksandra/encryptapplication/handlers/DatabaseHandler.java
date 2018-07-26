package com.example.aleksandra.encryptapplication.handlers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.aleksandra.encryptapplication.model.message.view.Message;
import com.example.aleksandra.encryptapplication.model.message.websocket.PrivateMessageModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aleksandra on 2018-01-15.
 */
public class DatabaseHandler extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "messagesManager";

    // Contacts table name
    private static final String TABLE_MESSAGES = "messages";

    // Contacts Table Columns names
    private static final String KEY_ID = "_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_MESSAGE_CODE = "message_code";
    private static final String KEY_CONVERSATION_CODE = "conversation_code";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static DatabaseHandler databaseHandler;

    private DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static DatabaseHandler getDatabaseHandler(Context context){
        if(databaseHandler == null){
            databaseHandler = new DatabaseHandler(context);
        }
        return databaseHandler;
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MESSAGE_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_USERNAME + " TEXT,"
                + KEY_MESSAGE + " TEXT," + KEY_MESSAGE_CODE + " TEXT," + KEY_CONVERSATION_CODE + " TEXT,"
                + KEY_TIMESTAMP +" DATETIME DEFAULT CURRENT_TIMESTAMP" + ");";
        db.execSQL(CREATE_MESSAGE_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        // Drop older table if existed
//        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
//
//        // Create tables again
//        onCreate(db);
    }

    public long addRow(PrivateMessageModel message, String conversationCode) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, message.getUsername());
        values.put(KEY_MESSAGE, message.getMessage());
        values.put(KEY_MESSAGE_CODE, message.getMessageCode());
        values.put(KEY_CONVERSATION_CODE, conversationCode);
        // Inserting Row
        long id = db.insert(TABLE_MESSAGES, null, values);
        db.close(); // Closing database connection
        return id;
    }

    public List<Message> getMessagesFromConversation(String conversationCode) {
        List<Message> messageList = new ArrayList<>();

        // Select All Query
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + KEY_CONVERSATION_CODE + " = '"+conversationCode+"' ORDER BY " + KEY_TIMESTAMP + " ASC";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        int id;
        String username;
        String messageString;
        Message message;
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                id = Integer.parseInt(cursor.getString(0));
                username = cursor.getString(1);
                messageString = cursor.getString(2);
                message = new Message.Builder(Message.TYPE_MESSAGE).id(id).username(
                        username).message(messageString).build();
                // Adding contact to list
                messageList.add(message);
                cursor.moveToNext();
            }
        }
        cursor.close();
        return messageList;
    }

    public Message getMessageByUUID(String uuid){
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + KEY_MESSAGE_CODE + " = '"+uuid+"'";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
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
        return message;
    }

    public int updateMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_MESSAGE, message.getMessage());

        // updating row
        return db.update(TABLE_MESSAGES, values, KEY_ID + " = ?",
                new String[] { String.valueOf(message.getId()) });
    }

    public void deleteMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, KEY_ID + " = ?",
                new String[]{String.valueOf(message.getId())});
        db.close();
    }
}