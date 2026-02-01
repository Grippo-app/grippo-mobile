package com.grippo.services.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.grippo.services.database.dao.DraftTrainingDao
import com.grippo.services.database.dao.EquipmentDao
import com.grippo.services.database.dao.ExerciseExampleDao
import com.grippo.services.database.dao.MuscleDao
import com.grippo.services.database.dao.TokenDao
import com.grippo.services.database.dao.TrainingDao
import com.grippo.services.database.dao.UserActiveDao
import com.grippo.services.database.dao.UserDao
import com.grippo.services.database.dao.WeightHistoryDao
import com.grippo.services.database.entity.DraftExerciseEntity
import com.grippo.services.database.entity.DraftIterationEntity
import com.grippo.services.database.entity.DraftTrainingEntity
import com.grippo.services.database.entity.EquipmentEntity
import com.grippo.services.database.entity.EquipmentGroupEntity
import com.grippo.services.database.entity.ExerciseEntity
import com.grippo.services.database.entity.ExerciseExampleBundleEntity
import com.grippo.services.database.entity.ExerciseExampleComponentsEntity
import com.grippo.services.database.entity.ExerciseExampleEntity
import com.grippo.services.database.entity.ExerciseExampleEquipmentEntity
import com.grippo.services.database.entity.IterationEntity
import com.grippo.services.database.entity.MuscleEntity
import com.grippo.services.database.entity.MuscleGroupEntity
import com.grippo.services.database.entity.TokenEntity
import com.grippo.services.database.entity.TrainingEntity
import com.grippo.services.database.entity.UserActiveEntity
import com.grippo.services.database.entity.UserEntity
import com.grippo.services.database.entity.UserExcludedEquipmentEntity
import com.grippo.services.database.entity.UserExcludedMuscleEntity
import com.grippo.services.database.entity.WeightHistoryEntity

@Database(
    entities = [
        TokenEntity::class,
        UserActiveEntity::class,

        EquipmentEntity::class,
        EquipmentGroupEntity::class,

        MuscleEntity::class,
        MuscleGroupEntity::class,

        ExerciseExampleEntity::class,
        ExerciseExampleBundleEntity::class,
        ExerciseExampleComponentsEntity::class,
        ExerciseExampleEquipmentEntity::class,

        TrainingEntity::class,
        ExerciseEntity::class,
        IterationEntity::class,

        DraftTrainingEntity::class,
        DraftExerciseEntity::class,
        DraftIterationEntity::class,

        UserEntity::class,
        UserExcludedMuscleEntity::class,
        UserExcludedEquipmentEntity::class,
        WeightHistoryEntity::class,
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(DatabaseConstructor::class)
public abstract class Database : RoomDatabase() {
    public abstract fun userActiveDao(): UserActiveDao
    public abstract fun tokenDao(): TokenDao
    public abstract fun userDao(): UserDao
    public abstract fun weightHistoryDao(): WeightHistoryDao
    public abstract fun equipmentDao(): EquipmentDao
    public abstract fun trainingDao(): TrainingDao
    public abstract fun draftTrainingDao(): DraftTrainingDao
    public abstract fun exerciseExampleDao(): ExerciseExampleDao
    public abstract fun muscleDao(): MuscleDao
}
