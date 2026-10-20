package com.grippo.services.backend

import com.grippo.services.backend.client.BackendClient
import com.grippo.services.backend.dto.IdResponse
import com.grippo.services.backend.dto.achievements.AchievementResponse
import com.grippo.services.backend.dto.auth.AppleBody
import com.grippo.services.backend.dto.auth.EmailAuthBody
import com.grippo.services.backend.dto.auth.GoogleBody
import com.grippo.services.backend.dto.auth.RefreshBody
import com.grippo.services.backend.dto.auth.RegisterBody
import com.grippo.services.backend.dto.auth.TokenResponse
import com.grippo.services.backend.dto.equipment.EquipmentGroupResponse
import com.grippo.services.backend.dto.equipment.EquipmentResponse
import com.grippo.services.backend.dto.exercise.example.GetExerciseExampleResponse
import com.grippo.services.backend.dto.muscle.MuscleGroupResponse
import com.grippo.services.backend.dto.muscle.MuscleResponse
import com.grippo.services.backend.dto.push.PushTokenBody
import com.grippo.services.backend.dto.training.ExerciseResponse
import com.grippo.services.backend.dto.training.TrainingBody
import com.grippo.services.backend.dto.training.TrainingResponse
import com.grippo.services.backend.dto.user.CreateProfileBody
import com.grippo.services.backend.dto.user.ExperienceBody
import com.grippo.services.backend.dto.user.GoalResponse
import com.grippo.services.backend.dto.user.HeightBody
import com.grippo.services.backend.dto.user.IdsBody
import com.grippo.services.backend.dto.user.SetGoalBody
import com.grippo.services.backend.dto.user.UserResponse
import com.grippo.services.backend.dto.user.WeightHistoryResponse
import io.ktor.client.call.body
import io.ktor.http.HttpMethod
import org.koin.core.annotation.Single

@Single
public class GrippoApi internal constructor(private val client: BackendClient) {

    /* * * * * * * * * * * * * * * * *
     * Auth service
     * * * * * * * * * * * * * * * * */

    public suspend fun login(body: EmailAuthBody): TokenResponse {
        return request(
            method = HttpMethod.Post,
            path = "/auth/login",
            body = body
        )
    }

    public suspend fun google(body: GoogleBody): TokenResponse {
        return request(
            method = HttpMethod.Post,
            path = "/auth/google",
            body = body,
        )
    }

    public suspend fun apple(body: AppleBody): TokenResponse {
        return request(
            method = HttpMethod.Post,
            path = "/auth/apple",
            body = body,
        )
    }

    public suspend fun register(body: RegisterBody): TokenResponse {
        return request(
            method = HttpMethod.Post,
            path = "/auth/register",
            body = body
        )
    }

    public suspend fun refresh(body: RefreshBody): TokenResponse {
        return request(
            method = HttpMethod.Post,
            path = "/auth/refresh",
            body = body
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Push token service
     * * * * * * * * * * * * * * * * */

    public suspend fun sendPushToken(body: PushTokenBody): Unit {
        return request(
            method = HttpMethod.Post,
            path = "/push-tokens",
            body = body,
        )
    }

    public suspend fun deletePushToken(): Unit {
        return request(
            method = HttpMethod.Delete,
            path = "/push-tokens",
        )
    }

    /* * * * * * * * * * * * * * * * *
     * User service
     * * * * * * * * * * * * * * * * */

    public suspend fun getUser(): UserResponse {
        return request(
            method = HttpMethod.Get,
            path = "/users"
        )
    }

    public suspend fun deleteUser(): Unit {
        return request(
            method = HttpMethod.Delete,
            path = "/users"
        )
    }

    public suspend fun createProfile(body: CreateProfileBody): UserResponse {
        return request(
            method = HttpMethod.Post,
            path = "/users",
            body = body
        )
    }

    public suspend fun updateExperience(body: ExperienceBody): UserResponse {
        return request(
            method = HttpMethod.Post,
            path = "/users/experience",
            body = body
        )
    }

    public suspend fun updateHeight(body: HeightBody): UserResponse {
        return request(
            method = HttpMethod.Post,
            path = "/users/height",
            body = body
        )
    }

    public suspend fun getExcludedEquipments(): List<EquipmentResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/users/excluded-equipments"
        )
    }

    public suspend fun postExcludedEquipments(body: IdsBody): Unit {
        return request(
            method = HttpMethod.Post,
            path = "/users/excluded-equipments",
            body = body
        )
    }

    public suspend fun getExcludedMuscles(): List<MuscleResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/users/excluded-muscles"
        )
    }

    public suspend fun postExcludedMuscles(body: IdsBody): Unit {
        return request(
            method = HttpMethod.Post,
            path = "/users/excluded-muscles",
            body = body
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Muscle service
     * * * * * * * * * * * * * * * * */

    public suspend fun getMuscles(): List<MuscleGroupResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/muscles"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Equipment service
     * * * * * * * * * * * * * * * * */

    public suspend fun getEquipments(): List<EquipmentGroupResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/equipments"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Weight history service
     * * * * * * * * * * * * * * * * */

    public suspend fun updateWeightHistory(value: Float): WeightHistoryResponse {
        return request(
            method = HttpMethod.Post,
            path = "/weight-history",
            body = WeightHistoryResponse(
                weight = value
            )
        )
    }

    public suspend fun deleteWeight(id: String): Unit {
        return request(
            method = HttpMethod.Delete,
            path = "/weight-history/${id}",
        )
    }

    public suspend fun getWeightHistory(): List<WeightHistoryResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/weight-history"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Goal service
     * * * * * * * * * * * * * * * * */

    public suspend fun getGoal(): GoalResponse? {
        return request(
            method = HttpMethod.Get,
            path = "/users/goal"
        )
    }

    public suspend fun setGoal(body: SetGoalBody): GoalResponse {
        return request(
            method = HttpMethod.Post,
            path = "/users/goal",
            body = body
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Trainings service
     * * * * * * * * * * * * * * * * */

    public suspend fun getTrainings(
        start: String,
        end: String
    ): List<TrainingResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/trainings",
            queryParams = mapOf("start" to start, "end" to end)
        )
    }

    public suspend fun setTraining(body: TrainingBody): IdResponse {
        return request(
            method = HttpMethod.Post,
            path = "/trainings",
            body = body
        )
    }

    public suspend fun updateTraining(id: String, body: TrainingBody): Unit {
        return request(
            method = HttpMethod.Put,
            path = "/trainings",
            body = body,
            queryParams = mapOf("id" to id)
        )
    }

    public suspend fun deleteTraining(id: String): Unit {
        return request(
            method = HttpMethod.Delete,
            path = "/trainings/$id"
        )
    }

    public suspend fun getTraining(id: String): TrainingResponse {
        return request(
            method = HttpMethod.Get,
            path = "/trainings/$id"
        )
    }

    /* * * * * * * * * * * * * * * * *
     * Exercise examples
     * * * * * * * * * * * * * * * * */

    public suspend fun getExerciseExamples(locale: String): List<GetExerciseExampleResponse> {
        return request(
            method = HttpMethod.Get,
            path = "/exercise-examples",
            locale = locale,
        )
    }

    public suspend fun getExerciseExample(id: String, locale: String): GetExerciseExampleResponse {
        return request(
            method = HttpMethod.Get,
            path = "/exercise-examples/$id",
            locale = locale,
        )
    }

    public suspend fun getRecentExercisesByExampleId(id: String): List<ExerciseResponse> {
        return request(
            method = HttpMethod.Get,
            path = "exercise-metrics/exercise-example/${id}/recent"
        )
    }

    public suspend fun getAchievementsByExampleId(id: String): AchievementResponse {
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
        queryParams: Map<String, String>? = null,
        locale: String? = null,
    ): T {
        val response = client.invoke(
            method = method,
            path = path,
            body = body,
            queryParams = queryParams,
            locale = locale,
        )
        return response.body<T>()
    }
}
