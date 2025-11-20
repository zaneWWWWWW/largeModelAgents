package com.example.projectv3.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.projectv3.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "chat.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_PREFIX = "messages_user_";
    private static final String PSYCH_TABLE_PREFIX = "psych_status_user_";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_IS_AI = "is_ai";
    private final String tableName;
    private final String psychTableName;

    public ChatDbHelper(Context context, long userId) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.tableName = userId > 0 ? TABLE_PREFIX + userId : TABLE_PREFIX + "guest";
        this.psychTableName = userId > 0 ? PSYCH_TABLE_PREFIX + userId : PSYCH_TABLE_PREFIX + "guest";
    }

    public ChatDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.tableName = TABLE_PREFIX + "guest";
        this.psychTableName = PSYCH_TABLE_PREFIX + "guest";
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createMsgTable = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                COLUMN_IS_AI + " INTEGER)";
        db.execSQL(createMsgTable);

        String createPsychTable = "CREATE TABLE IF NOT EXISTS " + psychTableName + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "result TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER)";
        db.execSQL(createPsychTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        db.execSQL("DROP TABLE IF EXISTS " + psychTableName);
        onCreate(db);
    }

    public long insertMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                COLUMN_IS_AI + " INTEGER)");
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONTENT, message.getContent());
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());
        values.put(COLUMN_IS_AI, message.isAi() ? 1 : 0);
        long id = db.insert(tableName, null, values);
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
            tableName, 
            values, 
            COLUMN_ID + " = ?", 
            new String[] { String.valueOf(message.getId()) }
        );
        
        return rowsAffected > 0;
    }

    public List<Message> getAllMessages() {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                COLUMN_IS_AI + " INTEGER)");
        String selectQuery = "SELECT * FROM " + tableName + " ORDER BY " + COLUMN_TIMESTAMP + " ASC";
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
        db.execSQL("CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                COLUMN_IS_AI + " INTEGER)");
        String selectQuery = "SELECT * FROM " + tableName + " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT " + limit;
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
        db.execSQL("CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                COLUMN_IS_AI + " INTEGER)");
        String selectQuery = "SELECT * FROM " + tableName + 
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
        db.execSQL("CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                COLUMN_IS_AI + " INTEGER)");
        return db.delete(tableName, null, null);
    }

    public long insertPsychStatus(String resultJson, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + psychTableName + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "result TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER)");
        ContentValues values = new ContentValues();
        values.put("result", resultJson);
        values.put(COLUMN_TIMESTAMP, timestamp);
        return db.insert(psychTableName, null, values);
    }

    public List<java.util.Map<String, Object>> getPsychStatusHistory() {
        List<java.util.Map<String, Object>> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + psychTableName + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "result TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER)");
        String selectQuery = "SELECT result, " + COLUMN_TIMESTAMP + " FROM " + psychTableName + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                java.util.Map<String, Object> record = new java.util.HashMap<>();
                int tsIndex = cursor.getColumnIndex(COLUMN_TIMESTAMP);
                int resIndex = cursor.getColumnIndex("result");
                if (tsIndex >= 0) {
                    record.put("timestamp", cursor.getLong(tsIndex));
                }
                if (resIndex >= 0) {
                    record.put("result", cursor.getString(resIndex));
                }
                if (!record.isEmpty()) {
                    history.add(record);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return history;
    }

    public int deleteAllPsychStatus() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS " + psychTableName + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "result TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER)");
        return db.delete(psychTableName, null, null);
    }
}
