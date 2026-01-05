package com.grippo.core.foundation

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.grippo.core.error.provider.ErrorProvider
import com.grippo.core.foundation.internal.operation.OperationManager
import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.foundation.models.BaseLoader
import com.grippo.toolkit.logger.AppLogger
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

public abstract class BaseViewModel<STATE, DIRECTION : BaseDirection, LOADER : BaseLoader>(
    state: STATE,
) : InstanceKeeper.Instance, KoinComponent {

    // ------------ STATE API ------------

    private val _state: MutableStateFlow<STATE> = MutableStateFlow(state)
    public val state: StateFlow<STATE> = _state.asStateFlow()

    protected fun update(updateFunc: (STATE) -> STATE) {
        _state.update { currentState -> updateFunc.invoke(currentState) }
    }

    // ------------ LOADER API ------------

    private val _loaders = MutableStateFlow<ImmutableSet<LOADER>>(persistentSetOf())
    public val loaders: StateFlow<ImmutableSet<LOADER>> = _loaders.asStateFlow()

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

    protected enum class Processing { WhileActive, Infinity }

    private val coroutineScope: CoroutineScope = CoroutineScope(
        context = SupervisorJob() + Dispatchers.Default
    )

    private val operationManager by inject<OperationManager> {
        parametersOf(coroutineScope)
    }

    protected fun <T> Flow<T>.safeLaunch(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        processing: Processing = Processing.WhileActive,
        loader: LOADER? = null,
        onError: (() -> Unit) = {},
    ): Job {
        addLoader(loader)

        val flow = when (processing) {
            Processing.WhileActive -> operationManager.whileActive(this@safeLaunch, activation)
            Processing.Infinity -> this@safeLaunch
        }

        val job = operationManager.launch(
            dispatcher = dispatcher,
            onError = { t -> sendError(t, onError) },
            block = {
                flow
                    .onEach { removeLoader(loader) }
                    .collect()
            }
        )

        // final cleanup regardless of path (no-op if already removed)
        job.invokeOnCompletion { removeLoader(loader) }
        return job
    }

    protected fun safeLaunch(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        processing: Processing = Processing.Infinity,
        loader: LOADER? = null,
        onError: (() -> Unit) = {},
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        addLoader(loader)

        val job = operationManager.launch(
            dispatcher = dispatcher,
            onError = { t -> sendError(t, onError) }
        ) {
            when (processing) {
                Processing.WhileActive,
                Processing.Infinity -> block()
            }
        }

        job.invokeOnCompletion { removeLoader(loader) }
        return job
    }

    // ------------ ACTIVATION API ------------

    private val _activation = MutableStateFlow(flowOf(false))
    internal val activation: Flow<Boolean> = _activation.flatMapLatest { it }.distinctUntilChanged()

    internal fun attachActivation(activationFlow: Flow<Boolean>) {
        _activation.value = activationFlow
            .distinctUntilChanged()
            .onCompletion { emit(false) }
    }

    internal fun detachActivation() {
        _activation.value = flowOf(false)
    }

    // ------------ ERROR API ------------

    private val errorProvider by inject<ErrorProvider>()

    private suspend fun sendError(exception: Throwable, onError: (() -> Unit)) {
        val log = buildString {
            append("─────── ViewModel error ──────\n")
            append("│ message: ${exception.message}\n")
            append("│ cause: ${exception.cause?.message}\n")
            append("└──────────────────────────")
        }
        AppLogger.General.error(log)
        errorProvider.provide(exception, callback = onError)
    }

    // ------------ RELEASE API ------------

    override fun onDestroy() {
        _navigator.close()
        coroutineScope.cancel()
    }
}
