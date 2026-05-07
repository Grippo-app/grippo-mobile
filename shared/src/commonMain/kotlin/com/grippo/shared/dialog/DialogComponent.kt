package com.grippo.shared.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.dialog.api.DialogConfig
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

    private val backCallback = BackCallback(onBack = { viewModel.onDismiss(null) })

    private val dialog = SlotNavigation<DialogConfig>()

    internal val childSlot: Value<ChildSlot<DialogConfig, Child>> = childSlot(
        source = dialog,
        serializer = DialogConfig.serializer(),
        key = "DialogComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private val reconcileScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        backHandler.register(backCallback)

        lifecycle.doOnCreate {
            viewModel.state
                .map { ReconcileTarget(it.sessionConfig, it.innerConfigs) }
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
        val active = activeContent() ?: return
        applyInnerStack(active, target.inner)
    }

    private fun applyInnerStack(active: DialogContentComponent, target: List<DialogConfig>) {
        val current = active.childStack.value.items.drop(1).map { it.configuration }
        if (current == target) return

        var commonLen = 0
        while (commonLen < current.size &&
            commonLen < target.size &&
            current[commonLen].matches(target[commonLen])
        ) {
            commonLen++
        }

        repeat(current.size - commonLen) { active.navigation.pop() }
        target.drop(commonLen).forEach { active.navigation.push(it) }
    }

    private fun activeContent(): DialogContentComponent? =
        childSlot.value.child?.instance?.component as? DialogContentComponent

    private fun createChild(router: DialogConfig, context: ComponentContext): Child {
        return Child.Content(
            DialogContentComponent(
                initial = router,
                componentContext = context,
                back = viewModel::onDismiss,
            )
        )
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        DialogScreen(this, state.value, loaders.value, viewModel)
    }

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Content(override val component: DialogContentComponent) : Child(component)
    }

    private data class ReconcileTarget(
        val session: DialogConfig?,
        val inner: List<DialogConfig>,
    )

    private fun DialogConfig.matches(other: DialogConfig): Boolean =
        this::class == other::class && this.key == other.key
}
