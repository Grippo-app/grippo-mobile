package com.grippo.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.grippo.database.dao.EquipmentDao
import com.grippo.database.dao.ExerciseExampleDao
import com.grippo.database.dao.MuscleDao
import com.grippo.database.dao.TokenDao
import com.grippo.database.dao.TrainingDao
import com.grippo.database.dao.UserDao
import com.grippo.database.dao.WeightHistoryDao
import com.grippo.database.entity.EquipmentEntity
import com.grippo.database.entity.EquipmentGroupEntity
import com.grippo.database.entity.ExerciseEntity
import com.grippo.database.entity.ExerciseEquipmentEntity
import com.grippo.database.entity.ExerciseExampleBundleEntity
import com.grippo.database.entity.ExerciseExampleEntity
import com.grippo.database.entity.ExerciseTutorialEntity
import com.grippo.database.entity.IterationEntity
import com.grippo.database.entity.MuscleEntity
import com.grippo.database.entity.MuscleGroupEntity
import com.grippo.database.entity.TokenEntity
import com.grippo.database.entity.TrainingEntity
import com.grippo.database.entity.UserEntity
import com.grippo.database.entity.WeightHistoryEntity

@Database(
    entities = [
        EquipmentEntity::class,
        EquipmentGroupEntity::class,
        MuscleEntity::class,
        MuscleGroupEntity::class,
        ExerciseExampleEntity::class,
        ExerciseExampleBundleEntity::class,
        ExerciseEquipmentEntity::class,
        ExerciseTutorialEntity::class,
        TrainingEntity::class,
        ExerciseEntity::class,
        IterationEntity::class,
        UserEntity::class,
        TokenEntity::class,
        WeightHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase : RoomDatabase() {
    public abstract fun equipmentDao(): EquipmentDao
    public abstract fun muscleDao(): MuscleDao
    public abstract fun exerciseExampleDao(): ExerciseExampleDao
    public abstract fun trainingDao(): TrainingDao
    public abstract fun userDao(): UserDao
    public abstract fun weightHistoryDao(): WeightHistoryDao
    public abstract fun tokenDao(): TokenDao
}