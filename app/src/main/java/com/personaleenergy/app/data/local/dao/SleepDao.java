package com.personaleenergy.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import com.personaleenergy.app.data.local.entities.SleepLocal;

@Dao
public interface SleepDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SleepLocal sleepLocal);
}
