package com.example.pp.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.pp.data.local.dao.BackgroundJobLogDao;
import com.example.pp.data.local.dao.CategoryDao;
import com.example.pp.data.local.dao.CommonDao;
import com.example.pp.data.local.dao.TagDao;
import com.example.pp.data.local.dao.TransactionDao;
import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.local.entity.CategoryEntity;
import com.example.pp.data.local.entity.TagEntity;
import com.example.pp.data.local.entity.TransactionEntity;
import com.example.pp.data.local.entity.TransactionTagCrossRef;

@Database(entities = {CategoryEntity.class, TagEntity.class, TransactionEntity.class, TransactionTagCrossRef.class, BackgroundJobLog.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract CategoryDao categoryDao();
    public abstract TagDao tagDao();
    public abstract TransactionDao transactionDao();
    public abstract CommonDao commonDao();
    public abstract BackgroundJobLogDao backgroundJobLogDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "personal_tracker_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}