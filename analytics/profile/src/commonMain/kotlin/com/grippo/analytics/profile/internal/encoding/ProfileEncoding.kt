package com.grippo.analytics.profile.internal.encoding

import com.grippo.analytics.common.models.InputStatus
import com.grippo.analytics.profile.internal.models.InputField
import com.grippo.analytics.profile.internal.models.InteractionAction
import com.grippo.analytics.profile.models.BodyPicker
import com.grippo.analytics.profile.models.ExperienceLevel
import com.grippo.analytics.profile.models.GoalPicker
import com.grippo.analytics.profile.models.ProfileMenuTarget
import com.grippo.analytics.profile.models.ProfileOperation
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.analytics.profile.models.SocialChannel

/** Stable wire identifiers stay internal; public screens describe rendered surfaces. */
internal object ProfileEncoding {
    fun code(value: ProfileScreen): String = when (value) {
        ProfileScreen.Menu -> "profile_menu"
        ProfileScreen.Body -> "profile_body"
        ProfileScreen.Goal -> "profile_goal"
        ProfileScreen.Experience -> "profile_experience"
        ProfileScreen.Muscles -> "profile_muscles"
        ProfileScreen.Equipments -> "profile_equipments"
        ProfileScreen.Settings -> "profile_settings"
        ProfileScreen.Social -> "profile_social"
    }

    fun code(value: ProfileMenuTarget): String = when (value) {
        ProfileMenuTarget.Body -> "body"
        ProfileMenuTarget.Goal -> "goal"
        ProfileMenuTarget.Experience -> "experience"
        ProfileMenuTarget.Muscles -> "muscles"
        ProfileMenuTarget.Equipments -> "equipments"
        ProfileMenuTarget.Settings -> "settings"
        ProfileMenuTarget.Social -> "social"
        ProfileMenuTarget.Debug -> "debug"
    }

    fun code(value: ExperienceLevel): String = when (value) {
        ExperienceLevel.Beginner -> "beginner"
        ExperienceLevel.Intermediate -> "intermediate"
        ExperienceLevel.Advanced -> "advanced"
        ExperienceLevel.Pro -> "pro"
    }

    fun code(value: SocialChannel): String = when (value) {
        SocialChannel.Instagram -> "instagram"
        SocialChannel.TikTok -> "tiktok"
        SocialChannel.YouTube -> "youtube"
        SocialChannel.Threads -> "threads"
    }

    fun code(value: ProfileOperation): String = when (value) {
        ProfileOperation.RefreshUser -> "refresh_user"
        ProfileOperation.RefreshGoal -> "refresh_goal"
        ProfileOperation.RefreshMuscles -> "refresh_muscles"
        ProfileOperation.RefreshEquipments -> "refresh_equipments"
        ProfileOperation.RefreshWeightHistory -> "refresh_weight_history"
        ProfileOperation.SaveBody -> "save_body"
        ProfileOperation.SaveGoal -> "save_goal"
        ProfileOperation.SaveExperience -> "save_experience"
        ProfileOperation.SaveMuscles -> "save_muscles"
        ProfileOperation.SaveEquipments -> "save_equipments"
        ProfileOperation.Logout -> "logout"
        ProfileOperation.DeleteAccount -> "delete_account"
    }

    fun code(value: InteractionAction): String = when (value) {
        InteractionAction.Back -> "back"
        InteractionAction.Apply -> "apply"
        InteractionAction.Save -> "save"
        InteractionAction.GroupSelected -> "group_selected"
        InteractionAction.Logout -> "logout"
        InteractionAction.DeleteOpened -> "delete_opened"
        InteractionAction.DeleteConfirmed -> "delete_confirmed"
    }

    fun code(value: InputField): String = when (value) {
        InputField.Height -> "height"
        InputField.Weight -> "weight"
        InputField.Primary -> "primary"
        InputField.Secondary -> "secondary"
        InputField.TargetDate -> "target_date"
    }

    fun code(value: InputStatus): String = when (value) {
        InputStatus.Empty -> "empty"
        InputStatus.Invalid -> "invalid"
        InputStatus.Valid -> "valid"
    }

    fun field(value: BodyPicker): InputField = when (value) {
        BodyPicker.Height -> InputField.Height
        BodyPicker.Weight -> InputField.Weight
    }

    fun field(value: GoalPicker): InputField = when (value) {
        GoalPicker.Primary -> InputField.Primary
        GoalPicker.Secondary -> InputField.Secondary
        GoalPicker.TargetDate -> InputField.TargetDate
    }
}
