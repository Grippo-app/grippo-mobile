package com.grippo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.grippo.database.entity.DraftExerciseEntity
import com.grippo.database.entity.DraftIterationEntity
import com.grippo.database.entity.DraftTrainingEntity
import com.grippo.database.models.DraftTrainingPack
import kotlinx.coroutines.flow.Flow

@Dao
public interface DraftTrainingDao {

    @Transaction
    @Query("""SELECT * FROM draft_training """)
    public fun get(): Flow<DraftTrainingPack?>

    // ────────────── INSERT ──────────────

    @Transaction
    public suspend fun insertOrReplace(
        training: DraftTrainingEntity,
        exercises: List<DraftExerciseEntity>,
        iterations: List<DraftIterationEntity>
    ) {
        insertTraining(training)

        for (exercise in exercises) {
            insertExercise(exercise)
        }

        for (iteration in iterations) {
            insertIteration(iteration)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertTraining(training: DraftTrainingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertExercise(exercise: DraftExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertIteration(iteration: DraftIterationEntity)

    // ────────────── DELETE ──────────────

    @Query("DELETE FROM draft_training")
    public suspend fun delete()
}
