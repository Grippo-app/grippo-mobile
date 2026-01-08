package com.grippo.data.features.exercise.metrics

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module(includes = [com.grippo.services.backend.BackendModule::class])
@ComponentScan
public class ExerciseMetricsFeatureModule
