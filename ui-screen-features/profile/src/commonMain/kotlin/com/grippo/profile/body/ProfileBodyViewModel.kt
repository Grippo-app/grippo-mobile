package com.grippo.profile.body

import com.grippo.analytics.profile.ProfileAnalytics
import com.grippo.analytics.profile.models.BodyPicker
import com.grippo.analytics.profile.models.ProfileEvent
import com.grippo.analytics.profile.models.ProfileOperation
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.data.features.api.weight.history.UpdateWeightUseCase
import com.grippo.data.features.api.weight.history.WeightHistoryFeature
import com.grippo.data.features.api.weight.history.models.WeightHistory
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.height_picker_title
import com.grippo.design.resources.provider.notification_weight_description
import com.grippo.design.resources.provider.notification_weight_title
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.weight_picker_title
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.user.toState
import com.grippo.profile.analytics.ProfileInputMapping
import com.grippo.screen.api.deeplink.Deeplink
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.local.notification.AppNotification
import com.grippo.toolkit.local.notification.NotificationKey
import com.grippo.toolkit.local.notification.NotificationManager
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach

internal class ProfileBodyViewModel(
    private val analytics: ProfileAnalytics,
    private val dialogController: DialogController,
    private val weightHistoryFeature: WeightHistoryFeature,
    private val userFeature: UserFeature,
    private val updateWeightUseCase: UpdateWeightUseCase,
    private val notificationManager: NotificationManager,
    private val stringProvider: StringProvider,
    initialState: ProfileBodyState?,
) : BaseViewModel<ProfileBodyState, ProfileBodyDirection, ProfileBodyLoader>(
    initialState ?: ProfileBodyState()
), ProfileBodyContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(ProfileScreen.Body, reason)
    }

    init {
        safeLaunch {
            analytics.trackOperation(ProfileOperation.RefreshWeightHistory) {
                weightHistoryFeature.getWeightHistory().getOrThrow()
            }
        }

        safeLaunch {
            analytics.trackOperation(ProfileOperation.RefreshUser) {
                userFeature.getUser().getOrThrow()
            }
        }

        userFeature
            .observeUser()
            .onEach(if (initialState == null) ::provideInitialUser else ::provideUser)
            .safeLaunch()

        weightHistoryFeature
            .observeWeightHistory()
            .onEach(::provideWeightHistory)
            .safeLaunch()
    }

    private fun provideWeightHistory(value: List<WeightHistory>) {
        val history = value.toState()
        update { it.copy(history = history) }
    }

    private fun provideUser(value: User?) {
        val user = value?.toState() ?: return
        update { it.copy(user = user) }
    }

    private fun provideInitialUser(value: User?) {
        val user = value?.toState() ?: return
        update {
            it.copy(
                weight = if (it.user == null && it.weight == WeightFormatState.Empty()) {
                    user.weight
                } else {
                    it.weight
                },
                height = if (it.user == null && it.height == HeightFormatState.Empty()) {
                    user.height
                } else {
                    it.height
                },
                user = user,
            )
        }
    }

    override fun onHeightPickerClick() {
        analytics.trackEvent(ProfileEvent.Body.PickerOpened(BodyPicker.Height))
        val dialog = DialogConfig.HeightPicker(
            title = UiText.Res(Res.string.height_picker_title),
            initial = state.value.height.value,
            onResult = { value ->
                val input = HeightFormatState.of(value)
                update { it.copy(height = input) }
                analytics.trackEvent(
                    ProfileEvent.Body.PickerConfirmed(BodyPicker.Height, ProfileInputMapping.status(input))
                )
            }
        )
        dialogController.open(dialog)
    }

    override fun onWeightPickerClick() {
        analytics.trackEvent(ProfileEvent.Body.PickerOpened(BodyPicker.Weight))
        val dialog = DialogConfig.WeightPicker(
            title = UiText.Res(Res.string.weight_picker_title),
            initial = state.value.weight.value,
            onResult = { value ->
                val input = WeightFormatState.of(value)
                update { it.copy(weight = input) }
                analytics.trackEvent(
                    ProfileEvent.Body.PickerConfirmed(BodyPicker.Weight, ProfileInputMapping.status(input))
                )
            }
        )
        dialogController.open(dialog)
    }

    override fun onApplyClick() {
        val weight = state.value.weight.value ?: return
        val height = state.value.height.value ?: return
        val user = state.value.user ?: return

        analytics.trackEvent(ProfileEvent.Body.Apply)
        safeLaunch(
            loader = ProfileBodyLoader.ApplyBodyChanges
        ) {
            analytics.trackOperation(ProfileOperation.SaveBody) {
                val hasNewWeight = user.weight.value != weight
                val hasNewHeight = user.height.value != height

                if (hasNewWeight) {
                    updateWeightUseCase.execute(weight).getOrThrow()
                }

                if (hasNewHeight) {
                    userFeature.setHeight(height).getOrThrow()
                }

                delay(500.milliseconds)

                val notification = AppNotification(
                    id = NotificationKey.ChangeWeight,
                    title = stringProvider.get(Res.string.notification_weight_title),
                    body = stringProvider.get(Res.string.notification_weight_description),
                    deeplink = Deeplink.WeightHistory.key
                )
                notificationManager.show(notification, 7.days)
            }

            navigateTo(ProfileBodyDirection.Back)
        }
    }

    override fun onBack() {
        analytics.trackEvent(ProfileEvent.Body.Back)
        navigateTo(ProfileBodyDirection.Back)
    }
}
