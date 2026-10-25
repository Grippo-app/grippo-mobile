import FirebaseCore
import FirebaseMessaging
import UIKit
import UserNotifications
import shared

class AppDelegate: NSObject, UIApplicationDelegate {

    let bootstrap: ApplicationBootstrap

    override init() {
        bootstrap = Koin().doInit(appDeclaration: { _ in })
        super.init()
    }

    let backDispatcher: BackDispatcher = BackDispatcherKt.BackDispatcher()

    private let stateStore = ApplicationStateStore()
    private var applicationState: ApplicationState?

    private(set) lazy var root: RootComponent = {
        let state: ApplicationState
        do {
            if let savedState = try stateStore.restore() {
                state = try ApplicationState(savedState: savedState)
            } else {
                state = ApplicationState()
            }
        } catch {
            NSLog("Unable to restore application state")
            state = ApplicationState()
        }
        applicationState = state
        return RootComponent(
            componentContext: DefaultComponentContext(
                lifecycle: ApplicationLifecycle(),
                stateKeeper: state.stateKeeper,
                instanceKeeper: nil,
                backHandler: backDispatcher
            ),
            close: {
                UIApplication.shared.perform(#selector(NSXPCConnection.suspend))
            },
            deeplink: nil
        )
    }()

    func saveState() {
        guard let applicationState else { return }
        do {
            try stateStore.save(applicationState.save())
        } catch {
            NSLog("Unable to save application state")
        }
    }

    func discardSavedState() {
        guard applicationState != nil else { return }
        do {
            try stateStore.clear()
        } catch {
            NSLog("Unable to discard application state")
        }
    }

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        // Configure here, not in iOSApp.init(): Firebase's swizzler needs AppDelegate installed first.
        FirebaseApp.configure()
        let analytics = IosFirebaseAnalytics()
        analytics.setCollection(collection: .unknown)
        bootstrap.analytics.attachSink(analyticsSink: analytics)
        bootstrap.crashReporting.attachSink(crashSink: IosFirebaseCrashlytics())
        bootstrap.messaging.attachTokenProvider(tokenProvider: IosFirebaseMessaging())

        UNUserNotificationCenter.current().delegate = self
        FirebaseMessaging.Messaging.messaging().delegate = self

        application.registerForRemoteNotifications()

        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        FirebaseMessaging.Messaging.messaging().apnsToken = deviceToken
    }

    // Forward remote messages to Firebase because automatic delegate forwarding is disabled.
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        FirebaseMessaging.Messaging.messaging().appDidReceiveMessage(userInfo)
        completionHandler(.noData)
    }
}
