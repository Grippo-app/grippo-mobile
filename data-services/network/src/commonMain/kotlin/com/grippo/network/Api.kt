package com.grippo.network

import com.grippo.network.client.NetworkClient
import com.grippo.network.dto.AuthBody
import com.grippo.network.dto.EquipmentGroupResponse
import com.grippo.network.dto.EquipmentResponse
import com.grippo.network.dto.ExerciseExampleCriteriaBody
import com.grippo.network.dto.ExerciseExampleFiltersBody
import com.grippo.network.dto.ExerciseExampleResponse
import com.grippo.network.dto.MuscleGroupResponse
import com.grippo.network.dto.MuscleResponse
import com.grippo.network.dto.RefreshBody
import com.grippo.network.dto.RegisterBody
import com.grippo.network.dto.TokenResponse
import com.grippo.network.dto.TrainingResponse
import com.grippo.network.dto.UserResponse
import com.grippo.network.dto.WeightHistoryResponse
import io.ktor.client.call.body
import io.ktor.http.HttpMethod

public class Api(private val client: NetworkClient) {

    /* * * * * * * * * * * * * * * * *
     * Auth service
     * * * * * * * * * * * * * * * * */

    public suspend fun login(body: AuthBody): Result<TokenResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/auth/login",
            body = body
        )
    }

    public suspend fun register(body: RegisterBody): Result<TokenResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/auth/register",
            body = body
        )
    }

    public suspend fun refresh(body: RefreshBody): Result<TokenResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/auth/refresh",
            body = body
        )
    }

    /* * * * * * * * * * * * * * * * *
     * User service
     * * * * * * * * * * * * * * * * */

    public suspend fun getUser(): Result<UserResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/users"
        )
    }

    public suspend fun getExcludedEquipments(): Result<List<EquipmentResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/users/excluded-equipments"
        )
    }

    public suspend fun deleteExcludedEquipment(id: String): Result<Unit> {
        return request(
            method = HttpMethod.Delete,
            path = "users/excluded-equipments/$id"
        )
    }

    public suspend fun setExcludedEquipment(id: String): Result<Unit> {
        return request(
            method = HttpMethod.Post,
            path = "users/excluded-equipments/$id"
        )
    }

    public suspend fun getExcludedMuscles(): Result<List<MuscleResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/users/excluded-muscles"
        )
    }

    public suspend fun deleteExcludedMuscle(id: String): Result<Unit> {
        return request(
            method = HttpMethod.Delete,
            path = "users/excluded-muscles/$id"
        )
    }

    public suspend fun setExcludedMuscle(id: String): Result<Unit> {
        return request(
            method = HttpMethod.Post,
            path = "users/excluded-muscles/$id"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Muscle service
     * * * * * * * * * * * * * * * * */

    public suspend fun getMuscles(): Result<List<MuscleGroupResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/muscles"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Equipment service
     * * * * * * * * * * * * * * * * */

    public suspend fun getEquipments(): Result<List<EquipmentGroupResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/equipments"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Weight history
     * * * * * * * * * * * * * * * * */

    public suspend fun updateWeightHistory(value: Float): Result<WeightHistoryResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/weight-history",
            body = WeightHistoryResponse(weight = value)
        )
    }

    public suspend fun deleteWeight(id: String): Result<Unit> {
        return request(
            method = HttpMethod.Delete,
            path = "/weight-history/${id}",
        )
    }

    public suspend fun getWeightHistory(): Result<List<WeightHistoryResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/weight-history"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Trainings
     * * * * * * * * * * * * * * * * */

    public suspend fun getTrainings(
        startDate: String,
        endDate: String
    ): Result<List<TrainingResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/trainings",
            queryParams = mapOf("start" to startDate, "end" to endDate)
        )
    }

    public suspend fun setTraining(body: TrainingResponse): Result<TrainingResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/trainings",
            body = body
        )
    }

    public suspend fun getTraining(trainingId: String): Result<TrainingResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/trainings/$trainingId"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Exercise examples
     * * * * * * * * * * * * * * * * */

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
    ): Result<List<ExerciseExampleResponse>> {
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
    ): Result<List<ExerciseExampleResponse>> {
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

    public suspend fun setExerciseExample(body: ExerciseExampleResponse): Result<ExerciseExampleResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/exercise-examples",
            body = body
        )
    }

    public suspend fun getExerciseExample(exerciseExampleId: String): Result<ExerciseExampleResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/exercise-examples/$exerciseExampleId"
        )
    }

//    public suspend fun getExerciseExampleFilters(): Result<ExerciseExampleFiltersDto> {
//        return request(
//            method = HttpMethod.Get,
//            path = "/filters"
//        )
//    }
//
//    public suspend fun getExerciseExampleAchievements(
//        exerciseExampleId: String,
//        size: Int
//    ): Result<ExerciseExampleAchievementsDto> {
//        return request(
//            method = HttpMethod.Get,
//            path = "/statistics/achievements/exercise-example",
//            queryParams = mapOf(
//                "id" to exerciseExampleId, "size" to size.toString()
//            ),
//        )
//    }
//
//    public suspend fun getUserMuscleById(id: String): Result<MuscleResponse> {
//        return request(
//            method = HttpMethod.Get,
//            path = "/user-muscles/$id"
//        )
//    }
//
//

    private suspend inline fun <reified T> request(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        queryParams: Map<String, String>? = null
    ): Result<T> {
        return runCatching {
            client.invoke(
                method = method,
                path = path,
                body = body,
                queryParams = queryParams
            ).body()
        }
    }
}