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

      -- Experience ladder filter
      AND (
          :experience IS NULL
          OR (
              CASE ee.experience
                  WHEN 'beginner'     THEN 1
                  WHEN 'intermediate' THEN 2
                  WHEN 'advanced'     THEN 3
                  WHEN 'pro'          THEN 4
                  ELSE 0
              END
              <=
              CASE :experience
                  WHEN 'beginner'     THEN 1  -- beginner sees only beginner
                  WHEN 'intermediate' THEN 2  -- beginner + intermediate
                  WHEN 'advanced'     THEN 3  -- beginner + intermediate + advanced
                  WHEN 'pro'          THEN 4  -- everything
                  ELSE 4
              END
          )
      )

      -- Optional muscle group filter: applies only if the top-percentage muscle belongs to the group
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

      -- EXCLUDE: any top-percentage muscle is in excludedMuscleIds
      AND NOT EXISTS (
          SELECT 1
          FROM exercise_example_bundle ebx
          WHERE ebx.exerciseExampleId = ee.id
            AND ebx.percentage = (
                SELECT MAX(eb2.percentage)
                FROM exercise_example_bundle eb2
                WHERE eb2.exerciseExampleId = ee.id
            )
            AND ebx.muscleId IN (:excludedMuscleIds)
      )

      -- EXCLUDE: any equipment is in excludedEquipmentIds
      AND NOT EXISTS (
          SELECT 1
          FROM exercise_example_equipment eee
          WHERE eee.exerciseExampleId = ee.id
            AND eee.equipmentId IN (:excludedEquipmentIds)
      )

    ORDER BY
      CASE WHEN :sorting = 'NewAdded'     THEN ee.createdAt END DESC,
      CASE WHEN :sorting = 'RecentlyUsed' THEN ee.lastUsed END DESC,
      CASE WHEN :sorting = 'MostlyUsed'   THEN ee.usageCount END DESC,
      ee.name ASC
    LIMIT CASE WHEN :limits IS NULL THEN -1 ELSE :limits END
    OFFSET CASE
        WHEN :limits IS NULL OR :number IS NULL OR :number <= 1 THEN 0
        ELSE (:number - 1) * :limits
    END
    """
    )
    public fun getAll(
        name: String? = null,
        forceType: String? = null,
        weightType: String? = null,
        category: String? = null,
        experience: String? = null,
        muscleGroupId: String? = null,
        excludedEquipmentIds: Set<String>,
        excludedMuscleIds: Set<String>,
        sorting: String,
        limits: Int?,
        number: Int?,
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

        if (bundles.isNotEmpty()) {
            insertBundles(bundles)
        }

        if (equipments.isNotEmpty()) {
            insertEquipments(equipments)
        }
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
