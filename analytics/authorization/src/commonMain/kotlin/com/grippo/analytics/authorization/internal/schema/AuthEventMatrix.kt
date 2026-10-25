package com.grippo.analytics.authorization.internal.schema

import com.grippo.analytics.authorization.internal.encoding.AuthEncoding
import com.grippo.analytics.authorization.internal.events.AuthPayloadEvent
import com.grippo.analytics.authorization.internal.models.InputField
import com.grippo.analytics.authorization.internal.models.InteractionAction
import com.grippo.analytics.authorization.models.AuthEvent
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.analytics.authorization.models.Method
import com.grippo.analytics.common.models.InputKeyboardAction
import com.grippo.analytics.common.models.InputStatus
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter
import com.grippo.toolkit.analytics.values.AnalyticsCount

internal object AuthEventMatrix {
    fun event(event: AuthEvent): AnalyticsEvent {
        return when (event) {
            AuthEvent.Splash.DismissRequested -> clicked(AuthScreen.Splash, InteractionAction.DismissRequested)
            AuthEvent.Onboarding.GetStarted -> clicked(AuthScreen.Onboarding, InteractionAction.Next)
            AuthEvent.Onboarding.DismissRequested -> clicked(AuthScreen.Onboarding, InteractionAction.DismissRequested)
            is AuthEvent.Login -> login(event)
            is AuthEvent.Registration -> registration(event)
            is AuthEvent.User -> user(event)
            is AuthEvent.Experience -> experience(event)
            is AuthEvent.Muscles -> muscles(event)
            is AuthEvent.Equipments -> equipments(event)
            AuthEvent.Completed.DismissRequested -> clicked(AuthScreen.Completed, InteractionAction.DismissRequested)
        }
    }

    private fun login(event: AuthEvent.Login): AnalyticsEvent {
        return when (event) {
            is AuthEvent.Login.InputChanged -> inputChanged(
                AuthScreen.Login,
                AuthEncoding.field(event.field),
                event.status,
            )
            is AuthEvent.Login.InputFocused -> field(
                AuthInteractionSchema.InputFocused,
                AuthScreen.Login,
                AuthEncoding.field(event.field),
            )
            is AuthEvent.Login.InputCleared -> field(
                AuthInteractionSchema.InputCleared,
                AuthScreen.Login,
                AuthEncoding.field(event.field),
            )
            is AuthEvent.Login.KeyboardAction -> keyboard(
                AuthScreen.Login,
                AuthEncoding.field(event.field),
                event.action,
            )
            is AuthEvent.Login.PasswordVisibilityChanged -> visibility(AuthScreen.Login, event.visible)
            is AuthEvent.Login.Submit -> submitted(AuthScreen.Login, event.method)
            AuthEvent.Login.Register -> clicked(AuthScreen.Login, InteractionAction.Register)
            AuthEvent.Login.DismissRequested -> clicked(AuthScreen.Login, InteractionAction.DismissRequested)
        }
    }

    private fun registration(event: AuthEvent.Registration): AnalyticsEvent {
        return when (event) {
            is AuthEvent.Registration.InputChanged -> inputChanged(
                AuthScreen.Registration,
                AuthEncoding.field(event.field),
                event.status,
            )
            is AuthEvent.Registration.InputFocused -> field(
                AuthInteractionSchema.InputFocused,
                AuthScreen.Registration,
                AuthEncoding.field(event.field),
            )
            is AuthEvent.Registration.InputCleared -> field(
                AuthInteractionSchema.InputCleared,
                AuthScreen.Registration,
                AuthEncoding.field(event.field),
            )
            is AuthEvent.Registration.KeyboardAction -> keyboard(
                AuthScreen.Registration,
                AuthEncoding.field(event.field),
                event.action,
            )
            is AuthEvent.Registration.PasswordVisibilityChanged -> visibility(AuthScreen.Registration, event.visible)
            AuthEvent.Registration.Submit -> submitted(AuthScreen.Registration, Method.Email)
            AuthEvent.Registration.DismissRequested -> clicked(AuthScreen.Registration, InteractionAction.DismissRequested)
        }
    }

    private fun user(event: AuthEvent.User): AnalyticsEvent {
        return when (event) {
            is AuthEvent.User.NameInputChanged -> inputChanged(
                AuthScreen.User,
                InputField.Name,
                event.status,
            )
            AuthEvent.User.NameInputFocused -> field(
                AuthInteractionSchema.InputFocused,
                AuthScreen.User,
                InputField.Name,
            )
            AuthEvent.User.NameInputCleared -> field(
                AuthInteractionSchema.InputCleared,
                AuthScreen.User,
                InputField.Name,
            )
            is AuthEvent.User.NameKeyboardAction -> keyboard(
                AuthScreen.User,
                InputField.Name,
                event.action,
            )
            is AuthEvent.User.PickerOpened -> field(
                AuthInteractionSchema.PickerOpened,
                AuthScreen.User,
                AuthEncoding.field(event.field),
            )
            is AuthEvent.User.PickerConfirmed -> payload(
                AuthInteractionSchema.PickerConfirmed,
                AuthScreen.User,
                AuthInteractionSchema.Field.value(AuthEncoding.field(event.field)),
                AuthInteractionSchema.Status.value(event.status),
            )
            is AuthEvent.User.Control -> clicked(AuthScreen.User, AuthEncoding.action(event.action))
        }
    }

    private fun experience(event: AuthEvent.Experience): AnalyticsEvent {
        return when (event) {
            is AuthEvent.Experience.Selected -> payload(
                AuthInteractionSchema.ExperienceSelected,
                AuthScreen.Experience,
                AuthInteractionSchema.Experience.value(event.value),
            )
            is AuthEvent.Experience.Control -> clicked(AuthScreen.Experience, AuthEncoding.action(event.action))
        }
    }

    private fun muscles(event: AuthEvent.Muscles): AnalyticsEvent {
        return when (event) {
            AuthEvent.Muscles.GroupSelected -> clicked(AuthScreen.ExcludedMuscles, InteractionAction.GroupSelected)
            is AuthEvent.Muscles.SelectionChanged -> selection(
                AuthScreen.ExcludedMuscles,
                event.selected,
                event.selectedCount,
            )
            is AuthEvent.Muscles.Control -> clicked(AuthScreen.ExcludedMuscles, AuthEncoding.action(event.action))
        }
    }

    private fun equipments(event: AuthEvent.Equipments): AnalyticsEvent {
        return when (event) {
            AuthEvent.Equipments.GroupSelected -> clicked(AuthScreen.MissingEquipments, InteractionAction.GroupSelected)
            is AuthEvent.Equipments.SelectionChanged -> selection(
                AuthScreen.MissingEquipments,
                event.selected,
                event.selectedCount,
            )
            is AuthEvent.Equipments.Control -> clicked(AuthScreen.MissingEquipments, AuthEncoding.action(event.action))
        }
    }

    private fun clicked(screen: AuthScreen, action: InteractionAction): AnalyticsEvent {
        return payload(AuthInteractionSchema.Clicked, screen, AuthInteractionSchema.Action.value(action))
    }

    private fun submitted(screen: AuthScreen, method: Method): AnalyticsEvent {
        return payload(AuthInteractionSchema.Submitted, screen, AuthOperationSchema.Method.value(method))
    }

    private fun inputChanged(screen: AuthScreen, field: InputField, status: InputStatus): AnalyticsEvent {
        return payload(
            AuthInteractionSchema.InputChanged,
            screen,
            AuthInteractionSchema.Field.value(field),
            AuthInteractionSchema.Status.value(status),
        )
    }

    private fun field(schema: AnalyticsEventSchema, screen: AuthScreen, field: InputField): AnalyticsEvent {
        return payload(schema, screen, AuthInteractionSchema.Field.value(field))
    }

    private fun keyboard(screen: AuthScreen, field: InputField, action: InputKeyboardAction): AnalyticsEvent {
        return payload(
            AuthInteractionSchema.Keyboard,
            screen,
            AuthInteractionSchema.Field.value(field),
            AuthInteractionSchema.KeyboardAction.value(action),
        )
    }

    private fun visibility(screen: AuthScreen, visible: Boolean): AnalyticsEvent {
        return payload(
            AuthInteractionSchema.PasswordVisibilityChanged,
            screen,
            AuthInteractionSchema.Visible.value(visible),
        )
    }

    private fun selection(screen: AuthScreen, selected: Boolean, count: Int): AnalyticsEvent {
        return payload(
            AuthInteractionSchema.SelectionChanged,
            screen,
            AuthInteractionSchema.Selected.value(selected),
            AuthInteractionSchema.SelectedCount.value(AnalyticsCount(count.toLong())),
        )
    }

    private fun payload(
        schema: AnalyticsEventSchema,
        screen: AuthScreen,
        vararg parameters: AnalyticsParameter,
    ): AnalyticsEvent {
        return AuthPayloadEvent(schema, buildList(parameters.size + 1) {
            add(AuthInteractionSchema.Screen.value(screen))
            addAll(parameters)
        })
    }
}
