package com.example.projectv3.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.projectv3.model.Guide;

import java.util.ArrayList;
import java.util.List;

public class GuideDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "guide.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_GUIDES = "guides";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_IS_DEFAULT = "is_default";
    private static final String COLUMN_IS_COMPLETED = "is_completed";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    public GuideDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_GUIDES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_IS_DEFAULT + " INTEGER, " +
                COLUMN_IS_COMPLETED + " INTEGER, " +
                COLUMN_USER_ID + " INTEGER, " +
                COLUMN_TIMESTAMP + " INTEGER)";
        db.execSQL(createTable);
        
        // 插入默认指南
        insertDefaultGuides(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // v2 升级：仅替换系统默认的五条建议，不影响用户自定义内容
        if (oldVersion < 2) {
            db.delete(TABLE_GUIDES, COLUMN_USER_ID + " = 0 AND " + COLUMN_IS_DEFAULT + " = 1", null);
            insertDefaultGuides(db);
        }
    }
    
    /**
     * 插入默认的5条治疗方案
     */
    private void insertDefaultGuides(SQLiteDatabase db) {
        // 固定五条建议基于 case.txt：新学校适应、社交紧张、孤独与害怕被拒绝
        String[] defaultGuides = {
            "社交小步走：每天主动与同桌或一位同学打招呼，并进行2-3分钟交流，逐步降低社交紧张",
            "认知重建练习：记录‘被拒绝’相关的自动化想法，用真实证据进行反驳，写出更平衡的替代观点",
            "正念与呼吸：每天10分钟腹式呼吸+正念观察情绪波动，提升对紧张与自我怀疑的调节能力",
            "渐进式适应计划：列出5件你能掌控的校园小事（如选座、提前准备话题），每天完成1件以增强自我效能",
            "连接支持系统：每周与一位旧友或家人通话/聊天，分享近况，维持情感支持、减轻孤独感"
        };
        
        for (String content : defaultGuides) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_CONTENT, content);
            values.put(COLUMN_IS_DEFAULT, 1); // 1表示默认治疗方案
            values.put(COLUMN_IS_COMPLETED, 0); // 0表示未完成
            values.put(COLUMN_USER_ID, 0); // 0表示系统默认治疗方案
            values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
            db.insert(TABLE_GUIDES, null, values);
        }
    }

    /**
     * 插入新治疗方案
     */
    public long insertGuide(Guide guide) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONTENT, guide.getContent());
        values.put(COLUMN_IS_DEFAULT, guide.isDefault() ? 1 : 0);
        values.put(COLUMN_IS_COMPLETED, guide.isCompleted() ? 1 : 0);
        values.put(COLUMN_USER_ID, guide.getUserId());
        values.put(COLUMN_TIMESTAMP, guide.getTimestamp());
        long id = db.insert(TABLE_GUIDES, null, values);
        guide.setId(id);
        return id;
    }

    /**
     * 更新治疗方案完成状态
     */
    public boolean updateGuideCompletionStatus(long guideId, boolean isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_COMPLETED, isCompleted ? 1 : 0);
        
        int rowsAffected = db.update(
            TABLE_GUIDES, 
            values, 
            COLUMN_ID + " = ?", 
            new String[] { String.valueOf(guideId) }
        );
        
        return rowsAffected > 0;
    }

    /**
     * 获取用户的所有治疗方案（包括系统默认治疗方案和用户自定义治疗方案）
     */
    public List<Guide> getGuidesForUser(long userId) {
        List<Guide> guides = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // 查询条件：系统默认指南(user_id=0)或当前用户的自定义指南
        String selectQuery = "SELECT * FROM " + TABLE_GUIDES + 
                " WHERE " + COLUMN_USER_ID + " = 0 OR " + 
                COLUMN_USER_ID + " = ? ORDER BY " + 
                COLUMN_IS_DEFAULT + " DESC, " + 
                COLUMN_TIMESTAMP + " ASC";
                
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                Guide guide = new Guide();
                guide.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
                guide.setContent(cursor.getString(cursor.getColumnIndex(COLUMN_CONTENT)));
                guide.setDefault(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_DEFAULT)) == 1);
                guide.setCompleted(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_COMPLETED)) == 1);
                guide.setUserId(cursor.getLong(cursor.getColumnIndex(COLUMN_USER_ID)));
                guide.setTimestamp(cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP)));
                guides.add(guide);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return guides;
    }
    
    /**
     * 删除用户自定义指南
     */
    public boolean deleteUserGuide(long guideId, long userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // 只能删除自己的自定义指南，不能删除系统默认指南
        return db.delete(TABLE_GUIDES, 
                COLUMN_ID + " = ? AND " + 
                COLUMN_USER_ID + " = ? AND " + 
                COLUMN_IS_DEFAULT + " = 0", 
                new String[] { String.valueOf(guideId), String.valueOf(userId) }) > 0;
    }
}