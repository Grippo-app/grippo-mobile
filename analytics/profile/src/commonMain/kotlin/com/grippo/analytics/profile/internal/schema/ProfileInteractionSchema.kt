package com.grippo.analytics.profile.internal.schema

import com.grippo.analytics.common.models.InputStatus
import com.grippo.analytics.profile.internal.encoding.ProfileEncoding
import com.grippo.analytics.profile.internal.models.InputField
import com.grippo.analytics.profile.internal.models.InteractionAction
import com.grippo.analytics.profile.models.ExperienceLevel
import com.grippo.analytics.profile.models.ProfileMenuTarget
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.analytics.profile.models.SocialChannel
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object ProfileInteractionSchema {
    val Screen = AnalyticsParameterKey.code<ProfileScreen>("interaction_screen", encode = ProfileEncoding::code)
    val Action = AnalyticsParameterKey.code<InteractionAction>("interaction_action", encode = ProfileEncoding::code)
    val Field = AnalyticsParameterKey.code<InputField>("input_field", encode = ProfileEncoding::code)
    val Status = AnalyticsParameterKey.code<InputStatus>("input_status", encode = ProfileEncoding::code)
    val Target = AnalyticsParameterKey.code<ProfileMenuTarget>("menu_target", encode = ProfileEncoding::code)
    val Experience = AnalyticsParameterKey.code<ExperienceLevel>("profile_experience", encode = ProfileEncoding::code)
    val Channel = AnalyticsParameterKey.code<SocialChannel>("social_channel", encode = ProfileEncoding::code)
    val Selected = AnalyticsParameterKey.flag("selected")
    val Count = AnalyticsParameterKey.count("selected_count")
    val Opened = AnalyticsParameterKey.flag("opened")

    val Clicked = AnalyticsEventSchema("profile_control_clicked", 1, listOf(Screen, Action))
    val MenuSelected = AnalyticsEventSchema("profile_menu_selected", 1, listOf(Screen, Target))
    val PickerOpened = AnalyticsEventSchema("profile_picker_opened", 1, listOf(Screen, Field))
    val PickerConfirmed = AnalyticsEventSchema("profile_picker_confirmed", 1, listOf(Screen, Field, Status))
    val ExperienceSelected = AnalyticsEventSchema("profile_experience_selected", 1, listOf(Screen, Experience))
    val SelectionChanged = AnalyticsEventSchema("profile_selection_changed", 1, listOf(Screen, Selected, Count))
    val PersonalizationChanged = AnalyticsEventSchema("profile_personalization_changed", 1, listOf(Screen, Selected, Count))
    val LinkClicked = AnalyticsEventSchema("profile_link_clicked", 1, listOf(Screen, Channel))
    val LinkResult = AnalyticsEventSchema("profile_link_result", 1, listOf(Screen, Channel, Opened))

    val all = listOf(
        Clicked,
        MenuSelected,
        PickerOpened,
        PickerConfirmed,
        ExperienceSelected,
        SelectionChanged,
        PersonalizationChanged,
        LinkClicked,
        LinkResult,
    )
}
