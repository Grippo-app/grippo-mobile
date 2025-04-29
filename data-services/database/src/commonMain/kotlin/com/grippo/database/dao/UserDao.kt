package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grippo.database.entity.UserEntity

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdateUser(user: UserEntity)

    @Query("SELECT * FROM user LIMIT 1")
    public suspend fun getUser(): UserEntity?

    @Query("DELETE FROM user")
    public suspend fun deleteTableUser()
}