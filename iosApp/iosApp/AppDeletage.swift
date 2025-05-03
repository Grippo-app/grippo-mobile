import UIKit
import shared

class AppDelegate: NSObject, UIApplicationDelegate {
    let root: RootComponent = RootComponent(
        componentContext: DefaultComponentContext(lifecycle: ApplicationLifecycle())
    )
}
