package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.database.entity.MuscleEntity
import com.grippo.database.entity.MuscleGroupEntity
import com.grippo.database.models.MuscleGroupWithMuscles
import kotlinx.coroutines.flow.Flow

@Dao
public interface MuscleDao {

    @Transaction
    public suspend fun insertAllGroupsWithMuscles(
        groups: List<MuscleGroupEntity>,
        muscles: List<MuscleEntity>
    ) {
        insertOrIgnore(groups)
        insertOrUpdate(muscles)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdate(muscle: MuscleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertOrUpdate(muscles: List<MuscleEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public suspend fun insertOrIgnore(muscleGroup: MuscleGroupEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public suspend fun insertOrIgnore(muscleGroups: List<MuscleGroupEntity>)

    @Transaction
    @Query("SELECT * FROM muscle WHERE id IN (:ids)")
    public fun getByIds(ids: List<String>): Flow<List<MuscleEntity>>

    @Transaction
    @Query("SELECT * FROM muscle_group")
    public fun getGroups(): Flow<List<MuscleGroupWithMuscles>>

    @Query("DELETE FROM muscle")
    public suspend fun delete()

    @Query("DELETE FROM muscle_group")
    public suspend fun deleteGroups()
}