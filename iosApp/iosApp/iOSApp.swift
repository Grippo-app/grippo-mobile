import SwiftUI
import shared

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self)
        var appDelegate: AppDelegate
    
    init() {
        Koin().doInit(appDeclaration: { _ in })
    }
    
    var body: some Scene {
        WindowGroup {
            RootView(root: appDelegate.root)
                .ignoresSafeArea(.keyboard) // Solving issue: https://youtrack.jetbrains.com/issue/CMP-3621
        }
    }
}
