package com.personaleenergy.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.personaleenergy.data.local.dao.HrDao;
import com.personaleenergy.data.local.dao.SleepDao;
import com.personaleenergy.data.local.dao.TypingDao;
import com.personaleenergy.data.local.dao.ReactionDao;
import com.personaleenergy.data.local.dao.PredictionDao;
import com.personaleenergy.data.local.entities.HrLocal;
import com.personaleenergy.data.local.entities.SleepLocal;
import com.personaleenergy.data.local.entities.TypingLocal;
import com.personaleenergy.data.local.entities.ReactionLocal;
import com.personaleenergy.data.local.entities.PredictionLocal;

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
        PredictionLocal.class
    },
    version = 2, // Incremented for TypingLocal schema changes (added totalChars, errors, durationSecs)
    exportSchema = false
)
public abstract class AppDb extends RoomDatabase {
    
    private static final String DATABASE_NAME = "personaleenergy_db";
    private static AppDb instance;
    
    public abstract HrDao hrDao();
    public abstract SleepDao sleepDao();
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
