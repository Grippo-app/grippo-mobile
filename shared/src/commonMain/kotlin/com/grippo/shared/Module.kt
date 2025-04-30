package com.grippo.shared

import com.grippo.data.features.authorization.authorizationFeatureModule
import com.grippo.data.features.user.userFeatureModule
import com.grippo.data.features.weight.history.weightHistoryFeatureModule
import com.grippo.database.databaseModule
import com.grippo.network.networkModule
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatformTools

public fun initKoin(
    appDeclaration: KoinAppDeclaration = {},
): KoinApplication = KoinPlatformTools.defaultContext().startKoin {
    appDeclaration()
    modules(
        platformModule,
        networkModule,
        databaseModule,
        authorizationFeatureModule,
        userFeatureModule,
        weightHistoryFeatureModule
    )
}