import SwiftUI
import shared

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate: AppDelegate

    @Environment(\.scenePhase)
    private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootView(root: appDelegate.root, backDispatcher: appDelegate.backDispatcher)
                .ignoresSafeArea(edges: .all)  // Solving issue: https://youtrack.jetbrains.com/issue/CMP-3621
        }
        .onChange(of: scenePhase) { phase in
            switch phase {
            case .active:
                appDelegate.discardSavedState()
            case .background:
                appDelegate.saveState()
            case .inactive:
                break
            @unknown default:
                break
            }
        }
    }
}
