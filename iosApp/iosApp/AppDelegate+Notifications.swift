import UserNotifications
import shared

/// Handles user notification events:
/// - deeplink navigation when the user taps a notification
/// - foreground banner presentation when a notification arrives while the app is open
extension AppDelegate: UNUserNotificationCenterDelegate {

    /// User tapped a notification — navigate to the deeplink.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        if let deeplink = userInfo["deeplink"] as? String {
            root.handleDeeplink(deeplink: deeplink)
        }
        completionHandler()
    }

    /// Notification arrived while app is in foreground — show it as a banner.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }
}
