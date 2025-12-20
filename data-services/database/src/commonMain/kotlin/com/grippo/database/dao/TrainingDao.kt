package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.database.entity.ExerciseEntity
import com.grippo.database.entity.IterationEntity
import com.grippo.database.entity.TrainingEntity
import com.grippo.database.models.ExercisePack
import com.grippo.database.models.TrainingPack
import kotlinx.coroutines.flow.Flow

@Dao
public interface TrainingDao {

    @Transaction
    @Query(
        """
        SELECT * FROM training
        WHERE createdAt BETWEEN :from AND :to
        ORDER BY createdAt DESC
        """
    )
    public fun get(from: String, to: String): Flow<List<TrainingPack>>

    @Transaction
    @Query(
        """
        SELECT * FROM training
        WHERE id = :id
        ORDER BY createdAt DESC
        LIMIT 1
        """
    )
    public fun getById(id: String): Flow<TrainingPack?>

    @Transaction
    @Query(
        """
        SELECT * FROM exercise
        WHERE id = :id
        LIMIT 1
        """
    )
    public fun getExerciseById(id: String): Flow<ExercisePack?>

    @Transaction
    @Query(
        """
        SELECT * FROM training
        ORDER BY createdAt DESC
        LIMIT 1
        """
    )
    public fun getLast(): Flow<TrainingPack?>

    // ────────────── INSERT ──────────────

    @Transaction
    public suspend fun insertOrReplace(
        training: TrainingEntity,
        exercises: List<ExerciseEntity>,
        iterations: List<IterationEntity>
    ) {
        insertTraining(training)

        if (exercises.isNotEmpty()) {
            insertExercises(exercises)
        }

        if (iterations.isNotEmpty()) {
            insertIterations(iterations)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertTraining(training: TrainingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertIteration(iteration: IterationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertIterations(iterations: List<IterationEntity>)

    // ────────────── DELETE ──────────────

    @Query("DELETE FROM training")
    public suspend fun delete()

    @Query("DELETE FROM training WHERE id = :id")
    public suspend fun deleteById(id: String)
}
