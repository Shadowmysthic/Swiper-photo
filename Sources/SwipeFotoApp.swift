import SwiftUI

@main
struct SwipeFotoApp: App {
    @StateObject private var store = PhotoStore()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(store)
        }
    }
}
