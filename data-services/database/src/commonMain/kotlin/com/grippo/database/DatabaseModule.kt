package com.grippo.database

import com.grippo.database.dao.EquipmentDao
import com.grippo.database.dao.ExerciseExampleDao
import com.grippo.database.dao.MuscleDao
import com.grippo.database.dao.TokenDao
import com.grippo.database.dao.TrainingDao
import com.grippo.database.dao.UserActiveDao
import com.grippo.database.dao.UserDao
import com.grippo.database.dao.WeightHistoryDao
import com.grippo.platform.core.NativeContext
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.grippo.database")
public class DatabaseModule {

    @Single
    internal fun provideDatabase(nativeContext: NativeContext): Database {
        return nativeContext.getDatabaseBuilder()
    }

    @Single
    internal fun provideTokenDao(db: Database): TokenDao = db.tokenDao()

    @Single
    internal fun provideUserDao(db: Database): UserDao = db.userDao()

    @Single
    internal fun provideWeightHistoryDao(db: Database): WeightHistoryDao = db.weightHistoryDao()

    @Single
    internal fun provideUserActiveDao(db: Database): UserActiveDao = db.userActiveDao()

    @Single
    internal fun provideEquipmentDao(db: Database): EquipmentDao = db.equipmentDao()

    @Single
    internal fun provideExerciseExampleDao(db: Database): ExerciseExampleDao =
        db.exerciseExampleDao()

    @Single
    internal fun provideMuscleDao(db: Database): MuscleDao = db.muscleDao()

    @Single
    internal fun provideTrainingDao(db: Database): TrainingDao = db.trainingDao()

}