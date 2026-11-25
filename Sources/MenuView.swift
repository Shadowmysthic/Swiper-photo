import SwiftUI

/// Menu awal: pilih mau membersihkan foto atau video.
struct MenuView: View {
    @EnvironmentObject var store: PhotoStore
    @State private var bounce = false

    var body: some View {
        VStack(spacing: 20) {
            Spacer()

            Text("🧹")
                .font(.system(size: 72))
                .offset(y: bounce ? -8 : 8)
                .animation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true), value: bounce)

            Text("Yuk beres-beres galeri!")
                .font(Theme.rounded(32, .heavy))
                .foregroundColor(Theme.ink)
                .multilineTextAlignment(.center)

            Text("Geser kanan untuk simpan, geser kiri untuk hapus. Seru kayak main game!")
                .font(Theme.rounded(15, .medium))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            VStack(spacing: 16) {
                ForEach(MediaKind.allCases, id: \.self) { kind in
                    KindCard(kind: kind)
                }
            }
            .padding(.top, 8)

            Spacer()

            Text("✅ Tidak ada yang terhapus sebelum kamu konfirmasi")
                .font(Theme.rounded(12, .medium))
                .foregroundColor(.secondary)
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 12)
        .onAppear { bounce = true }
    }
}

struct KindCard: View {
    @EnvironmentObject var store: PhotoStore
    let kind: MediaKind

    private var total: Int { store.libraryCounts[kind] ?? 0 }
    private var done: Int { min(store.doneCounts[kind] ?? 0, total) }

    private var percent: Int {
        total == 0 ? 0 : Int(Double(done) / Double(total) * 100)
    }

    private var colors: [Color] {
        kind == .photo
            ? [Theme.accent, Color(red: 0.72, green: 0.42, blue: 0.98)]
            : [Theme.trash, Color(red: 1.0, green: 0.60, blue: 0.35)]
    }

    var body: some View {
        Button {
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            withAnimation(.spring()) { store.select(kind) }
        } label: {
            HStack(spacing: 16) {
                Text(kind.emoji)
                    .font(.system(size: 38))
                    .frame(width: 70, height: 70)
                    .background(Circle().fill(Color.white.opacity(0.25)))

                VStack(alignment: .leading, spacing: 4) {
                    Text(kind.titleID)
                        .font(Theme.rounded(26, .heavy))
                    Text("\(total) \(kind.nameID) • \(percent)% sudah dicek")
                        .font(Theme.rounded(14, .medium))
                        .opacity(0.9)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 18, weight: .bold))
            }
            .foregroundColor(.white)
            .padding(18)
            .background(
                LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing)
            )
            .clipShape(RoundedRectangle(cornerRadius: 26))
            .shadow(color: colors[0].opacity(0.35), radius: 12, y: 8)
        }
        .buttonStyle(BounceButtonStyle())
        .disabled(total == 0)
        .opacity(total == 0 ? 0.5 : 1)
    }
}
