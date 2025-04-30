package com.grippo.data.features.user

import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.user.data.UserRepositoryImpl
import com.grippo.data.features.user.domain.UserFeatureImpl
import com.grippo.data.features.user.domain.UserRepository
import org.koin.core.module.Module
import org.koin.dsl.module

public val userFeatureModule: Module = module {
    single<UserFeature> {
        UserFeatureImpl(get())
    }

    single<UserRepository> {
        UserRepositoryImpl(get(), get())
    }
}