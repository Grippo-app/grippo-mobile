package com.grippo.data.features.exercise.examples

import com.grippo.backend.BackendModule
import com.grippo.database.DatabaseModule
import com.grippo.ml.translation.MlTranslationModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [BackendModule::class, DatabaseModule::class, MlTranslationModule::class])
@ComponentScan
public class ExerciseExamplesFeatureModule
