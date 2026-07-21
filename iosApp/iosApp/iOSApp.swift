import SwiftUI
import Shared

@main
struct iOSApp: App {

    @Environment(\.scenePhase) private var scenePhase

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
        .onChange(of: scenePhase) { phase in
            // Drain anything the Share Extension captured while we were backgrounded/closed. The
            // Kotlin side is Mutex-guarded and empty-safe, so firing on every activation is fine.
            if phase == .active {
                ShareInboxKt.drainShareInbox()
            }
        }
    }
}
