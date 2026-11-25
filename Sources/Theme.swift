import SwiftUI

enum Theme {
    static let keep = Color(red: 0.16, green: 0.78, blue: 0.56)
    static let trash = Color(red: 1.0, green: 0.38, blue: 0.46)
    static let accent = Color(red: 0.45, green: 0.38, blue: 0.96)
    static let sun = Color(red: 1.0, green: 0.78, blue: 0.28)
    static let ink = Color(red: 0.14, green: 0.14, blue: 0.22)
    static let confetti: [Color] = [keep, trash, accent, sun, .pink, .cyan]

    static func rounded(_ size: CGFloat, _ weight: Font.Weight = .bold) -> Font {
        Font.system(size: size, weight: weight, design: .rounded)
    }
}

struct AppBackground: View {
    var body: some View {
        LinearGradient(
            colors: [Color(red: 0.97, green: 0.95, blue: 1.0), Color(red: 0.89, green: 0.96, blue: 1.0)],
            startPoint: .top,
            endPoint: .bottom
        )
        .ignoresSafeArea()
    }
}

struct BounceButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.94 : 1)
            .animation(.spring(response: 0.25, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

/// Konfeti sederhana untuk perayaan saat semua media selesai direview.
struct ConfettiView: View {
    private struct Piece: Identifiable {
        let id = UUID()
        let x: CGFloat
        let size: CGFloat
        let delay: Double
        let duration: Double
        let color: Color
        let spin: Double
    }

    @State private var fall = false
    @State private var pieces: [Piece] = (0..<36).map { _ in
        Piece(
            x: CGFloat.random(in: 0...1),
            size: CGFloat.random(in: 6...12),
            delay: Double.random(in: 0...1.2),
            duration: Double.random(in: 2...3.5),
            color: Theme.confetti.randomElement() ?? .pink,
            spin: Double.random(in: 180...540)
        )
    }

    var body: some View {
        GeometryReader { geo in
            ZStack {
                ForEach(pieces) { piece in
                    RoundedRectangle(cornerRadius: 2)
                        .fill(piece.color)
                        .frame(width: piece.size, height: piece.size * 1.6)
                        .rotationEffect(.degrees(fall ? piece.spin : 0))
                        .position(x: piece.x * geo.size.width, y: fall ? geo.size.height + 30 : -30)
                        .animation(.easeIn(duration: piece.duration).delay(piece.delay), value: fall)
                }
            }
        }
        .allowsHitTesting(false)
        .onAppear { fall = true }
    }
}
