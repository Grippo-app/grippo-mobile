package com.grippo.data.features.user.data

import com.grippo.data.features.api.user.models.User
import com.grippo.data.features.api.user.models.WeightHistory
import com.grippo.data.features.user.domain.UserRepository
import com.grippo.database.dao.UserDao
import com.grippo.database.dao.WeightHistoryDao
import com.grippo.database.mapper.toDomain
import com.grippo.network.Api
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class UserRepositoryImpl(
    private val api: Api,
    private val userDao: UserDao,
    private val weightHistoryDao: WeightHistoryDao,
) : UserRepository {

    override fun observeUser(): Flow<User?> {
        return userDao.getUser()
            .map { it?.toDomain() }
    }

    override fun observeWeightHistory(): Flow<List<WeightHistory>> {
        return userDao.getw()
            .map { it?.toDomain() }
    }

    override fun observeLastWeight(): Flow<WeightHistory> {
        TODO("Not yet implemented")
    }

    override fun getUser(): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun getWeightHistory(): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun updateWeight(value: Double): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun removeWeight(id: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun setExcludedMuscle(id: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun deleteExcludedMuscle(id: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun setExcludedEquipment(id: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun deleteExcludedEquipment(id: String): Result<Unit> {
        TODO("Not yet implemented")
    }


}