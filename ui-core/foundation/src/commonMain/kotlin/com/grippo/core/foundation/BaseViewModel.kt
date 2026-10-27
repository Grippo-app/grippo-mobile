package com.grippo.core.foundation

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.grippo.core.error.provider.ErrorDecision
import com.grippo.core.error.provider.ErrorProvider
import com.grippo.core.foundation.analytics.ScreenAnalytics
import com.grippo.core.foundation.internal.operation.OperationManager
import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.foundation.models.BaseLoader
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import com.grippo.toolkit.crashreporting.CrashReporting
import com.grippo.toolkit.logger.AppLogger
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

public abstract class BaseViewModel<STATE, DIRECTION : BaseDirection, LOADER : BaseLoader>(
    state: STATE,
) : InstanceKeeper.Instance, KoinComponent {

    // ------------ STATE API ------------

    private val _state: MutableStateFlow<STATE> = MutableStateFlow(state)
    public val state: StateFlow<STATE> = _state.asStateFlow()

    protected fun update(updateFunc: (STATE) -> STATE) {
        _state.update(updateFunc)
    }

    // ------------ LOADER API ------------

    private val _loaders = MutableStateFlow<ImmutableSet<LOADER>>(persistentSetOf())
    public val loaders: StateFlow<ImmutableSet<LOADER>> = _loaders.asStateFlow()

    protected suspend fun <T> withLoader(
        loader: LOADER?,
        block: suspend () -> T
    ): T {
        addLoader(loader)
        return try {
            block()
        } finally {
            removeLoader(loader)
        }
    }

    private fun addLoader(loader: LOADER?) {
        loader ?: return
        _loaders.update { (it + loader).toPersistentSet() }
    }

    private fun removeLoader(loader: LOADER?) {
        loader ?: return
        _loaders.update { (it - loader).toPersistentSet() }
    }

    // ------------ NAVIGATION API ------------

    private val _navigator = Channel<DIRECTION>(Channel.CONFLATED)
    public val navigator: Flow<DIRECTION> = _navigator.receiveAsFlow()

    protected fun navigateTo(destination: DIRECTION) {
        _navigator.trySend(destination)
    }

    // ------------ COROUTINE API ------------

    protected enum class Processing {
        WhileActive,
        Infinity,
    }

    private val coroutineScope: CoroutineScope = CoroutineScope(
        context = SupervisorJob() + Dispatchers.Main.immediate
    )

    private val operationManager by inject<OperationManager> {
        parametersOf(coroutineScope)
    }

    protected fun <T> Flow<T>.safeLaunch(
        dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        processing: Processing = Processing.WhileActive,
        recovery: ErrorRecovery,
        mapError: (Throwable) -> Throwable = { it },
    ): Job {
        val source = when (processing) {
            Processing.WhileActive -> operationManager.whileActive(this@safeLaunch, activation)
            Processing.Infinity -> this@safeLaunch
        }

        val flow = source
            .retryWhen { cause, _ -> recover(failure = cause, recovery = recovery, mapError = mapError) }
            .catch { cause -> if (cause is CancellationException) throw cause }

        return operationManager.launch(
            dispatcher = dispatcher,
            onError = { exception -> inform(failure = exception, mapError = mapError) },
            block = {
                flow.collect()
                    },
        )
    }

    protected fun safeLaunch(
        dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        loader: LOADER? = null,
        recovery: ErrorRecovery,
        mapError: (Throwable) -> Throwable = { it },
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        return operationManager.launch(
            dispatcher = dispatcher,
            onError = { exception -> inform(failure = exception, mapError = mapError) },
            block = {
                while (true) {
                    try {
                        withLoader(loader) { block() }
                        break
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (failure: Throwable) {
                        if (!recover(failure = failure, recovery = recovery, mapError = mapError)) break
                    }
                }
            },
        )
    }

    // ------------ ACTIVATION API ------------

    private val _activation = MutableStateFlow<Flow<Boolean>?>(null)
    internal val activation: Flow<Boolean> = _activation
        .filterNotNull()
        .flatMapLatest { it }
        .distinctUntilChanged()

    internal fun attachActivation(activationFlow: Flow<Boolean>) {
        _activation.value = activationFlow
            .distinctUntilChanged()
            .onCompletion { emit(false) }
    }

    internal fun detachActivation() {
        _activation.value = null
    }

    // ------------ SCREEN ANALYTICS API ------------

    protected open val screenPresentation: ((NavigationReason) -> ScreenPresentation)? = null

    internal fun bindScreen(lifecycle: Lifecycle) {
        val present = screenPresentation ?: return
        get<ScreenAnalytics>().bind(lifecycle, present)
    }

    // ------------ ERROR API ------------

    private val errorProvider by inject<ErrorProvider>()
    private val crashReporting by inject<CrashReporting>()

    private suspend fun recover(
        failure: Throwable,
        recovery: ErrorRecovery,
        mapError: (Throwable) -> Throwable,
    ): Boolean {
        if (failure is CancellationException) throw failure

        report(failure)

        val error = if (failure is Exception) mapError(failure) else failure

        return when (recovery) {
            ErrorRecovery.Retry -> {
                errorProvider.retry(error) == ErrorDecision.Retry
            }
            is ErrorRecovery.Close -> {
                errorProvider.inform(error)
                recovery.onClose()
                false
            }
        }
    }

    private suspend fun inform(failure: Throwable, mapError: (Throwable) -> Throwable) {
        report(failure)

        val error = if (failure is Exception) mapError(failure) else failure

        errorProvider.inform(error)
    }

    private fun report(failure: Throwable) {
        val log = buildString {
            append("─────── ViewModel error ──────\n")
            append("│ message: ${failure.message}\n")
            append("│ cause: ${failure.cause?.message}\n")
            append("└──────────────────────────")
        }
        AppLogger.General.error(log)
        crashReporting.reportException(failure)
    }

    // ------------ RELEASE API ------------

    override fun onDestroy() {
        _navigator.close()
        coroutineScope.cancel()
    }
}
