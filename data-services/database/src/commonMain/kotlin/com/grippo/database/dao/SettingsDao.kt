package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grippo.database.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface SettingsDao {

    @Query("SELECT * FROM settings WHERE id = 0")
    public fun get(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrReplace(settings: SettingsEntity)
}