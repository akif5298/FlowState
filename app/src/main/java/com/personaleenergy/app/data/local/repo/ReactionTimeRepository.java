package com.personaleenergy.app.data.local.repo;

import android.content.Context;
import android.util.Log;
import com.personaleenergy.app.data.local.AppDb;
import com.personaleenergy.app.data.local.dao.ReactionDao;
import com.personaleenergy.app.data.local.entities.ReactionLocal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReactionTimeRepository {

    private static final String TAG = "ReactionTimeRepository";
    private final ReactionDao reactionDao;
    private final ExecutorService databaseWriteExecutor;

    public ReactionTimeRepository(Context context) {
        AppDb db = AppDb.getInstance(context);
        this.reactionDao = db.reactionDao();
        this.databaseWriteExecutor = Executors.newSingleThreadExecutor();
    }

    public void save(ReactionLocal reactionLocal, DataCallback<Long> callback) {
        databaseWriteExecutor.execute(() -> {
            try {
                long id = reactionDao.insert(reactionLocal);
                Log.d(TAG, "Saved reaction time to database with id: " + id);
                if (callback != null) {
                    callback.onSuccess(id);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving reaction time", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(Exception e);
    }
}
