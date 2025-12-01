package com.personaleenergy.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import com.personaleenergy.app.data.local.entities.TypingLocal;

@Dao
public interface TypingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TypingLocal typingLocal);
}
