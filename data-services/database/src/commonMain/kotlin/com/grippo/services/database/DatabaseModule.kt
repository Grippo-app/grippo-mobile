package com.grippo.services.database

import com.grippo.services.database.dao.DraftTrainingDao
import com.grippo.services.database.dao.EquipmentDao
import com.grippo.services.database.dao.ExerciseExampleDao
import com.grippo.services.database.dao.MuscleDao
import com.grippo.services.database.dao.TokenDao
import com.grippo.services.database.dao.TrainingDao
import com.grippo.services.database.dao.UserActiveDao
import com.grippo.services.database.dao.UserDao
import com.grippo.services.database.dao.WeightHistoryDao
import com.grippo.toolkit.context.ContextModule
import com.grippo.toolkit.context.NativeContext
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module(includes = [ContextModule::class])
@ComponentScan
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

    @Single
    internal fun provideDraftTrainingDao(db: Database): DraftTrainingDao = db.draftTrainingDao()
}
