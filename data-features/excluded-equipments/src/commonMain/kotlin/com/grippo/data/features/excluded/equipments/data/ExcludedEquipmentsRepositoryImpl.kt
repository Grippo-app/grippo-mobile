package com.grippo.data.features.excluded.equipments.data

import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.excluded.equipments.domain.ExcludedEquipmentsRepository
import com.grippo.database.dao.UserActiveDao
import com.grippo.database.dao.UserDao
import com.grippo.database.domain.equipment.toDomain
import com.grippo.network.Api
import com.grippo.network.dto.user.IdsBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ExcludedEquipmentsRepository::class])
internal class ExcludedEquipmentsRepositoryImpl(
    private val api: Api,
    private val userDao: UserDao,
    private val userActiveDao: UserActiveDao
) : ExcludedEquipmentsRepository {

    override fun observeExcludedEquipments(): Flow<List<Equipment>> {
        return userActiveDao.get()
            .flatMapLatest { userId ->
                if (userId.isNullOrEmpty()) flowOf(emptyList())
                else userDao.getExcludedEquipments(userId).map { it.toDomain() }
            }
    }

    override suspend fun getExcludedEquipments(): Result<Unit> {
        val response = api.getExcludedEquipments()

        response.onSuccess {
            val userId = userActiveDao.get()
                .firstOrNull()
                ?: return@onSuccess

            val ids = it.mapNotNull { m -> m.id }

            userDao.insertOrReplaceExcludedEquipments(userId, ids)
        }

        return response.map { }
    }

    override suspend fun setExcludedEquipments(ids: List<String>): Result<Unit> {
        val response = api.postExcludedEquipments(IdsBody(ids))

        response.onSuccess {
            val userId = userActiveDao.get()
                .firstOrNull()
                ?: return@onSuccess

            val ids = api.getExcludedEquipments()
                .getOrNull()
                ?.mapNotNull { it.id }
                ?: return@onSuccess

            userDao.insertOrReplaceExcludedEquipments(userId, ids)
        }

        return response.map { }
    }
}
