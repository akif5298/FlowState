package com.personaleenergy.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.personaleenergy.app.data.local.entities.PredictionLocal;

import java.util.List;

@Dao
public interface PredictionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PredictionLocal prediction);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<PredictionLocal> predictions);

    @Query("SELECT * FROM prediction_local WHERE predictionTime >= :startMs AND predictionTime < :endMs ORDER BY predictionTime ASC")
    List<PredictionLocal> getByDateRange(long startMs, long endMs);

    @Query("SELECT * FROM prediction_local WHERE synced = 0")
    List<PredictionLocal> pending();

    @Query("UPDATE prediction_local SET synced = 1 WHERE id IN (:ids)")
    void markSynced(List<Long> ids);
}
