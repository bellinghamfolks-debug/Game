# Sources and integration decisions

The iOS application uses SwiftUI for screens, SceneKit with Metal rendering for its 3D studio, AVFoundation for playback, Core Haptics for tactile cues and XCTest for unit/UI tests. These frameworks ship with Apple's SDK and are linked from the OS. No online accounts, analytics SDKs, advertising SDKs, networking services or API keys are required for gameplay.

- **actions/checkout**, MIT. Fetches source in CI; pinned to 34e114876b0b11c390a56381ad16ebd13914f8d5. Credentials are not retained. https://github.com/actions/checkout
- **actions/upload-artifact**, MIT. Publishes the verified device archive and diagnostics; pinned to ea165f8d65b6e75b540449e92b4886f43607fa02. https://github.com/actions/upload-artifact
- **Apple-Actions/Example-iOS** was reviewed as a workflow reference. This project uses a separate unsigned-device build, with no uploaded certificate, keychain secret or profile. https://github.com/Apple-Actions/Example-iOS
- **QuizBattle** supplies part of the Arabic question bank under the enclosed MIT notice. See the question-source notes in this directory for upstream data provenance.
- **Audio, app icon, stage and game UI** were created for this project. The audio score and percussion are synthesized, with no sampled commercial recordings. The stage geometry and particles are authored in Swift. System fonts and system symbols render through native APIs.
