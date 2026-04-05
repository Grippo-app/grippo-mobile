package com.grippo.toolkit.local.notification

public sealed class NotificationKey(public open val key: Int) {
    public data object ChangeWeight : NotificationKey(1)
    public data object FinishWorkout : NotificationKey(2)
    public data class Custom(override val key: Int) : NotificationKey(key)
}