package com.grippo.authorization.analytics

import com.grippo.data.features.api.authorization.*
import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.*
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.goal.models.*
import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.*
import com.grippo.data.features.api.weight.history.WeightHistoryFeature
import com.grippo.data.features.api.weight.history.models.WeightHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Local domain fake; no Firebase SDK and no credentials in analytics fixtures. */
internal class AuthorizationFixture : AuthorizationFeature, UserFeature, ExcludedMusclesFeature,
    ExcludedEquipmentsFeature, ExerciseExampleFeature, GoalFeature, WeightHistoryFeature {
    enum class PreparationRequest(val call: String) {
        Profile("profile"),
        ExcludedMuscles("excluded_muscles"),
        ExcludedEquipments("excluded_equipments"),
        ExerciseExamples("exercise_examples"),
        WeightHistory("weight_history"),
        Goal("goal"),
        PushFetch("push_fetch"),
        PushRegister("push_register"),
    }

    private val failures = mutableMapOf<PreparationRequest, Exception>()
    var serverError: Exception? = null
    var profileError: Exception?
        get() = failures[PreparationRequest.Profile]
        set(value) {
            if (value == null) failures.remove(PreparationRequest.Profile)
            else failures[PreparationRequest.Profile] = value
        }
    var beforeProfile: suspend () -> Unit = {}
    var beforeWeightHistory: suspend () -> Unit = {}
    var hasProfile = true
    var pushToken: String? = "secret_push"
    var tokenFlow: Flow<String?> = flowOf("secret_session")
    var userFlow: Flow<User?> = flowOf(null)
    val calls = mutableListOf<String>()
    var credential: String? = null
    fun loginUseCase() = LoginUseCase(this, this, this, this, this, this, this)
    fun registerUseCase() = RegisterUseCase(this, this, this, this, this, this, this)
    fun failAt(request: PreparationRequest, error: Exception) {
        failures[request] = error
    }

    private fun <T> result(request: PreparationRequest, value: T): Result<T> =
        failures[request]?.let { Result.failure(it) } ?: Result.success(value)

    private fun <T> request(request: PreparationRequest, value: T): Result<T> {
        calls += request.call
        return result(request, value)
    }

    private fun server(method: String, value: String): Result<Unit> {
        calls += method; credential = value
        return serverError?.let { Result.failure(it) } ?: Result.success(Unit)
    }
    override suspend fun login(email: String, password: String) = server("email", email)
    override suspend fun google(token: String) = server("google", token)
    override suspend fun apple(code: String) = server("apple", code)
    override suspend fun register(email: String, password: String) = server("register", email)
    override fun getToken() = tokenFlow
    override suspend fun getPushToken(): String? {
        calls += PreparationRequest.PushFetch.call
        failures[PreparationRequest.PushFetch]?.let { throw it }
        return pushToken
    }
    override suspend fun updatePushToken(token: String) = request(PreparationRequest.PushRegister, Unit)
    override suspend fun getUser(): Result<Boolean> {
        calls += PreparationRequest.Profile.call
        beforeProfile()
        return result(PreparationRequest.Profile, hasProfile)
    }
    override fun observeUser(): Flow<User?> = userFlow
    override suspend fun getExcludedMuscles() = request(PreparationRequest.ExcludedMuscles, Unit)
    override suspend fun getExcludedEquipments() = request(PreparationRequest.ExcludedEquipments, Unit)
    override suspend fun getExerciseExamples() = request(PreparationRequest.ExerciseExamples, Unit)
    override suspend fun getWeightHistory(): Result<Unit> {
        calls += PreparationRequest.WeightHistory.call
        beforeWeightHistory()
        return result(PreparationRequest.WeightHistory, Unit)
    }
    override suspend fun getGoal() = request(PreparationRequest.Goal, true)
    override suspend fun logout(): Unit = error("Unused fixture call")
    override suspend fun deletePushToken(): Result<Unit> = error("Unused fixture call")
    override suspend fun createProfile(profile: CreateUserProfile): Result<Boolean> = error("Unused fixture call")
    override suspend fun deleteProfile(): Result<Unit> = error("Unused fixture call")
    override suspend fun setExperience(experience: ExperienceEnum): Result<Boolean> = error("Unused fixture call")
    override suspend fun setHeight(height: Int): Result<Boolean> = error("Unused fixture call")
    override fun observeExcludedMuscles(): Flow<List<Muscle>> = error("Unused fixture call")
    override suspend fun setExcludedMuscles(ids: List<String>): Result<Unit> = error("Unused fixture call")
    override fun observeExcludedEquipments(): Flow<List<Equipment>> = error("Unused fixture call")
    override suspend fun setExcludedEquipments(ids: List<String>): Result<Unit> = error("Unused fixture call")
    override fun observeExerciseExamples(name: String?, scope: ExampleScope, sorting: ExampleSortingEnum,
        rules: UserExerciseExampleRules, page: ExamplePage, experience: ExperienceEnum): Flow<List<ExerciseExample>> = error("Unused fixture call")
    override fun observeExerciseExamples(ids: List<String>): Flow<List<ExerciseExample>> = error("Unused fixture call")
    override fun observeExerciseExample(id: String): Flow<ExerciseExample?> = error("Unused fixture call")
    override suspend fun getExerciseExampleById(id: String): Result<Unit> = error("Unused fixture call")
    override fun observeGoal(): Flow<Goal?> = error("Unused fixture call")
    override suspend fun setGoal(goal: SetGoal): Result<Boolean> = error("Unused fixture call")
    override fun observeWeightHistory(): Flow<List<WeightHistory>> = error("Unused fixture call")
    override fun observeLastWeight(): Flow<WeightHistory?> = error("Unused fixture call")
    override suspend fun updateWeight(value: Float): Result<Unit> = error("Unused fixture call")
    override suspend fun deleteWeight(id: String): Result<Unit> = error("Unused fixture call")
}
