package com.grippo.core

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.grippo.core.models.BaseDirection
import com.grippo.logger.AppLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

public abstract class BaseViewModel<STATE, DIRECTION : BaseDirection>(
    state: STATE,
) : InstanceKeeper.Instance, KoinComponent {

    public val coroutineScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state: MutableStateFlow<STATE> = MutableStateFlow(state)
    public val state: StateFlow<STATE> = _state.asStateFlow()

    private val _navigator = Channel<DIRECTION>(Channel.CONFLATED)
    public val navigator: Flow<DIRECTION> = _navigator.receiveAsFlow()

    protected fun navigateTo(destination: DIRECTION) {
        coroutineScope.launch { _navigator.send(destination) }
    }

    private val handler = CoroutineExceptionHandler { _, exception -> sendError(exception) }

    protected fun sendError(exception: Throwable) {
        exception.message ?: exception.message ?: "Error"
        AppLogger.error("┌───────── ViewModel error ─────────")
        AppLogger.error("│ message: ${exception.message}")
        AppLogger.error("│ cause: ${exception.cause?.message}")
        AppLogger.error("└───────── ViewModel error ─────────")
        // send error
    }

    protected fun update(updateFunc: (STATE) -> STATE) {
        _state.update { currentState -> updateFunc.invoke(currentState) }
    }

    protected fun safeLaunch(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        coroutineScope.launch(dispatcher + handler, block = block)
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
