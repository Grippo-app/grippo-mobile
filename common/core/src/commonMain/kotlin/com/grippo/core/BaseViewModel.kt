package com.grippo.core

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.grippo.core.models.BaseDirection
import com.grippo.core.models.BaseLoader
import com.grippo.error.provider.ErrorProvider
import com.grippo.logger.AppLogger
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.cancellation.CancellationException

public abstract class BaseViewModel<STATE, DIRECTION : BaseDirection, LOADER : BaseLoader>(
    state: STATE,
) : InstanceKeeper.Instance, KoinComponent {

    public val coroutineScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val errorProvider by inject<ErrorProvider>()

    private val _state: MutableStateFlow<STATE> = MutableStateFlow(state)
    public val state: StateFlow<STATE> = _state.asStateFlow()

    private val _navigator = Channel<DIRECTION>(Channel.BUFFERED)
    public val navigator: Flow<DIRECTION> = _navigator.receiveAsFlow()

    private val _loaders = MutableStateFlow<ImmutableSet<LOADER>>(persistentSetOf())
    public val loaders: StateFlow<ImmutableSet<LOADER>> = _loaders.asStateFlow()

    protected fun update(updateFunc: (STATE) -> STATE) {
        _state.update { currentState -> updateFunc.invoke(currentState) }
    }

    protected fun navigateTo(destination: DIRECTION) {
        _navigator.trySend(destination)
    }

    protected fun safeLaunch(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        loader: LOADER? = null,
        onError: (() -> Unit) = {},
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        addLoader(loader)

        val job = coroutineScope.launch(dispatcher) {
            try {
                block()
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                sendError(t, onError)
            }
        }

        job.invokeOnCompletion { removeLoader(loader) }
        return job
    }

    protected fun <T> Flow<T>.safeLaunch(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        loader: LOADER? = null,
        onError: (() -> Unit) = {},
    ): Job {
        return this
            .flowOn(dispatcher)
            .onStart { addLoader(loader) }
            .catch { e -> if (e !is CancellationException) sendError(e, onError) else throw e }
            .onCompletion { removeLoader(loader) }
            .launchIn(coroutineScope)
    }

    private fun addLoader(loader: LOADER?) {
        loader ?: return
        _loaders.update { (it + loader).toPersistentSet() }
    }

    private fun removeLoader(loader: LOADER?) {
        loader ?: return
        _loaders.update { (it - loader).toPersistentSet() }
    }

    private suspend fun sendError(exception: Throwable, onError: (() -> Unit)) {
        AppLogger.General.error("┌───────── ViewModel error ─────────")
        AppLogger.General.error("│ message: ${exception.message}")
        AppLogger.General.error("│ cause: ${exception.cause?.message}")
        AppLogger.General.error("│ stacktrace: ${exception.stackTraceToString()}")
        AppLogger.General.error("└───────────────────────────────────")
        errorProvider.provide(exception, callback = onError)
    }

    override fun onDestroy() {
        _navigator.close()
        coroutineScope.cancel()
    }
}