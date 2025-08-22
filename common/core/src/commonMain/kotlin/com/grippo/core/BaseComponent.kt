package com.grippo.core

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnStart
import com.arkivanov.essenty.lifecycle.doOnStop
import com.grippo.core.internal.ComponentTreeLogger
import com.grippo.core.internal.ResultEmitter
import com.grippo.core.models.BaseDirection
import com.grippo.core.models.BaseResult
import com.grippo.core.models.ComponentIdentifier
import com.grippo.core.models.ComponentLifeCycleEvent
import com.grippo.core.models.NoneIdentifier
import com.grippo.core.models.Result
import com.grippo.core.models.ResultKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public abstract class BaseComponent<DIRECTION : BaseDirection>(
    componentContext: ComponentContext,
    private val identifier: ComponentIdentifier = NoneIdentifier,
) : ComponentContext by componentContext, KoinComponent {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val resultEmitter by inject<ResultEmitter>()
    private val activeResultSubscriptions = mutableListOf<Job>()
    private val componentIdentifier by inject<ComponentLifecycleEmitter>()

    protected abstract val viewModel: BaseViewModel<*, DIRECTION, *>

    init {
        lifecycle.doOnStart {
            componentIdentifier.invoke(identifier, ComponentLifeCycleEvent.OnStart)
        }

        lifecycle.doOnStop {
            componentIdentifier.invoke(identifier, ComponentLifeCycleEvent.OnStop)
        }

        lifecycle.doOnCreate {
            ComponentTreeLogger.logLifecycleEvent(
                ComponentLifeCycleEvent.OnCreate,
                this::class.simpleName
            )

            viewModel.navigator
                .onEach(::eventListener)
                .launchIn(coroutineScope)
        }

        lifecycle.doOnDestroy {
            ComponentTreeLogger.logLifecycleEvent(
                ComponentLifeCycleEvent.OnDestroy,
                this::class.simpleName
            )
            activeResultSubscriptions.forEach { it.cancel() }
            activeResultSubscriptions.clear()
            coroutineScope.cancel()
        }
    }

    protected abstract suspend fun eventListener(direction: DIRECTION)

    protected fun <T : BaseResult> subscribeToResult(
        key: ResultKey<T>,
        onResult: suspend (T) -> Unit
    ) {
        val job = resultEmitter.results
            .filter { (resultKey, _) -> resultKey == key.key }
            .onEach { (_, result) -> @Suppress("UNCHECKED_CAST") onResult(result as T) }
            .launchIn(coroutineScope)

        activeResultSubscriptions.add(job)
    }

    protected fun <T : Any> sendResult(key: ResultKey<Result<T>>, data: T) {
        resultEmitter.sendResult(key, data)
    }

    @Composable
    public abstract fun Render()
}
