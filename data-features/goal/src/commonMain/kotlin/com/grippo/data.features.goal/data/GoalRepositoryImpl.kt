package com.grippo.data.features.goal.data

import com.grippo.core.error.provider.AppError
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.goal.models.SetGoal
import com.grippo.data.features.goal.domain.GoalRepository
import com.grippo.domain.dto.user.toBody
import com.grippo.dto.entity.user.toEntityOrNull
import com.grippo.entity.domain.user.toDomain
import com.grippo.services.backend.GrippoApi
import com.grippo.services.database.dao.GoalDao
import com.grippo.services.database.dao.UserActiveDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [GoalRepository::class])
internal class GoalRepositoryImpl(
    private val api: GrippoApi,
    private val goalDao: GoalDao,
    private val userActiveDao: UserActiveDao,
) : GoalRepository {

    override fun observeGoal(): Flow<Goal?> {
        return userActiveDao.get()
            .flatMapLatest { activeId ->
                if (activeId == null) flowOf(null)
                else goalDao.getByUserId(activeId).map { it?.toDomain() }
            }
    }

    override suspend fun getGoal(): Boolean {
        val userId = userActiveDao.get().firstOrNull() ?: return false
        val response = api.getGoal()

        if (response == null) {
            goalDao.deleteByUserId(userId)
            return false
        }

        if (response.personalizations == null) {
            throw AppError.Network.IncompleteResponse()
        }
        val entity = response.toEntityOrNull(userId) ?: throw AppError.Network.IncompleteResponse()
        val mapped = entity.toDomain() ?: throw AppError.Network.IncompleteResponse()
        if (mapped.personalizations.size != entity.personalizations.size ||
            (response.secondaryGoal != null && mapped.secondaryGoal == null)
        ) {
            throw AppError.Network.IncompleteResponse()
        }
        goalDao.insertOrUpdate(entity)
        return true
    }

    override suspend fun setGoal(goal: SetGoal): Unit {
        val userId = userActiveDao.get().firstOrNull()
            ?: throw AppError.Expected(
                message = "An active user is required to save a goal",
                description = null,
            )
        val response = api.setGoal(goal.toBody())

        if (response.personalizations == null) {
            throw AppError.Network.IncompleteResponse()
        }
        val entity = response.toEntityOrNull(userId) ?: throw AppError.Network.IncompleteResponse()
        val mapped = entity.toDomain() ?: throw AppError.Network.IncompleteResponse()
        if (mapped.personalizations.size != entity.personalizations.size ||
            (response.secondaryGoal != null && mapped.secondaryGoal == null)
        ) {
            throw AppError.Network.IncompleteResponse()
        }
        goalDao.insertOrUpdate(entity)
    }
}
