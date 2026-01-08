package com.grippo.services.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grippo.services.database.entity.UserActiveEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface UserActiveDao {

    @Query("SELECT userId FROM user_active WHERE id = 0")
    public fun get(): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrReplace(active: UserActiveEntity)

    @Query("DELETE FROM user_active WHERE id = 0")
    public suspend fun delete()
}