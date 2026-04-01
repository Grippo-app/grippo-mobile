package com.grippo.toolkit.permission

public sealed class PermissionStatus {

    /** The permission is granted; the feature can proceed. */
    public data object Granted : PermissionStatus()

    /**
     * The permission was denied but the system will still show the dialog
     * if [PermissionManager.request] is called again (Android only).
     * On iOS this maps to `.notDetermined` — the user hasn't been asked yet.
     */
    public data object Denied : PermissionStatus()

    /**
     * The permission was permanently denied.
     *
     * - **Android**: the user ticked "Don't ask again". Direct the user to
     *   app Settings to re-enable the permission.
     * - **iOS**: the user explicitly denied the permission in the system
     *   dialog (`UNAuthorizationStatus.denied`).
     */
    public data object DeniedPermanently : PermissionStatus()
}
