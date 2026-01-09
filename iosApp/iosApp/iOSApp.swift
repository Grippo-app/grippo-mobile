import SwiftUI
import shared
import FirebaseCore

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate: AppDelegate

    init() {
        Koin().doInit(appDeclaration: { _ in })

        FirebaseApp.configure()
        FirebaseProvider.shared.setup(analytics: IosFirebaseAnalytics())
    }

    var body: some Scene {
        WindowGroup {
            RootView(root: appDelegate.root, backDispatcher: appDelegate.backDispatcher)
                .ignoresSafeArea(edges: .all) // Solving issue: https://youtrack.jetbrains.com/issue/CMP-3621
        }
    }
}
