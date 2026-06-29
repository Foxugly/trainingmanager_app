import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            // Custom scheme now; HTTPS Universal Links need an associated-domains
            // entitlement + AASA on tm.foxugly.com (ops prerequisite).
            .onOpenURL { url in
                MainViewControllerKt.handleDeepLink(uri: url.absoluteString)
            }
    }
}
