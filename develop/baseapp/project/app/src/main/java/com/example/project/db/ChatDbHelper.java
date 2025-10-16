package com.example.project.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.project.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "chat.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_MESSAGES = "messages";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_IS_AI = "is_ai";

    public ChatDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_MESSAGES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                COLUMN_IS_AI + " INTEGER)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }

    public long insertMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONTENT, message.getContent());
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());
        values.put(COLUMN_IS_AI, message.isAi() ? 1 : 0);
        long id = db.insert(TABLE_MESSAGES, null, values);
        message.setId(id);
        return id;
    }

    public boolean updateMessage(Message message) {
        if (message.getId() <= 0) {
            return false; // 无效的ID
        }
        
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONTENT, message.getContent());
        
        // 更新消息内容
        int rowsAffected = db.update(
            TABLE_MESSAGES, 
            values, 
            COLUMN_ID + " = ?", 
            new String[] { String.valueOf(message.getId()) }
        );
        
        return rowsAffected > 0;
    }

    public List<Message> getAllMessages() {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " ORDER BY " + COLUMN_TIMESTAMP + " ASC";
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Message message = new Message();
                message.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
                message.setContent(cursor.getString(cursor.getColumnIndex(COLUMN_CONTENT)));
                message.setTimestamp(cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP)));
                message.setAi(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_AI)) == 1);
                messages.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return messages;
    }
    
    /**
     * 获取最近的指定数量消息
     * @param limit 获取的消息数量限制
     * @return 最近的消息列表（按时间升序）
     */
    public List<Message> getRecentMessages(int limit) {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT " + limit;
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Message message = new Message();
                message.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
                message.setContent(cursor.getString(cursor.getColumnIndex(COLUMN_CONTENT)));
                message.setTimestamp(cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP)));
                message.setAi(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_AI)) == 1);
                messages.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        // 反转列表，使其按时间升序排列
        List<Message> reversedMessages = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            reversedMessages.add(messages.get(i));
        }
        
        return reversedMessages;
    }
    
    /**
     * 获取最近的指定数量用户消息（不包括AI回复）
     * @param limit 获取的用户消息数量限制
     * @return 最近的用户消息列表（按时间升序）
     */
    public List<Message> getRecentUserMessages(int limit) {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + 
                            " WHERE " + COLUMN_IS_AI + " = 0 " +
                            " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT " + limit;
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Message message = new Message();
                message.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
                message.setContent(cursor.getString(cursor.getColumnIndex(COLUMN_CONTENT)));
                message.setTimestamp(cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP)));
                message.setAi(false); // 这里一定是用户消息
                messages.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        // 反转列表，使其按时间升序排列
        List<Message> reversedMessages = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            reversedMessages.add(messages.get(i));
        }
        
        return reversedMessages;
    }
    
    /**
     * 删除数据库中所有聊天消息
     * @return 删除的消息数量
     */
    public int deleteAllMessages() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_MESSAGES, null, null);
    }
}