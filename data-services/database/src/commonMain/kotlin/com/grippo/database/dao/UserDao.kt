package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.grippo.database.entity.EquipmentEntity
import com.grippo.database.entity.MuscleEntity
import com.grippo.database.entity.UserEntity
import com.grippo.database.entity.UserExcludedEquipmentEntity
import com.grippo.database.entity.UserExcludedMuscleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

@Dao
public interface UserDao {

    @Query(
        """
        SELECT m.* FROM muscle AS m
        INNER JOIN user_excluded_muscle AS uem ON m.id = uem.muscleId
    """
    )
    public fun getExcludedMuscles(): Flow<List<MuscleEntity>>

    @Query(
        """
        SELECT e.* FROM equipment AS e
        INNER JOIN user_excluded_equipment AS uee ON e.id = uee.equipmentId
    """
    )
    public fun getExcludedEquipments(): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM user LIMIT 1")
    public fun get(): Flow<UserEntity?>

    @Query("SELECT * FROM user WHERE id = :id LIMIT 1")
    public fun getById(id: String): Flow<UserEntity?>

    // ────────────── INSERT ──────────────

    @Transaction
    public suspend fun insertOrReplaceExcludedEquipments(equipments: List<UserExcludedEquipmentEntity>) {
        clearExcludedEquipments()
        insertExcludedEquipments(equipments)
    }

    @Transaction
    public suspend fun insertOrReplaceExcludedMuscles(muscles: List<UserExcludedMuscleEntity>) {
        clearExcludedMuscles()
        insertExcludedMuscles(muscles)
    }

    @Transaction
    public suspend fun insertOrUpdate(user: UserEntity) {
        val existingUser = getById(user.id).firstOrNull()

        if (existingUser != null) {
            updateUser(user)
        } else {
            insertUser(user)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertUser(user: UserEntity)

    @Update
    public suspend fun updateUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertExcludedEquipments(equipments: List<UserExcludedEquipmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertExcludedMuscles(muscles: List<UserExcludedMuscleEntity>)

    // ────────────── DELETE ──────────────

    @Query("DELETE FROM user_excluded_equipment")
    public suspend fun clearExcludedEquipments()

    @Query("DELETE FROM user_excluded_muscle")
    public suspend fun clearExcludedMuscles()
}