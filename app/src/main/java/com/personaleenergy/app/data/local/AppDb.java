package com.personaleenergy.app.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.personaleenergy.app.data.local.dao.HrDao;
import com.personaleenergy.app.data.local.dao.PredictionDao;
import com.personaleenergy.app.data.local.dao.ReactionDao;
import com.personaleenergy.app.data.local.dao.SleepDao;
import com.personaleenergy.app.data.local.dao.TypingDao;
import com.personaleenergy.app.data.local.entities.HrLocal;
import com.personaleenergy.app.data.local.entities.PredictionLocal;
import com.personaleenergy.app.data.local.entities.ReactionLocal;
import com.personaleenergy.app.data.local.entities.SleepLocal;
import com.personaleenergy.app.data.local.entities.TypingLocal;

@Database(entities = {HrLocal.class, PredictionLocal.class, ReactionLocal.class, SleepLocal.class, TypingLocal.class}, version = 1)
public abstract class AppDb extends RoomDatabase {

    public abstract HrDao hrDao();
    public abstract PredictionDao predictionDao();
    public abstract ReactionDao reactionDao();
    public abstract SleepDao sleepDao();
    public abstract TypingDao typingDao();

    private static volatile AppDb INSTANCE;

    public static AppDb getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDb.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDb.class, "app_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
