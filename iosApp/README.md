# ShareCenter — iOS app

A thin SwiftUI host around the shared Kotlin/Compose framework. The project file is generated from
[`project.yml`](project.yml) by [XcodeGen](https://github.com/yonaskolb/XcodeGen), so nothing but the
spec and the Swift sources is checked in.

## What's here

| File | Role |
|---|---|
| `iosApp/iOSApp.swift` | `@main` app; calls `MainViewControllerKt.setupKoin()` at launch |
| `iosApp/ContentView.swift` | `UIViewControllerRepresentable` hosting `MainViewController()` |
| `iosApp/Info.plist` | bundle metadata; ATS allows cleartext for the dev server |
| `project.yml` | XcodeGen spec — framework search paths, linker flags, the Gradle pre-build script |

The shared framework is built by the Gradle task `:shared:embedAndSignAppleFrameworkForXcode`,
which a pre-build script phase runs on every Xcode build (it's incremental). It compiles, links,
and embeds+signs `Shared.framework` into the app bundle.

> **Current UI is a placeholder.** `MainViewController()` renders a "shell running" screen until WP7
> moves the app's screens into `commonMain`. This app still proves the real stack: framework
> linking, Compose rendering on iOS, `setupKoin()` starting the graph, and (when a screen resolves a
> ViewModel) the `NativeSqliteDriver` + repositories.

## One-time setup

1. **Install tools** (once):
   ```sh
   brew install xcodegen
   ```
   Xcode (with iOS platform + a Simulator) and a JDK are also required. If `gradlew` can't find
   Java from Xcode's script phase, set `JAVA_HOME` in `project.yml`'s script (a fallback is wired
   already).

2. **Generate the Xcode project** (re-run whenever `project.yml` or the file list changes):
   ```sh
   cd iosApp
   xcodegen generate
   ```

3. **Open and run**:
   ```sh
   open iosApp.xcodeproj
   ```
   Select the `iosApp` scheme + an iOS Simulator, then Run (⌘R). The first build is slow (it builds
   the Kotlin/Native framework).

## Command-line build (no Xcode GUI)

After `xcodegen generate`:
```sh
cd iosApp
xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build
```

## Configuration

The dev API base URL lives in `shared/src/iosMain/kotlin/cut/the/crap/MainViewController.kt`
(`DEV_API_BASE_URL`). Change it there. If you point it at an `https://` server, tighten the
`NSAppTransportSecurity` exception in `Info.plist`.

## Known gaps (reduced iOS v1)

- **File picker** (`rememberFilePicker`) yields nothing — the real `UIDocumentPickerViewController`
  needs presentation testing; lands with the UI work.
- **Backup / restore**, **WebView X-login** are capability-flagged off (`IosBackupManager`,
  `IosLoginFlow.isSupported = false`).
- **`LocaleFormat`** (`NSNumberFormatter`/`NSDateFormatter`) compiles but is unverified at runtime —
  confirm number/date output looks right on a running Simulator.
