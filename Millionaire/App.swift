import SwiftUI

@main
struct MillionaireApp: App {
    var body: some Scene {
        WindowGroup {
            StudioRoot()
                .environment(\.layoutDirection, .rightToLeft)
                .environment(\.locale, Locale(identifier: "ar_SA"))
                .preferredColorScheme(.dark)
        }
    }
}
