package com.grippo.network

import com.grippo.network.client.NetworkClient
import com.grippo.network.dto.AuthDto
import com.grippo.network.dto.EquipmentGroupDto
import com.grippo.network.dto.ExcludedEquipmentDto
import com.grippo.network.dto.ExcludedMuscleDto
import com.grippo.network.dto.ExerciseExampleAchievementsDto
import com.grippo.network.dto.ExerciseExampleCriteriaBody
import com.grippo.network.dto.ExerciseExampleDto
import com.grippo.network.dto.ExerciseExampleFiltersBody
import com.grippo.network.dto.ExerciseExampleFiltersDto
import com.grippo.network.dto.MuscleDto
import com.grippo.network.dto.MuscleGroupDto
import com.grippo.network.dto.RegisterDto
import com.grippo.network.dto.TokenDto
import com.grippo.network.dto.TrainingDto
import com.grippo.network.dto.UserDto
import com.grippo.network.dto.WeightHistoryDto
import io.ktor.client.call.body
import io.ktor.http.HttpMethod

public class Api(private val client: NetworkClient) {

    public suspend fun login(body: AuthDto): TokenDto {
        return request(
            method = HttpMethod.Post,
            path = "/auth/login",
            body = body
        )
    }

    public suspend fun register(body: RegisterDto): TokenDto {
        return request(
            method = HttpMethod.Post,
            path = "/auth/register",
            body = body
        )
    }

    public suspend fun getTrainings(startDate: String, endDate: String): List<TrainingDto> {
        return request(
            method = HttpMethod.Get,
            path = "/trainings",
            queryParams = mapOf(
                "start" to startDate, "end" to endDate
            )
        )
    }

    public suspend fun setTraining(body: TrainingDto): TrainingDto {
        return request(
            method = HttpMethod.Post,
            path = "/trainings",
            body = body
        )
    }

    public suspend fun getTraining(trainingId: String): TrainingDto {
        return request(
            method = HttpMethod.Get,
            path = "/trainings/$trainingId"
        )
    }

    public suspend fun getExerciseExamples(
        page: Int,
        size: Int,
        query: String?,
        weightType: String?,
        forceType: String?,
        experience: String?,
        category: String?,
        muscleIds: List<String>,
        equipmentIds: List<String>
    ): List<ExerciseExampleDto> {
        return request(
            method = HttpMethod.Post,
            path = "/exercise-examples/all",
            queryParams = buildMap {
                put("page", page.toString())
                put("size", size.toString())
            },
            body = ExerciseExampleFiltersBody(
                category = category,
                equipmentIds = equipmentIds,
                experience = experience,
                forceType = forceType,
                muscleIds = muscleIds,
                query = query,
                weightType = weightType
            )
        )
    }

    public suspend fun getRecommendedExerciseExamples(
        page: Int,
        size: Int,
        exerciseCount: Int?,
        targetMuscleId: String?,
        exerciseExampleIds: List<String>
    ): List<ExerciseExampleDto> {
        return request(
            method = HttpMethod.Post,
            path = "/exercise-examples/recommended",
            queryParams = buildMap {
                put("page", page.toString())
                put("size", size.toString())
            },
            body = ExerciseExampleCriteriaBody(
                exerciseCount = exerciseCount,
                exerciseExampleIds = exerciseExampleIds,
                targetMuscleId = targetMuscleId
            )
        )
    }

    public suspend fun getExerciseExampleFilters(): ExerciseExampleFiltersDto {
        return request(
            method = HttpMethod.Get,
            path = "/filters"
        )
    }

    public suspend fun setExerciseExample(body: ExerciseExampleDto): ExerciseExampleDto {
        return request(
            method = HttpMethod.Post,
            path = "/exercise-examples",
            body = body
        )
    }

    public suspend fun getExerciseExample(exerciseExampleId: String): ExerciseExampleDto {
        return request(
            method = HttpMethod.Get,
            path = "/exercise-examples/$exerciseExampleId"
        )
    }

    public suspend fun getExerciseExampleAchievements(
        exerciseExampleId: String,
        size: Int
    ): ExerciseExampleAchievementsDto {
        return request(
            method = HttpMethod.Get,
            path = "/statistics/achievements/exercise-example",
            queryParams = mapOf(
                "id" to exerciseExampleId, "size" to size.toString()
            ),
        )
    }

    public suspend fun getUserMuscles(): List<MuscleGroupDto> {
        return request(
            method = HttpMethod.Get,
            path = "/user-muscles"
        )
    }

    public suspend fun getPublicMuscles(): List<MuscleGroupDto> {
        return request(
            method = HttpMethod.Get,
            path = "/public-muscles"
        )
    }

    public suspend fun getUserEquipments(): List<EquipmentGroupDto> {
        return request(
            method = HttpMethod.Get,
            path = "/user-equipments"
        )
    }

    public suspend fun getPublicEquipments(): List<EquipmentGroupDto> {
        return request(
            method = HttpMethod.Get,
            path = "/public-equipments"
        )
    }

    public suspend fun getUserMuscleById(id: String): MuscleDto {
        return request(
            method = HttpMethod.Get,
            path = "/user-muscles/$id"
        )
    }

    public suspend fun getUser(): UserDto {
        return request(
            method = HttpMethod.Get,
            path = "/users/profile"
        )
    }

    public suspend fun deleteExcludedMuscle(id: String): ExcludedMuscleDto {
        return request(
            method = HttpMethod.Delete,
            path = "/excluded-muscles/$id"
        )
    }

    public suspend fun setExcludedMuscle(id: String): ExcludedMuscleDto {
        return request(
            method = HttpMethod.Post,
            path = "/excluded-muscles/$id"
        )
    }

    public suspend fun deleteExcludedEquipment(id: String): ExcludedEquipmentDto {
        return request(
            method = HttpMethod.Delete,
            path = "/excluded-equipments/$id"
        )
    }

    public suspend fun setExcludedEquipment(id: String): ExcludedEquipmentDto {
        return request(
            method = HttpMethod.Post,
            path = "/excluded-equipments/$id"
        )
    }

    public suspend fun updateWeightHistory(value: Double): WeightHistoryDto {
        return request(
            method = HttpMethod.Post,
            path = "/weight-history",
            body = WeightHistoryDto(weight = value)
        )
    }

    public suspend fun removeWeight(id: String) {
        return request(
            method = HttpMethod.Delete,
            path = "/weight-history/${id}",
        )
    }

    public suspend fun getWeightHistory(): List<WeightHistoryDto> {
        return request(
            method = HttpMethod.Get,
            path = "/weight-history"
        )
    }

    private suspend inline fun <reified T> request(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        queryParams: Map<String, String>? = null
    ): T {
        return client.invoke(
            method = method,
            path = path,
            body = body,
            queryParams = queryParams
        ).body()
    }
}