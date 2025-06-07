import SwiftUI
import shared

struct RootView: UIViewControllerRepresentable {
    let root: RootComponent
    let backDispatcher: BackDispatcher

    func makeUIViewController(context: Context) -> UIViewController {
        let controller = RootViewControllerKt.rootViewController(root: root, backDispatcher: backDispatcher)
        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
