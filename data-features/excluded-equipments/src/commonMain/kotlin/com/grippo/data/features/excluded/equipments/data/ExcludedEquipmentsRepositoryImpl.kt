package com.grippo.data.features.excluded.equipments.data

import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.excluded.equipments.domain.ExcludedEquipmentsRepository
import com.grippo.entity.domain.equipment.toDomain
import com.grippo.services.backend.GrippoApi
import com.grippo.services.backend.dto.user.IdsBody
import com.grippo.services.database.dao.UserActiveDao
import com.grippo.services.database.dao.UserDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ExcludedEquipmentsRepository::class])
internal class ExcludedEquipmentsRepositoryImpl(
    private val api: GrippoApi,
    private val userDao: UserDao,
    private val userActiveDao: UserActiveDao
) : ExcludedEquipmentsRepository {

    override fun observeExcludedEquipments(): Flow<List<Equipment>> {
        return observeActiveProfileId()
            .flatMapLatest { profileId ->
                if (profileId.isNullOrEmpty()) flowOf(emptyList())
                else userDao.getExcludedEquipments(profileId).map { it.toDomain() }
            }
    }

    override suspend fun getExcludedEquipments(): Result<Unit> {
        val response = api.getExcludedEquipments()

        response.onSuccess {
            val profileId = getActiveProfileId() ?: return@onSuccess
            val ids = it.mapNotNull { m -> m.id }
            userDao.insertOrReplaceExcludedEquipments(profileId, ids)
        }

        return response.map { }
    }

    override suspend fun setExcludedEquipments(ids: List<String>): Result<Unit> {
        val response = api.postExcludedEquipments(IdsBody(ids))

        response.onSuccess {
            val profileId = getActiveProfileId() ?: return@onSuccess
            val equipments = api.getExcludedEquipments().getOrNull() ?: return@onSuccess
            val ids = equipments.mapNotNull { it.id }
            userDao.insertOrReplaceExcludedEquipments(profileId, ids)
        }

        return response.map { }
    }

    private fun observeActiveProfileId(): Flow<String?> {
        return userActiveDao.get()
            .flatMapLatest { userId ->
                if (userId.isNullOrEmpty()) flowOf(null)
                else userDao.getById(userId).map { it?.profileId }
            }
    }

    private suspend fun getActiveProfileId(): String? {
        val userId = userActiveDao.get().firstOrNull() ?: return null
        return userDao.getById(userId).firstOrNull()?.profileId
    }
}
