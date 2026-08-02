import UIKit
import UniformTypeIdentifiers

/// The ShareCenter Share Extension — deliberately tiny.
///
/// It runs in a separate process from the app and does NOT link the shared Kotlin `Shared`
/// framework. Its only job is to capture the shared URL (or plain text) and drop it into the App
/// Group "inbox" as a single file. The main app drains that inbox and does the real
/// resolve/save/enrich work on next foreground (see `ShareInbox.kt` / `drainShareInbox()`).
///
/// Keeping it this small avoids the extension's tight (~120 MB) memory budget and the complexity of
/// running SQLDelight/Ktor/Koin in a second process.
///
/// `appGroupId` MUST match `APP_GROUP_ID` in `ShareInbox.kt` and the
/// `com.apple.security.application-groups` entitlement on both targets.
class ShareViewController: UIViewController {

    private let appGroupId = "group.cut.the.crap"

    override func viewDidLoad() {
        super.viewDidLoad()
        handleShare()
    }

    private func handleShare() {
        guard let providers = (extensionContext?.inputItems.first as? NSExtensionItem)?.attachments else {
            return complete()
        }

        let urlType = UTType.url.identifier
        let textType = UTType.plainText.identifier

        // Prefer a real URL attachment; fall back to plain text (which may itself be a URL — the
        // app's GenericSharedLinkHandler saves any string as a link, matching Android).
        if let provider = providers.first(where: { $0.hasItemConformingToTypeIdentifier(urlType) }) {
            provider.loadItem(forTypeIdentifier: urlType, options: nil) { [weak self] item, _ in
                self?.save((item as? URL)?.absoluteString)
                self?.complete()
            }
        } else if let provider = providers.first(where: { $0.hasItemConformingToTypeIdentifier(textType) }) {
            provider.loadItem(forTypeIdentifier: textType, options: nil) { [weak self] item, _ in
                self?.save(item as? String)
                self?.complete()
            }
        } else {
            complete()
        }
    }

    /// Writes one file per shared item (`<uuid>.txt`) into `<AppGroup>/inbox/`. One-file-per-item
    /// avoids cross-process append races and is crash-safe. Silently no-ops if the container is
    /// unavailable (entitlement not provisioned) — better to drop a share than to crash the sheet.
    private func save(_ urlString: String?) {
        guard
            let urlString, !urlString.isEmpty,
            let container = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroupId)
        else { return }

        let inbox = container.appendingPathComponent("inbox", isDirectory: true)
        try? FileManager.default.createDirectory(at: inbox, withIntermediateDirectories: true)
        let file = inbox.appendingPathComponent(UUID().uuidString + ".txt")
        try? urlString.data(using: .utf8)?.write(to: file, options: .atomic)
    }

    /// `loadItem` completion handlers run off the main thread; completing the request is UI-thread work.
    private func complete() {
        DispatchQueue.main.async { [weak self] in
            self?.extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
        }
    }
}
