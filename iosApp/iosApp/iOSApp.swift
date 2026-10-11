import SwiftUI
import shared

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate: AppDelegate

    init() {
        #if DEBUG
        let analyticsDebug = true
        #else
        let analyticsDebug = false
        #endif
        Koin().doInit(analyticsDebug: analyticsDebug, appDeclaration: { _ in })
    }

    var body: some Scene {
        WindowGroup {
            RootView(root: appDelegate.root, backDispatcher: appDelegate.backDispatcher)
                .ignoresSafeArea(edges: .all)  // Solving issue: https://youtrack.jetbrains.com/issue/CMP-3621
        }
    }
}
