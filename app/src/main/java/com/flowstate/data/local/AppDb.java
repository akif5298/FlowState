package com.flowstate.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.flowstate.data.local.dao.HrDao;
import com.flowstate.data.local.dao.SleepDao;
import com.flowstate.data.local.dao.TypingDao;
import com.flowstate.data.local.dao.ReactionDao;
import com.flowstate.data.local.dao.PredictionDao;
import com.flowstate.data.local.dao.HrvDao;
import com.flowstate.data.local.dao.StepsDao;
import com.flowstate.data.local.dao.WorkoutDao;
import com.flowstate.data.local.dao.BodyTempDao;
import com.flowstate.data.local.dao.ManualEnergyInputDao;
import com.flowstate.data.local.dao.ScheduleDao;
import com.flowstate.data.local.dao.CaffeineIntakeDao;
import com.flowstate.data.local.dao.EmotionDao;
import com.flowstate.data.local.dao.DeviceUsageDao;
import com.flowstate.data.local.dao.WeatherDao;

import com.flowstate.data.local.entities.HrLocal;
import com.flowstate.data.local.entities.SleepLocal;
import com.flowstate.data.local.entities.TypingLocal;
import com.flowstate.data.local.entities.ReactionLocal;
import com.flowstate.data.local.entities.PredictionLocal;
import com.flowstate.data.local.entities.HrvLocal;
import com.flowstate.data.local.entities.StepsLocal;
import com.flowstate.data.local.entities.WorkoutLocal;
import com.flowstate.data.local.entities.BodyTempLocal;
import com.flowstate.data.local.entities.ManualEnergyInputLocal;
import com.flowstate.data.local.entities.ScheduleLocal;
import com.flowstate.data.local.entities.CaffeineIntakeLocal;
import com.flowstate.data.local.entities.EmotionLocal;
import com.flowstate.data.local.entities.DeviceUsageLocal;
import com.flowstate.data.local.entities.WeatherLocal;

/**
 * Room database for local storage
 * 
 * Stores data locally before syncing to Supabase
 */
@Database(
    entities = {
        HrLocal.class,
        SleepLocal.class,
        TypingLocal.class,
        ReactionLocal.class,
        PredictionLocal.class,
        HrvLocal.class,
        StepsLocal.class,
        WorkoutLocal.class,
        BodyTempLocal.class,
        ManualEnergyInputLocal.class,
        ScheduleLocal.class,
        CaffeineIntakeLocal.class,
        EmotionLocal.class,
        DeviceUsageLocal.class,
        WeatherLocal.class
    },
    version = 4, // Incremented for new manual input entities
    exportSchema = false
)
public abstract class AppDb extends RoomDatabase {
    
    private static final String DATABASE_NAME = "flowstate_db";
    private static AppDb instance;
    
    public abstract HrDao hrDao();
    public abstract SleepDao sleepDao();
    public abstract TypingDao typingDao();
    public abstract ReactionDao reactionDao();
    public abstract PredictionDao predictionDao();
    public abstract HrvDao hrvDao();
    public abstract StepsDao stepsDao();
    public abstract WorkoutDao workoutDao();
    public abstract BodyTempDao bodyTempDao();
    public abstract ManualEnergyInputDao manualEnergyInputDao();
    public abstract ScheduleDao scheduleDao();
    public abstract CaffeineIntakeDao caffeineIntakeDao();
    public abstract EmotionDao emotionDao();
    public abstract DeviceUsageDao deviceUsageDao();
    public abstract WeatherDao weatherDao();
    
    /**
     * Get singleton instance of the database
     */
    public static synchronized AppDb getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDb.class,
                    DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // For development - remove in production
            .build();
        }
        return instance;
    }
}

