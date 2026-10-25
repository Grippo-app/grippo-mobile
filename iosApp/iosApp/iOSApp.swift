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
                // Compose owns all insets; SwiftUI safe areas leave an odd keyboard gap (CMP-3621).
                .ignoresSafeArea(edges: .all)
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
