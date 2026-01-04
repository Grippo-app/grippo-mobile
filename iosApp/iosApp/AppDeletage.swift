import UIKit
import shared

class AppDelegate: NSObject, UIApplicationDelegate {
    var backDispatcher: BackDispatcher = BackDispatcherKt.BackDispatcher()

    lazy var root: RootComponent = RootComponent(
        componentContext: DefaultComponentContext(
            lifecycle: ApplicationLifecycle(),
            stateKeeper: nil, // stateKeeper,
            instanceKeeper: nil,
            backHandler: backDispatcher
        ),
        close: {
            // ✅ Soft hide app
            UIApplication.shared.perform(#selector(NSXPCConnection.suspend))

            // ❌ Hard close app
            // exit(0)
        }
    )

    func application(_ application: UIApplication, shouldSaveSecureApplicationState coder: NSCoder) -> Bool {
        return true
    }

    func application(_ application: UIApplication, shouldRestoreSecureApplicationState coder: NSCoder) -> Bool {
        return true
    }
}
