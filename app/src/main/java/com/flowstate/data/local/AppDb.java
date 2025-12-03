package com.flowstate.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.flowstate.data.local.dao.HrDao;
import com.flowstate.data.local.dao.SleepDao;
import com.flowstate.data.local.dao.StepsDao;
import com.flowstate.data.local.dao.TypingDao;
import com.flowstate.data.local.dao.ReactionDao;
import com.flowstate.data.local.dao.PredictionDao;

import com.flowstate.data.local.entities.HrLocal;
import com.flowstate.data.local.entities.SleepLocal;
import com.flowstate.data.local.entities.StepsLocal;
import com.flowstate.data.local.entities.TypingLocal;
import com.flowstate.data.local.entities.ReactionLocal;
import com.flowstate.data.local.entities.PredictionLocal;

/**
 * Room database for local storage
 * 
 * Stores data locally
 */
@Database(
    entities = {
        HrLocal.class,
        SleepLocal.class,
        StepsLocal.class,
        TypingLocal.class,
        ReactionLocal.class,
        PredictionLocal.class
    },
    version = 2,
    exportSchema = false
)
public abstract class AppDb extends RoomDatabase {
    
    private static final String DATABASE_NAME = "flowstate_db";
    private static AppDb instance;
    
    public abstract HrDao hrDao();
    public abstract SleepDao sleepDao();
    public abstract StepsDao stepsDao();
    public abstract TypingDao typingDao();
    public abstract ReactionDao reactionDao();
    public abstract PredictionDao predictionDao();
    
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

