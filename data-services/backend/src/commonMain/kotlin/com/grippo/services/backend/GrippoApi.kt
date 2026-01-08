package com.grippo.services.backend

import io.ktor.client.call.body
import io.ktor.http.HttpMethod
import org.koin.core.annotation.Single

@Single
public class GrippoApi internal constructor(private val client: com.grippo.services.backend.client.BackendClient) {

    /* * * * * * * * * * * * * * * * *
     * Auth service
     * * * * * * * * * * * * * * * * */

    public suspend fun login(body: com.grippo.services.backend.dto.auth.EmailAuthBody): Result<com.grippo.services.backend.dto.auth.TokenResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/auth/login",
            body = body
        )
    }

    public suspend fun google(body: com.grippo.services.backend.dto.auth.GoogleBody): Result<com.grippo.services.backend.dto.auth.TokenResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/auth/google",
            body = body,
        )
    }

    public suspend fun register(body: com.grippo.services.backend.dto.auth.RegisterBody): Result<com.grippo.services.backend.dto.auth.TokenResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/auth/register",
            body = body
        )
    }

    public suspend fun refresh(body: com.grippo.services.backend.dto.auth.RefreshBody): Result<com.grippo.services.backend.dto.auth.TokenResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/auth/refresh",
            body = body
        )
    }

    /* * * * * * * * * * * * * * * * *
     * User service
     * * * * * * * * * * * * * * * * */

    public suspend fun getUser(): Result<com.grippo.services.backend.dto.user.UserResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/users"
        )
    }

    public suspend fun deleteUser(): Result<Unit> {
        return request(
            method = HttpMethod.Delete,
            path = "/users"
        )
    }

    public suspend fun createProfile(body: com.grippo.services.backend.dto.user.CreateProfileBody): Result<com.grippo.services.backend.dto.user.UserResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/users",
            body = body
        )
    }

    public suspend fun updateExperience(body: com.grippo.services.backend.dto.user.ExperienceBody): Result<com.grippo.services.backend.dto.user.UserResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/users/experience",
            body = body
        )
    }

    public suspend fun getExcludedEquipments(): Result<List<com.grippo.services.backend.dto.equipment.EquipmentResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/users/excluded-equipments"
        )
    }

    public suspend fun postExcludedEquipments(body: com.grippo.services.backend.dto.user.IdsBody): Result<Unit> {
        return request(
            method = HttpMethod.Post,
            path = "/users/excluded-equipments",
            body = body
        )
    }

    public suspend fun getExcludedMuscles(): Result<List<com.grippo.services.backend.dto.muscle.MuscleResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/users/excluded-muscles"
        )
    }

    public suspend fun postExcludedMuscles(body: com.grippo.services.backend.dto.user.IdsBody): Result<Unit> {
        return request(
            method = HttpMethod.Post,
            path = "/users/excluded-muscles",
            body = body
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Muscle service
     * * * * * * * * * * * * * * * * */

    public suspend fun getMuscles(): Result<List<com.grippo.services.backend.dto.muscle.MuscleGroupResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/muscles"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Equipment service
     * * * * * * * * * * * * * * * * */

    public suspend fun getEquipments(): Result<List<com.grippo.services.backend.dto.equipment.EquipmentGroupResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/equipments"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Weight history service
     * * * * * * * * * * * * * * * * */

    public suspend fun updateWeightHistory(value: Float): Result<com.grippo.services.backend.dto.user.WeightHistoryResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/weight-history",
            body = _root_ide_package_.com.grippo.services.backend.dto.user.WeightHistoryResponse(
                weight = value
            )
        )
    }

    public suspend fun deleteWeight(id: String): Result<Unit> {
        return request(
            method = HttpMethod.Delete,
            path = "/weight-history/${id}",
        )
    }

    public suspend fun getWeightHistory(): Result<List<com.grippo.services.backend.dto.user.WeightHistoryResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/weight-history"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Trainings service
     * * * * * * * * * * * * * * * * */

    public suspend fun getTrainings(
        start: String,
        end: String
    ): Result<List<com.grippo.services.backend.dto.training.TrainingResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/trainings",
            queryParams = mapOf("start" to start, "end" to end)
        )
    }

    public suspend fun setTraining(body: com.grippo.services.backend.dto.training.TrainingBody): Result<com.grippo.services.backend.dto.IdResponse> {
        return request(
            method = HttpMethod.Post,
            path = "/trainings",
            body = body
        )
    }

    public suspend fun updateTraining(
        id: String,
        body: com.grippo.services.backend.dto.training.TrainingBody
    ): Result<Unit> {
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

    public suspend fun getTraining(id: String): Result<com.grippo.services.backend.dto.training.TrainingResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/trainings/$id"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Exercise examples
     * * * * * * * * * * * * * * * * */

    public suspend fun getExerciseExamples(): Result<List<com.grippo.services.backend.dto.exercise.example.GetExerciseExampleResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "/exercise-examples",
        )
    }

    public suspend fun getExerciseExample(id: String): Result<com.grippo.services.backend.dto.exercise.example.GetExerciseExampleResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/exercise-examples/$id"
        )
    }

    public suspend fun getRecentExercisesByExampleId(id: String): Result<List<com.grippo.services.backend.dto.training.ExerciseResponse>> {
        return request(
            method = HttpMethod.Get,
            path = "exercise-metrics/exercise-example/${id}/recent"
        )
    }

    public suspend fun getAchievementsByExampleId(id: String): Result<com.grippo.services.backend.dto.achievements.AchievementResponse> {
        return request(
            method = HttpMethod.Get,
            path = "exercise-metrics/exercise-example/${id}/achievements"
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
