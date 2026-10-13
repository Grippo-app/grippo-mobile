package com.grippo.analytics.profile.internal.schema

import com.grippo.analytics.common.models.InputStatus
import com.grippo.analytics.profile.internal.encoding.ProfileEncoding
import com.grippo.analytics.profile.internal.events.ProfilePayloadEvent
import com.grippo.analytics.profile.internal.models.InputField
import com.grippo.analytics.profile.internal.models.InteractionAction
import com.grippo.analytics.profile.models.ProfileEvent
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter
import com.grippo.toolkit.analytics.values.AnalyticsCount

/** Exhaustive mapping of closed observations to explicit wire schemas. */
internal object ProfileEventMatrix {
    fun event(event: ProfileEvent): AnalyticsEvent = when (event) {
        is ProfileEvent.MenuSelected -> payload(
            ProfileInteractionSchema.MenuSelected,
            ProfileScreen.Menu,
            ProfileInteractionSchema.Target.value(event.target),
        )
        ProfileEvent.Body.Apply -> clicked(ProfileScreen.Body, InteractionAction.Apply)
        ProfileEvent.Body.DismissRequested -> clicked(ProfileScreen.Body, InteractionAction.DismissRequested)
        is ProfileEvent.Body.PickerOpened -> pickerOpened(ProfileScreen.Body, ProfileEncoding.field(event.field))
        is ProfileEvent.Body.PickerConfirmed -> pickerConfirmed(
            ProfileScreen.Body,
            ProfileEncoding.field(event.field),
            event.status,
        )
        ProfileEvent.Goal.Save -> clicked(ProfileScreen.Goal, InteractionAction.Save)
        ProfileEvent.Goal.DismissRequested -> clicked(ProfileScreen.Goal, InteractionAction.DismissRequested)
        is ProfileEvent.Goal.PickerOpened -> pickerOpened(ProfileScreen.Goal, ProfileEncoding.field(event.field))
        ProfileEvent.Goal.PrimaryConfirmed -> pickerConfirmed(
            ProfileScreen.Goal,
            InputField.Primary,
            InputStatus.Valid,
        )
        ProfileEvent.Goal.SecondaryConfirmed -> pickerConfirmed(
            ProfileScreen.Goal,
            InputField.Secondary,
            InputStatus.Valid,
        )
        is ProfileEvent.Goal.TargetDateConfirmed -> pickerConfirmed(
            ProfileScreen.Goal,
            InputField.TargetDate,
            event.status,
        )
        ProfileEvent.Experience.Apply -> clicked(ProfileScreen.Experience, InteractionAction.Apply)
        ProfileEvent.Experience.DismissRequested -> clicked(ProfileScreen.Experience, InteractionAction.DismissRequested)
        ProfileEvent.Muscles.GroupSelected -> clicked(ProfileScreen.Muscles, InteractionAction.GroupSelected)
        ProfileEvent.Muscles.Apply -> clicked(ProfileScreen.Muscles, InteractionAction.Apply)
        ProfileEvent.Muscles.DismissRequested -> clicked(ProfileScreen.Muscles, InteractionAction.DismissRequested)
        is ProfileEvent.Muscles.SelectionChanged -> selection(
            ProfileInteractionSchema.SelectionChanged,
            ProfileScreen.Muscles,
            event.selected,
            event.selectedCount,
        )
        ProfileEvent.Equipments.GroupSelected -> clicked(ProfileScreen.Equipments, InteractionAction.GroupSelected)
        ProfileEvent.Equipments.Apply -> clicked(ProfileScreen.Equipments, InteractionAction.Apply)
        ProfileEvent.Equipments.DismissRequested -> clicked(ProfileScreen.Equipments, InteractionAction.DismissRequested)
        is ProfileEvent.Equipments.SelectionChanged -> selection(
            ProfileInteractionSchema.SelectionChanged,
            ProfileScreen.Equipments,
            event.selected,
            event.selectedCount,
        )
        ProfileEvent.Settings.Logout -> clicked(ProfileScreen.Settings, InteractionAction.Logout)
        ProfileEvent.Settings.DeleteOpened -> clicked(ProfileScreen.Settings, InteractionAction.DeleteOpened)
        ProfileEvent.Settings.DeleteConfirmed -> clicked(ProfileScreen.Settings, InteractionAction.DeleteConfirmed)
        ProfileEvent.Settings.DismissRequested -> clicked(ProfileScreen.Settings, InteractionAction.DismissRequested)
        ProfileEvent.Social.DismissRequested -> clicked(ProfileScreen.Social, InteractionAction.DismissRequested)
        is ProfileEvent.Goal.PersonalizationChanged -> selection(
            ProfileInteractionSchema.PersonalizationChanged,
            ProfileScreen.Goal,
            event.selected,
            event.selectedCount,
        )
        is ProfileEvent.Experience.Selected -> payload(
            ProfileInteractionSchema.ExperienceSelected,
            ProfileScreen.Experience,
            ProfileInteractionSchema.Experience.value(event.value),
        )
        is ProfileEvent.Social.LinkClicked -> payload(
            ProfileInteractionSchema.LinkClicked,
            ProfileScreen.Social,
            ProfileInteractionSchema.Channel.value(event.channel),
        )
        is ProfileEvent.Social.LinkResult -> payload(
            ProfileInteractionSchema.LinkResult,
            ProfileScreen.Social,
            ProfileInteractionSchema.Channel.value(event.channel),
            ProfileInteractionSchema.Opened.value(event.opened),
        )
    }

    private fun clicked(screen: ProfileScreen, action: InteractionAction): AnalyticsEvent =
        payload(ProfileInteractionSchema.Clicked, screen, ProfileInteractionSchema.Action.value(action))

    private fun pickerOpened(screen: ProfileScreen, field: InputField): AnalyticsEvent =
        payload(ProfileInteractionSchema.PickerOpened, screen, ProfileInteractionSchema.Field.value(field))

    private fun pickerConfirmed(screen: ProfileScreen, field: InputField, status: InputStatus): AnalyticsEvent =
        payload(
            ProfileInteractionSchema.PickerConfirmed,
            screen,
            ProfileInteractionSchema.Field.value(field),
            ProfileInteractionSchema.Status.value(status),
        )

    private fun selection(
        schema: AnalyticsEventSchema,
        screen: ProfileScreen,
        selected: Boolean,
        count: Int,
    ): AnalyticsEvent = payload(
        schema,
        screen,
        ProfileInteractionSchema.Selected.value(selected),
        ProfileInteractionSchema.Count.value(AnalyticsCount(count.toLong())),
    )

    private fun payload(
        schema: AnalyticsEventSchema,
        screen: ProfileScreen,
        vararg parameters: AnalyticsParameter,
    ): AnalyticsEvent {
        return ProfilePayloadEvent(schema, buildList(parameters.size + 1) {
            add(ProfileInteractionSchema.Screen.value(screen))
            addAll(parameters)
        })
    }
}
