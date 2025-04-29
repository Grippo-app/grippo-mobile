package com.grippo.data.features.authorization

import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.authorization.data.AuthorizationRepositoryImpl
import com.grippo.data.features.authorization.domain.AuthorizationFeatureImpl
import com.grippo.data.features.authorization.domain.AuthorizationRepository
import org.koin.core.module.Module
import org.koin.dsl.module

public val authorizationFeatureModule: Module = module {
    single<AuthorizationFeature> {
        AuthorizationFeatureImpl(get())
    }

    single<AuthorizationRepository> {
        AuthorizationRepositoryImpl(get(), get())
    }
}