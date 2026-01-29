package com.grippo.authorization.profile.creation.user

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class UserComponent(
    componentContext: ComponentContext,
    private val toExperience: (name: String, weight: Float, height: Int) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<UserDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        UserViewModel(
            dialogController = getKoin().get(),
            authorizationFeature = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: UserDirection) {
        when (direction) {
            is UserDirection.Experience -> toExperience.invoke(
                direction.name,
                direction.weight,
                direction.height
            )

            UserDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        UserScreen(state.value, loaders.value, viewModel)
    }
}