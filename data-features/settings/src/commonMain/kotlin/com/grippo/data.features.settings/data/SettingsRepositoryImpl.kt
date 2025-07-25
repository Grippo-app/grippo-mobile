package com.grippo.data.features.settings.data

import com.grippo.data.features.api.settings.models.ColorMode
import com.grippo.data.features.api.settings.models.Settings
import com.grippo.data.features.settings.domain.SettingsRepository
import com.grippo.database.dao.SettingsDao
import com.grippo.database.mapper.settings.toDomain
import com.grippo.domain.mapper.settings.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [SettingsRepository::class])
internal class SettingsRepositoryImpl(
    private val settingsDao: SettingsDao
) : SettingsRepository {

    override fun observeSettings(): Flow<Settings?> {
        return settingsDao.get()
            .map { it.toDomain() }
    }

    override suspend fun setColorMode(mode: ColorMode): Result<Unit> {
        val updated = settingsDao.get().firstOrNull()
            ?.copy(colorMode = mode.toEntity())
            ?: return Result.success(Unit)

        settingsDao.insertOrReplace(updated)

        return Result.success(Unit)
    }
}