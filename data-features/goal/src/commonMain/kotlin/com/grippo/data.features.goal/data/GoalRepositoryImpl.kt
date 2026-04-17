package com.grippo.data.features.goal.data

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

    override suspend fun getGoal(): Result<Boolean> {
        val userId = userActiveDao.get().firstOrNull() ?: return Result.success(false)
        val response = api.getGoal()

        return response.map { dto ->
            val entity = dto?.toEntityOrNull(userId) ?: return@map false
            goalDao.insertOrUpdate(entity)
            true
        }
    }

    override suspend fun setGoal(goal: SetGoal): Result<Boolean> {
        val userId = userActiveDao.get().firstOrNull() ?: return Result.success(false)
        val response = api.setGoal(goal.toBody())

        return response.map { dto ->
            val entity = dto.toEntityOrNull(userId) ?: return@map false
            goalDao.insertOrUpdate(entity)
            true
        }
    }
}
