package com.grippo.core

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnStart
import com.arkivanov.essenty.lifecycle.doOnStop
import com.grippo.core.internal.ComponentTreeLogger
import com.grippo.core.models.BaseDirection
import com.grippo.core.models.ComponentIdentifier
import com.grippo.core.models.ComponentLifeCycleEvent
import com.grippo.core.models.NoneIdentifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public abstract class BaseComponent<DIRECTION : BaseDirection>(
    componentContext: ComponentContext,
    private val identifier: ComponentIdentifier = NoneIdentifier,
) : ComponentContext by componentContext, KoinComponent {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    protected abstract val viewModel: BaseViewModel<*, DIRECTION>

    private val componentIdentifier by inject<ComponentLifecycleEmitter>()

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
            coroutineScope.cancel()
        }
    }

    protected abstract suspend fun eventListener(rout: DIRECTION)

    @Composable
    public abstract fun Render()
}
