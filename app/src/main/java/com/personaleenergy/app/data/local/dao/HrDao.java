package com.personaleenergy.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import com.personaleenergy.app.data.local.entities.HrLocal;

@Dao
public interface HrDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(HrLocal hrLocal);
}
