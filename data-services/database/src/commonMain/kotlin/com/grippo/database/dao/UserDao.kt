package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.database.entity.UserEntity
import com.grippo.database.entity.UserExcludedEquipmentEntity
import com.grippo.database.entity.UserExcludedMuscleEntity
import com.grippo.database.models.UserFull
import kotlinx.coroutines.flow.Flow

@Dao
public interface UserDao {

    @Query("SELECT * FROM user LIMIT 1")
    public fun get(): Flow<UserEntity?>

    @Transaction
    @Query("SELECT * FROM user LIMIT 1")
    public fun getFull(): Flow<UserFull?>

    @Transaction
    public suspend fun insertUserFull(userFull: UserFull) {
        insertOrUpdateUser(userFull.user)

        clearExcludedMuscles(userFull.user.id)
        insertExcludedMuscles(userFull.excludedMuscles)

        clearExcludedEquipments(userFull.user.id)
        insertExcludedEquipments(userFull.excludedEquipments)
    }

    @Transaction
    public suspend fun replaceExcludedEquipments(
        userId: String,
        equipments: List<UserExcludedEquipmentEntity>
    ) {
        clearExcludedEquipments(userId)
        insertExcludedEquipments(equipments)
    }

    @Transaction
    public suspend fun replaceExcludedMuscles(
        userId: String,
        muscles: List<UserExcludedMuscleEntity>
    ) {
        clearExcludedMuscles(userId)
        insertExcludedMuscles(muscles)
    }

    // Supportive methods

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdateUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertExcludedEquipments(equipments: List<UserExcludedEquipmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertExcludedMuscles(muscles: List<UserExcludedMuscleEntity>)

    @Query("DELETE FROM user_excluded_equipment WHERE userId = :userId")
    public suspend fun clearExcludedEquipments(userId: String)

    @Query("DELETE FROM user_excluded_muscle WHERE userId = :userId")
    public suspend fun clearExcludedMuscles(userId: String)
}