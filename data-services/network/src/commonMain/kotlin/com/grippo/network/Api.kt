package com.grippo.network

import com.grippo.network.client.NetworkClient
import com.grippo.network.dto.auth.AuthBody
import com.grippo.network.dto.auth.RefreshBody
import com.grippo.network.dto.auth.RegisterBody
import com.grippo.network.dto.auth.TokenResponse
import com.grippo.network.dto.equipment.EquipmentGroupResponse
import com.grippo.network.dto.equipment.EquipmentResponse
import com.grippo.network.dto.exercise.example.ExerciseExampleResponse
import com.grippo.network.dto.muscle.MuscleGroupResponse
import com.grippo.network.dto.muscle.MuscleResponse
import com.grippo.network.dto.training.TrainingResponse
import com.grippo.network.user.IdsBody
import com.grippo.network.user.UserResponse
import com.grippo.network.user.WeightHistoryResponse
import io.ktor.client.call.body
import io.ktor.http.HttpMethod
import org.koin.core.annotation.Single

@Single
public class Api internal constructor(private val client: NetworkClient) {

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

    public suspend fun postExcludedEquipments(body: IdsBody): Result<Unit> {
        return request(
            method = HttpMethod.Post,
            path = "/users/excluded-equipments",
            body = body
        )
    }

    public suspend fun getExcludedMuscles(): Result<List<MuscleResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/users/excluded-muscles"
        )
    }

    public suspend fun postExcludedMuscles(body: IdsBody): Result<Unit> {
        return request(
            method = HttpMethod.Post,
            path = "/users/excluded-muscles",
            body = body
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
     * Weight history service
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
     * Trainings service
     * * * * * * * * * * * * * * * * */

    public suspend fun getTrainings(start: String, end: String): Result<List<TrainingResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/trainings",
            queryParams = mapOf("start" to start, "end" to end)
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

    public suspend fun getExerciseExamples(): Result<List<ExerciseExampleResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/exercise-examples",
        )
    }

    public suspend fun getExerciseExample(id: String): Result<ExerciseExampleResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/exercise-examples/$id"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Only admin
     * * * * * * * * * * * * * * * * */

    public suspend fun setExerciseExample(body: ExerciseExampleResponse): Result<ExerciseExampleResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/exercise-examples",
            body = body
        )
    }

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