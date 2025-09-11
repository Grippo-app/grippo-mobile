package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.database.entity.ExerciseExampleBundleEntity
import com.grippo.database.entity.ExerciseExampleEntity
import com.grippo.database.entity.ExerciseExampleEquipmentEntity
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
    SELECT DISTINCT ee.*
    FROM exercise_example ee
    WHERE (:name IS NULL OR LOWER(ee.name) LIKE '%' || LOWER(:name) || '%')
      AND (:forceType IS NULL OR ee.forceType = :forceType)
      AND (:weightType IS NULL OR ee.weightType = :weightType)
      AND (:category IS NULL OR ee.category = :category)
      AND (:experience IS NULL OR ee.experience = :experience)
      AND (
          :muscleGroupId IS NULL OR EXISTS (
              SELECT 1
              FROM exercise_example_bundle eb
              INNER JOIN muscle m ON m.id = eb.muscleId
              WHERE eb.exerciseExampleId = ee.id
                AND eb.percentage = (
                    SELECT MAX(eb2.percentage)
                    FROM exercise_example_bundle eb2
                    WHERE eb2.exerciseExampleId = ee.id
                )
                AND m.muscleGroupId = :muscleGroupId
          )
      )
    ORDER BY
      CASE WHEN :sorting = 'NewAdded'     THEN ee.createdAt END DESC,
      CASE WHEN :sorting = 'RecentlyUsed' THEN ee.lastUsed END DESC,
      CASE WHEN :sorting = 'MostlyUsed'   THEN ee.usageCount END DESC,
      ee.name ASC
    """
    )
    public fun getAll(
        name: String? = null,
        forceType: String? = null,
        weightType: String? = null,
        category: String? = null,
        experience: String? = null,
        muscleGroupId: String? = null,
        sorting: String
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
    ) {
        insertExerciseExample(example)
        insertBundles(bundles)
        insertEquipments(equipments)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertExerciseExample(example: ExerciseExampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertBundles(bundles: List<ExerciseExampleBundleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertEquipments(equipments: List<ExerciseExampleEquipmentEntity>)

    // ────────────── DELETE ──────────────

    @Query("DELETE FROM exercise_example")
    public suspend fun delete()
}