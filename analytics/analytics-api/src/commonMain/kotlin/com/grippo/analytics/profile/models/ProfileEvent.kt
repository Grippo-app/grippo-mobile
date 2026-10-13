package com.grippo.analytics.profile.models

import com.grippo.analytics.common.models.InputStatus

/** Observed callbacks only; no raw input, measurements, dates, URLs or entity IDs. */
public sealed interface ProfileEvent {
    public data class MenuSelected(public val target: ProfileMenuTarget) : ProfileEvent

    public sealed interface Body : ProfileEvent {
        public data class PickerOpened(public val field: BodyPicker) : Body
        public data class PickerConfirmed(public val field: BodyPicker, public val status: InputStatus) : Body
        public data object Apply : Body
        public data object Back : Body
    }

    public sealed interface Goal : ProfileEvent {
        public data class PickerOpened(public val field: GoalPicker) : Goal
        public data object PrimaryConfirmed : Goal
        public data object SecondaryConfirmed : Goal
        public data class TargetDateConfirmed(public val status: InputStatus) : Goal
        public data class PersonalizationChanged(public val selected: Boolean, public val selectedCount: Int) : Goal
        public data object Save : Goal
        public data object Back : Goal
    }

    public sealed interface Experience : ProfileEvent {
        public data class Selected(public val value: ExperienceLevel) : Experience
        public data object Apply : Experience
        public data object Back : Experience
    }

    public sealed interface Muscles : ProfileEvent {
        public data object GroupSelected : Muscles
        public data class SelectionChanged(public val selected: Boolean, public val selectedCount: Int) : Muscles
        public data object Apply : Muscles
        public data object Back : Muscles
    }

    public sealed interface Equipments : ProfileEvent {
        public data object GroupSelected : Equipments
        public data class SelectionChanged(public val selected: Boolean, public val selectedCount: Int) : Equipments
        public data object Apply : Equipments
        public data object Back : Equipments
    }

    public sealed interface Settings : ProfileEvent {
        public data object Logout : Settings
        public data object DeleteOpened : Settings
        public data object DeleteConfirmed : Settings
        public data object Back : Settings
    }

    public sealed interface Social : ProfileEvent {
        public data class LinkClicked(public val channel: SocialChannel) : Social
        public data class LinkResult(public val channel: SocialChannel, public val opened: Boolean) : Social
        public data object Back : Social
    }
}
