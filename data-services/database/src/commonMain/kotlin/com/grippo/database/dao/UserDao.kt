package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grippo.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdate(user: UserEntity)

    @Query("SELECT * FROM user LIMIT 1")
    public fun get(): Flow<UserEntity?>

    @Query("DELETE FROM user")
    public suspend fun delete()
}