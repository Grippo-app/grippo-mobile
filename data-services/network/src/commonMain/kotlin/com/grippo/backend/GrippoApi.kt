package com.grippo.backend

import com.grippo.backend.client.GrippoClient
import com.grippo.backend.dto.IdResponse
import com.grippo.backend.dto.auth.AuthBody
import com.grippo.backend.dto.auth.RefreshBody
import com.grippo.backend.dto.auth.RegisterBody
import com.grippo.backend.dto.auth.TokenResponse
import com.grippo.backend.dto.equipment.EquipmentGroupResponse
import com.grippo.backend.dto.equipment.EquipmentResponse
import com.grippo.backend.dto.exercise.example.GetExerciseExampleResponse
import com.grippo.backend.dto.muscle.MuscleGroupResponse
import com.grippo.backend.dto.muscle.MuscleResponse
import com.grippo.backend.dto.training.TrainingBody
import com.grippo.backend.dto.training.TrainingResponse
import com.grippo.backend.dto.user.IdsBody
import com.grippo.backend.dto.user.UserResponse
import com.grippo.backend.dto.user.WeightHistoryResponse
import io.ktor.client.call.body
import io.ktor.http.HttpMethod
import org.koin.core.annotation.Single

@Single
public class GrippoApi internal constructor(private val client: GrippoClient) {

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

    public suspend fun setTraining(body: TrainingBody): Result<IdResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/trainings",
            body = body
        )
    }

    public suspend fun updateTraining(id: String, body: TrainingBody): Result<Unit> {
        return request(
            method = HttpMethod.Put,
            path = "/trainings",
            body = body,
            queryParams = mapOf("id" to id)
        )
    }

    public suspend fun deleteTraining(id: String): Result<Unit> {
        return request(
            method = HttpMethod.Delete,
            path = "/trainings/$id"
        )
    }

    public suspend fun getTraining(id: String): Result<TrainingResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/trainings/$id"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Exercise examples
     * * * * * * * * * * * * * * * * */

    public suspend fun getExerciseExamples(): Result<List<GetExerciseExampleResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/exercise-examples",
        )
    }

    public suspend fun getExerciseExample(id: String): Result<GetExerciseExampleResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/exercise-examples/$id"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Utilities
     * * * * * * * * * * * * * * * * */

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