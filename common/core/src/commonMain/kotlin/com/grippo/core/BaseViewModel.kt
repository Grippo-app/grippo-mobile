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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

public abstract class BaseViewModel<STATE, DIRECTION : BaseDirection, LOADER : BaseLoader>(
    state: STATE,
) : InstanceKeeper.Instance, KoinComponent {

    public val coroutineScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val errorProvider by inject<ErrorProvider>()

    private val _state: MutableStateFlow<STATE> = MutableStateFlow(state)
    public val state: StateFlow<STATE> = _state.asStateFlow()

    private val _navigator = Channel<DIRECTION>(Channel.CONFLATED)
    public val navigator: Flow<DIRECTION> = _navigator.receiveAsFlow()

    private val _loaders = MutableStateFlow<ImmutableSet<LOADER>>(persistentSetOf())
    public val loaders: StateFlow<ImmutableSet<LOADER>> = _loaders.asStateFlow()

    protected fun update(updateFunc: (STATE) -> STATE) {
        _state.update { currentState -> updateFunc.invoke(currentState) }
    }

    protected fun navigateTo(destination: DIRECTION) {
        AppLogger.performance(destination.toString())
        coroutineScope.launch { _navigator.send(destination) }
    }

    protected fun safeLaunch(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        loader: LOADER? = null,
        onError: (() -> Unit) = {},
        block: suspend CoroutineScope.() -> Unit,
    ) {
        if (loader != null) {
            _loaders.update { it.toMutableSet().apply { add(loader) }.toPersistentSet() }
        }

        val handler = CoroutineExceptionHandler { _, throwable ->
            sendError(throwable, onError)
        }

        coroutineScope.launch(dispatcher + handler) {
            block.invoke(this)
        }.invokeOnCompletion {
            if (loader != null) {
                _loaders.update { it.toMutableSet().apply { remove(loader) }.toPersistentSet() }
            }
        }
    }

    protected fun <T> Flow<T>.safeLaunch(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        loader: LOADER? = null,
        onError: (() -> Unit) = {},
    ) {
        this.onStart {
            if (loader != null) {
                _loaders.update { it.toMutableSet().apply { add(loader) }.toPersistentSet() }
            }
        }.catch { exception ->
            sendError(
                exception = exception,
                onError = onError,
            )
        }.onCompletion {
            _loaders.update { it.toMutableSet().apply { remove(loader) }.toPersistentSet() }
        }.flowOn(
            context = dispatcher
        ).launchIn(
            scope = coroutineScope
        )
    }

    private fun sendError(exception: Throwable, onError: (() -> Unit)) {
        AppLogger.error("┌───────── ViewModel error ─────────")
        AppLogger.error("│ message: ${exception.message}")
        AppLogger.error("│ cause: ${exception.cause?.message}")
        AppLogger.error("└───────── ViewModel error ─────────")

        errorProvider.provide(exception, callback = onError)
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
