import SwiftUI
import Shared

@main
struct iOSApp: App {

    init() {
        // Start the Koin graph once, before any ViewModel is resolved. Mirrors Android's
        // MyApplication.onCreate(). (Kotlin's setupKoin(); it is NOT named initKoin because
        // Kotlin/Native would export that as doInitKoin.)
        MainViewControllerKt.setupKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)   // Compose draws edge-to-edge and manages its own insets
        }
    }
}
