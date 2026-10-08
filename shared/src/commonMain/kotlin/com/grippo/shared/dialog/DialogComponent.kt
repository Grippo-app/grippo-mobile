package com.grippo.shared.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.shared.dialog.content.DialogContentComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@Immutable
internal class DialogComponent(
    componentContext: ComponentContext,
) : BaseComponent<DialogDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        DialogViewModel(dialogProvider = getKoin().get())
    }

    private val backCallback = BackCallback(
        isEnabled = viewModel.state.value.phase == SheetPhase.Present,
        onBack = { viewModel.onDismiss(null) },
    )

    private val dialog = SlotNavigation<DialogSession>()

    internal val childSlot: Value<ChildSlot<DialogSession, DialogContentComponent>> = childSlot(
        source = dialog,
        serializer = null, // Sessions own callbacks and must not survive process death.
        initialConfiguration = { viewModel.state.value.session },
        key = "DialogComponent.v3",
        handleBackButton = false,
        childFactory = ::createChild,
    )

    private val reconcileScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        backHandler.register(backCallback)
        lifecycle.doOnCreate {
            viewModel.state
                .onEach { backCallback.isEnabled = it.phase == SheetPhase.Present }
                .map { ReconcileTarget(it.session, it.stack.map { entry -> entry.asStep() }) }
                .distinctUntilChanged()
                .onEach(::reconcile)
                .launchIn(reconcileScope)
        }

        lifecycle.doOnDestroy {
            reconcileScope.cancel()
        }
    }

    override suspend fun eventListener(direction: DialogDirection) {
        // No directions emitted — reconciliation is state-driven via state.flow.
    }

    private fun reconcile(target: ReconcileTarget) {
        val currentSession = childSlot.value.child?.configuration

        when {
            target.session == null && currentSession != null -> {
                dialog.dismiss()
                return
            }

            target.session != null && currentSession != target.session -> {
                dialog.activate(target.session)
            }
        }

        if (target.session == null) return
        val active = childSlot.value.child?.instance ?: return
        applyStack(active, target.steps)
    }

    private fun applyStack(active: DialogContentComponent, target: List<DialogStep>) {
        val current = active.childStack.value.items.map { it.configuration }
        if (current == target || target.isEmpty()) return
        active.navigation.replaceAll(*target.toTypedArray())
    }

    private fun createChild(router: DialogSession, context: ComponentContext): DialogContentComponent =
        DialogContentComponent(
            initial = viewModel.state.value.stack.first().asStep(),
            navigationFor = { owner -> viewModel.navigation(router, owner) },
            componentContext = context,
        )

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        DialogScreen(this, state.value, loaders.value, viewModel)
    }

    private data class ReconcileTarget(
        val session: DialogSession?,
        val steps: List<DialogStep>,
    )
}
