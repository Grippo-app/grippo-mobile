package com.grippo.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.grippo.database.dao.EquipmentDao
import com.grippo.database.dao.ExerciseExampleDao
import com.grippo.database.dao.MuscleDao
import com.grippo.database.dao.SettingsDao
import com.grippo.database.dao.TokenDao
import com.grippo.database.dao.TrainingDao
import com.grippo.database.dao.UserActiveDao
import com.grippo.database.dao.UserDao
import com.grippo.database.dao.WeightHistoryDao
import com.grippo.database.entity.EquipmentEntity
import com.grippo.database.entity.EquipmentGroupEntity
import com.grippo.database.entity.ExerciseEntity
import com.grippo.database.entity.ExerciseExampleBundleEntity
import com.grippo.database.entity.ExerciseExampleEntity
import com.grippo.database.entity.ExerciseExampleEquipmentEntity
import com.grippo.database.entity.ExerciseExampleTutorialEntity
import com.grippo.database.entity.IterationEntity
import com.grippo.database.entity.MuscleEntity
import com.grippo.database.entity.MuscleGroupEntity
import com.grippo.database.entity.SettingsEntity
import com.grippo.database.entity.TokenEntity
import com.grippo.database.entity.TrainingEntity
import com.grippo.database.entity.UserActiveEntity
import com.grippo.database.entity.UserEntity
import com.grippo.database.entity.UserExcludedEquipmentEntity
import com.grippo.database.entity.UserExcludedMuscleEntity
import com.grippo.database.entity.WeightHistoryEntity

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
        ExerciseExampleEquipmentEntity::class,
        ExerciseExampleTutorialEntity::class,

        TrainingEntity::class,
        ExerciseEntity::class,
        IterationEntity::class,

        UserEntity::class,
        UserExcludedMuscleEntity::class,
        UserExcludedEquipmentEntity::class,
        WeightHistoryEntity::class,

        SettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(DatabaseConstructor::class)
public abstract class Database : RoomDatabase() {
    public abstract fun userActiveDao(): UserActiveDao
    public abstract fun tokenDao(): TokenDao
    public abstract fun settingsDao(): SettingsDao
    public abstract fun userDao(): UserDao
    public abstract fun weightHistoryDao(): WeightHistoryDao
    public abstract fun equipmentDao(): EquipmentDao
    public abstract fun trainingDao(): TrainingDao
    public abstract fun exerciseExampleDao(): ExerciseExampleDao
    public abstract fun muscleDao(): MuscleDao
}