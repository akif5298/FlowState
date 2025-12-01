package com.personaleenergy.data.local.repo;

import android.content.Context;
import android.util.Log;
import com.personaleenergy.data.local.AppDb;
import com.personaleenergy.data.local.dao.TypingDao;
import com.personaleenergy.data.local.entities.TypingLocal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TypingSpeedRepository {

    private static final String TAG = "TypingSpeedRepository";
    private final TypingDao typingDao;
    private final ExecutorService databaseWriteExecutor;

    public TypingSpeedRepository(Context context) {
        AppDb db = AppDb.getInstance(context);
        this.typingDao = db.typingDao();
        this.databaseWriteExecutor = Executors.newSingleThreadExecutor();
    }

    public void save(TypingLocal typingLocal, DataCallback<Long> callback) {
        databaseWriteExecutor.execute(() -> {
            try {
                long id = typingDao.insert(typingLocal);
                Log.d(TAG, "Saved typing test to database with id: " + id);
                if (callback != null) {
                    callback.onSuccess(id);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving typing test", e);
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
