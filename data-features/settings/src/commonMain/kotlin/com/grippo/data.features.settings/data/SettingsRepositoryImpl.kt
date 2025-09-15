package com.grippo.data.features.settings.data

import com.grippo.data.features.api.settings.models.Locale
import com.grippo.data.features.api.settings.models.Settings
import com.grippo.data.features.api.settings.models.Theme
import com.grippo.data.features.settings.domain.SettingsRepository
import com.grippo.database.dao.SettingsDao
import com.grippo.database.domain.settings.toDomain
import com.grippo.database.entity.SettingsEntity
import com.grippo.domain.database.settings.settings.toEntity
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
            .map { it?.toDomain() ?: SettingsEntity().toDomain() }
    }

    override suspend fun setTheme(theme: Theme): Result<Unit> {
        val updated = settingsDao.get().firstOrNull()
            ?.copy(theme = theme.toEntity())
            ?: SettingsEntity(theme = theme.toEntity())

        settingsDao.insertOrReplace(updated)

        return Result.success(Unit)
    }

    override suspend fun setLocale(locale: Locale): Result<Unit> {
        val updated = settingsDao.get().firstOrNull()
            ?.copy(locale = locale.toEntity())
            ?: SettingsEntity(locale = locale.toEntity())

        settingsDao.insertOrReplace(updated)

        return Result.success(Unit)
    }
}