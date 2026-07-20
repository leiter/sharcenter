import SwiftUI
import Shared

/// Hosts the Compose Multiplatform UI (`MainViewController()` from the shared framework) inside
/// SwiftUI. `MainViewController()` renders the real shared `App()` — the same composable the
/// Android launcher shows.
struct ContentView: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
