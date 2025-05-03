import SwiftUI

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self)
        var appDelegate: AppDelegate
    
    var body: some Scene {
        WindowGroup {
            RootView(root: appDelegate.root)
        }
    }
}
