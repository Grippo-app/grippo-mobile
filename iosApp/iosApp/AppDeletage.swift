import FirebaseCore
import FirebaseMessaging
import UIKit
import UserNotifications
import shared

class AppDelegate: NSObject, UIApplicationDelegate {

    var backDispatcher: BackDispatcher = BackDispatcherKt.BackDispatcher()

    lazy var root: RootComponent = RootComponent(
        componentContext: DefaultComponentContext(
            lifecycle: ApplicationLifecycle(),
            stateKeeper: nil,
            instanceKeeper: nil,
            backHandler: backDispatcher
        ),
        close: {
            // ✅ Soft hide app
            UIApplication.shared.perform(#selector(NSXPCConnection.suspend))

            // ❌ Hard close app
            // exit(0)
        },
        deeplink: nil
    )

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        // Firebase must be configured here — BEFORE any other Firebase calls.
        // Calling FirebaseApp.configure() in iOSApp.init() is too early:
        // at that point AppDelegate is not yet set as UIApplication.delegate,
        // so AppDelegateSwizzler cannot find it and logs:
        // "App Delegate does not conform to UIApplicationDelegate protocol."
        FirebaseApp.configure()
        FirebaseProvider.shared.setup(
            analytics: IosFirebaseAnalytics(),
            crashlytics: IosFirebaseCrashlytics(),
            messaging: IosFirebaseMessaging()
        )

        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self

        // Register with APNs so Firebase can obtain an FCM token.
        // Note: this does NOT show a permission prompt — it only fetches the APNs device token.
        // The user-facing notification permission dialog is requested later via PermissionManager
        // (inside HomeViewModel), so it appears only after the user has logged in.
        application.registerForRemoteNotifications()

        return true
    }

    // Forward the APNs device token to Firebase Messaging so it can map it to an FCM token.
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }

    // Required for Firebase to handle data messages and silent pushes (e.g. token refresh via APNs).
    // Without this, FCM data-only messages and content-available payloads are silently dropped.
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        completionHandler(.newData)
    }

    func application(_ application: UIApplication, shouldSaveSecureApplicationState coder: NSCoder) -> Bool {
        return true
    }

    func application(_ application: UIApplication, shouldRestoreSecureApplicationState coder: NSCoder) -> Bool {
        return true
    }
}
