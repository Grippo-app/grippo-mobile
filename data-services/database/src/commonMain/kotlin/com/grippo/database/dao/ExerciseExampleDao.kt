package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.database.entity.ExerciseExampleBundleEntity
import com.grippo.database.entity.ExerciseExampleEntity
import com.grippo.database.entity.ExerciseExampleEquipmentEntity
import com.grippo.database.entity.ExerciseExampleTutorialEntity
import com.grippo.database.models.ExerciseExamplePack
import kotlinx.coroutines.flow.Flow

@Dao
public interface ExerciseExampleDao {

    @Transaction
    @Query("SELECT * FROM exercise_example WHERE id = :id")
    public fun getById(id: String): Flow<ExerciseExamplePack>

    @Transaction
    @Query("SELECT * FROM exercise_example ORDER BY updatedAt DESC")
    public fun get(): Flow<List<ExerciseExamplePack>>

    @Transaction
    @Query(
        """
        SELECT * FROM exercise_example
        WHERE (LENGTH(TRIM(:name)) = 0 OR LOWER(name) LIKE LOWER('%' || :name || '%'))
        AND (:forceType IS NULL OR forceType = :forceType)
        AND (:weightType IS NULL OR weightType = :weightType)
        AND (:category IS NULL OR category = :category)
        AND (:experience IS NULL OR experience = :experience)
        ORDER BY updatedAt DESC
    """
    )
    public fun getFiltered(
        name: String,
        forceType: String? = null,
        weightType: String? = null,
        category: String? = null,
        experience: String? = null
    ): Flow<List<ExerciseExamplePack>>

    @Transaction
    @Query("SELECT * FROM exercise_example WHERE id IN (:ids) ORDER BY updatedAt DESC")
    public fun get(ids: List<String>): Flow<List<ExerciseExamplePack>>

    // ────────────── INSERT ──────────────

    @Transaction
    public suspend fun insertOrReplace(
        example: ExerciseExampleEntity,
        bundles: List<ExerciseExampleBundleEntity>,
        equipments: List<ExerciseExampleEquipmentEntity>,
        tutorials: List<ExerciseExampleTutorialEntity>
    ) {
        insertExerciseExample(example)
        insertBundles(bundles)
        insertEquipments(equipments)
        insertTutorials(tutorials)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertExerciseExample(example: ExerciseExampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertBundles(bundles: List<ExerciseExampleBundleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertEquipments(equipments: List<ExerciseExampleEquipmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertTutorials(tutorials: List<ExerciseExampleTutorialEntity>)

    // ────────────── DELETE ──────────────

    @Query("DELETE FROM exercise_example")
    public suspend fun delete()
}