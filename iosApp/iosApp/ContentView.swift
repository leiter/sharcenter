import SwiftUI
import Shared

/// Hosts the Compose Multiplatform UI (`MainViewController()` from the shared framework) inside
/// SwiftUI. Once WP7 moves the screens into commonMain, `MainViewController()` renders the real
/// app; until then it shows a placeholder that still proves framework linking + Compose + Koin.
struct ContentView: UIViewControllerRepresentable {

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
