package com.grippo.data.features.exercise.metrics

import com.grippo.backend.BackendModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [BackendModule::class])
@ComponentScan
public class ExerciseMetricsFeatureModule
