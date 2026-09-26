package com.example.pp.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.pp.data.local.dao.BackgroundJobLogDao;
import com.example.pp.data.local.dao.CategoryDao;
import com.example.pp.data.local.dao.CommonDao;
import com.example.pp.data.local.dao.GymDao;
import com.example.pp.data.local.dao.HistoryDao;
import com.example.pp.data.local.dao.HydrationDao;
import com.example.pp.data.local.dao.NutrientEntityDao;
import com.example.pp.data.local.dao.NutritionIntakeDao;
import com.example.pp.data.local.dao.QuotesDao;
import com.example.pp.data.local.dao.RegistryDao;
import com.example.pp.data.local.dao.ScreenTimeDao;
import com.example.pp.data.local.dao.SleepDao;
import com.example.pp.data.local.dao.TagDao;
import com.example.pp.data.local.dao.TaskDao;
import com.example.pp.data.local.dao.TransactionDao;
import com.example.pp.data.local.dao.VocabularyDao;
import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.local.entity.BodyWeight;
import com.example.pp.data.local.entity.CategoryEntity;
import com.example.pp.data.local.entity.GymTracking;
import com.example.pp.data.local.entity.HistoryApiTrack;
import com.example.pp.data.local.entity.HydrationIntake;
import com.example.pp.data.local.entity.NutrientEntity;
import com.example.pp.data.local.entity.NutritionIntake;
import com.example.pp.data.local.entity.QuotesApiTrack;
import com.example.pp.data.local.entity.RegistryEntity;
import com.example.pp.data.local.entity.SavedQuote;
import com.example.pp.data.local.entity.ScreenTime;
import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.data.local.entity.TagEntity;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;
import com.example.pp.data.local.entity.TransactionEntity;
import com.example.pp.data.local.entity.TransactionTagCrossRef;
import com.example.pp.data.local.entity.Vocabulary;
import com.example.pp.utils.TaskTypeConverters;

@Database(entities = {
    CategoryEntity.class, 
    TagEntity.class, 
    TransactionEntity.class, 
    TransactionTagCrossRef.class, 
    BackgroundJobLog.class,
    TaskDefinition.class,
    TaskState.class,
    HydrationIntake.class,
    NutritionIntake.class,
    NutrientEntity.class, 
    ScreenTime.class,
    SleepEntity.class,
    HistoryApiTrack.class,
    QuotesApiTrack.class,
    SavedQuote.class,
    BodyWeight.class,
    GymTracking.class,
    Vocabulary.class,
    RegistryEntity.class
}, version = 27, exportSchema = false)
@TypeConverters({TaskTypeConverters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract CategoryDao categoryDao();
    public abstract TagDao tagDao();
    public abstract TransactionDao transactionDao();
    public abstract CommonDao commonDao();
    public abstract BackgroundJobLogDao backgroundJobLogDao();
    public abstract TaskDao taskDao();
    public abstract HydrationDao hydrationDao();
    public abstract NutritionIntakeDao nutritionIntakeDao();
    public abstract NutrientEntityDao nutrientDao();
    public abstract ScreenTimeDao screenTimeDao();
    public abstract SleepDao sleepDao();
    public abstract HistoryDao historyDao();
    public abstract QuotesDao quotesDao();
    public abstract GymDao gymDao();
    public abstract VocabularyDao vocabularyDao();
    public abstract RegistryDao registryDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "personal_tracker_db")
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    createTriggers(db);
                                }

                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    createTriggers(db);
                                }
                            })
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }

    private static void createTriggers(SupportSQLiteDatabase db) {
        db.execSQL("CREATE TRIGGER IF NOT EXISTS limit_history_rows AFTER INSERT ON history_api_track " +
                "BEGIN " +
                "DELETE FROM history_api_track WHERE id IN (SELECT id FROM history_api_track ORDER BY fetch_timestamp DESC LIMIT -1 OFFSET 20); " +
                "END;");

        db.execSQL("CREATE TRIGGER IF NOT EXISTS limit_quotes_rows AFTER INSERT ON quotes_api_track " +
                "BEGIN " +
                "DELETE FROM quotes_api_track WHERE id IN (SELECT id FROM quotes_api_track ORDER BY fetch_timestamp DESC LIMIT -1 OFFSET 20); " +
                "END;");
    }
}