package com.grippo.shared.dialog

import androidx.compose.runtime.Composable
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
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.dialog.api.DialogConfig
import com.grippo.shared.dialog.content.DialogContentComponent

internal class DialogComponent(
    componentContext: ComponentContext,
) : BaseComponent<DialogDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        DialogViewModel(dialogProvider = getKoin().get())
    }

    var content: DialogContentComponent? = null

    private val backCallback = BackCallback(onBack = { viewModel.dismiss(null) })

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: DialogDirection) {
        when (direction) {
            is DialogDirection.Activate -> dialog.activate(direction.config)
            is DialogDirection.Dismiss -> dialog.dismiss().also { content = null }
            DialogDirection.Pop -> content?.navigation?.pop()
            is DialogDirection.Push -> content?.navigation?.push(direction.config)
        }
    }

    private val dialog = SlotNavigation<DialogConfig>()

    internal val childSlot: Value<ChildSlot<DialogConfig, Child>> = childSlot(
        source = dialog,
        serializer = DialogConfig.serializer(),
        key = "DialogComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: DialogConfig, context: ComponentContext): Child {
        val component = content ?: DialogContentComponent(
            initial = router,
            componentContext = context,
            back = viewModel::dismiss,
        ).also { content = it }

        return Child.Content(component)
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        DialogScreen(childSlot, state.value, loaders.value, viewModel)
    }

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Content(override val component: DialogContentComponent) :
            Child(component)
    }
}
