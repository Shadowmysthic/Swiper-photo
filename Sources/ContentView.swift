import SwiftUI
import Photos

struct ContentView: View {
    @EnvironmentObject var store: PhotoStore

    var body: some View {
        ZStack {
            AppBackground()
            content
        }
        .task { await store.start() }
        .alert("Oops", isPresented: Binding(
            get: { store.errorMessage != nil },
            set: { if !$0 { store.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(store.errorMessage ?? "")
        }
    }

    @ViewBuilder
    private var content: some View {
        switch store.access {
        case .unknown:
            ProgressView("Memuat...")
        case .denied:
            deniedView
        case .granted:
            if store.kind == nil {
                MenuView()
            } else {
                DeckView()
            }
        }
    }

    private var deniedView: some View {
        VStack(spacing: 14) {
            Text("🔒").font(.system(size: 56))
            Text("Akses galeri dibutuhkan")
                .font(Theme.rounded(22, .heavy))
                .foregroundColor(Theme.ink)
            Text("Buka Pengaturan lalu izinkan akses ke Foto (pilih Semua Foto).")
                .font(Theme.rounded(15, .medium))
                .multilineTextAlignment(.center)
                .foregroundColor(.secondary)
            Button("Buka Pengaturan") {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accent)
        }
        .padding()
    }
}

// MARK: - Layar swipe

struct DeckView: View {
    @EnvironmentObject var store: PhotoStore
    @State private var showReview = false
    @State private var confirmReset = false

    private var kind: MediaKind { store.kind ?? .photo }

    private var progress: Double {
        Double(store.reviewedCount) / Double(max(store.totalCount, 1))
    }

    private var cheer: String {
        switch progress {
        case ..<0.25: return "Ayo mulai! 💪"
        case ..<0.5: return "Mantap, lanjut! 🔥"
        case ..<0.75: return "Sudah lewat setengah! 🚀"
        case ..<1: return "Dikit lagi! 🏁"
        default: return "Bersih! ✨"
        }
    }

    var body: some View {
        VStack(spacing: 14) {
            header

            ZStack {
                if store.queue.isEmpty {
                    doneView
                } else {
                    ForEach(Array(store.queue.prefix(2).enumerated()).reversed(), id: \.element.localIdentifier) { index, asset in
                        SwipeCard(asset: asset, isTop: index == 0) { delete in
                            store.swipe(delete: delete)
                        }
                    }
                }
            }
            .padding(.horizontal)

            buttons
        }
        .padding(.vertical, 8)
        .sheet(isPresented: $showReview) {
            ReviewView().environmentObject(store)
        }
        .confirmationDialog("Mulai ulang dari awal?", isPresented: $confirmReset, titleVisibility: .visible) {
            Button("Reset progres (tidak ada yang dihapus)", role: .destructive) { store.resetProgress() }
            Button("Batal", role: .cancel) {}
        }
    }

    // MARK: Header

    private var header: some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                roundIcon("house.fill") {
                    withAnimation(.spring()) { store.backToMenu() }
                }
                Menu {
                    Button("Mulai ulang progres \(kind.nameID)", role: .destructive) { confirmReset = true }
                } label: {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(Theme.ink)
                        .frame(width: 38, height: 38)
                        .background(Circle().fill(Color.white))
                        .shadow(color: .black.opacity(0.08), radius: 4, y: 2)
                }
                Spacer()
                Text("\(kind.emoji) \(store.queue.count) \(kind.nameID) lagi")
                    .font(Theme.rounded(18, .heavy))
                    .foregroundColor(Theme.ink)
                Spacer()
                Button {
                    showReview = true
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "trash.fill")
                        Text("\(store.trash.count)")
                    }
                    .font(Theme.rounded(15, .heavy))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(store.trash.isEmpty ? Color.white : Theme.trash)
                    .foregroundColor(store.trash.isEmpty ? Theme.ink : .white)
                    .clipShape(Capsule())
                    .shadow(color: .black.opacity(0.08), radius: 4, y: 2)
                }
                .buttonStyle(BounceButtonStyle())
            }

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.white)
                    Capsule()
                        .fill(LinearGradient(colors: [Theme.keep, Theme.accent], startPoint: .leading, endPoint: .trailing))
                        .frame(width: max(10, geo.size.width * progress))
                        .animation(.spring(), value: progress)
                }
            }
            .frame(height: 10)

            HStack {
                Text("💚 Simpan \(store.keptCount)")
                Spacer()
                Text(cheer)
                Spacer()
                Text("🗑 Antre \(store.trash.count)")
            }
            .font(Theme.rounded(12, .semibold))
            .foregroundColor(.secondary)
        }
        .padding(.horizontal)
    }

    private func roundIcon(_ name: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: name)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Theme.ink)
                .frame(width: 38, height: 38)
                .background(Circle().fill(Color.white))
                .shadow(color: .black.opacity(0.08), radius: 4, y: 2)
        }
        .buttonStyle(BounceButtonStyle())
    }

    // MARK: Tombol aksi

    private var buttons: some View {
        HStack(spacing: 26) {
            actionButton(icon: "xmark", label: "Hapus", fill: Theme.trash, iconColor: .white, size: 70) {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                store.swipe(delete: true)
            }
            actionButton(icon: "arrow.uturn.backward", label: "Undo", fill: .white, iconColor: Theme.sun, size: 52) {
                store.undo()
            }
            .opacity(store.canUndo ? 1 : 0.35)
            .disabled(!store.canUndo)
            actionButton(icon: "heart.fill", label: "Simpan", fill: Theme.keep, iconColor: .white, size: 70) {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                store.swipe(delete: false)
            }
        }
        .disabled(store.queue.isEmpty && !store.canUndo)
        .padding(.bottom, 6)
    }

    private func actionButton(icon: String, label: String, fill: Color, iconColor: Color, size: CGFloat, action: @escaping () -> Void) -> some View {
        VStack(spacing: 4) {
            Button(action: action) {
                Image(systemName: icon)
                    .font(.system(size: size * 0.38, weight: .heavy))
                    .foregroundColor(iconColor)
                    .frame(width: size, height: size)
                    .background(Circle().fill(fill))
                    .shadow(color: fill.opacity(0.4), radius: 8, y: 5)
            }
            .buttonStyle(BounceButtonStyle())
            Text(label)
                .font(Theme.rounded(11, .semibold))
                .foregroundColor(.secondary)
        }
    }

    // MARK: Selesai

    private var doneView: some View {
        ZStack {
            VStack(spacing: 14) {
                Text("🎉").font(.system(size: 64))
                Text("Semua \(kind.nameID) sudah dicek!")
                    .font(Theme.rounded(24, .heavy))
                    .foregroundColor(Theme.ink)
                    .multilineTextAlignment(.center)
                if store.trash.isEmpty {
                    Text("Tidak ada yang perlu dihapus. Galerimu sudah rapi ✨")
                        .font(Theme.rounded(15, .medium))
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                } else {
                    Text("Ada \(store.trash.count) \(kind.nameID) di antrean hapus. Tinjau dulu sebelum dihapus ya.")
                        .font(Theme.rounded(15, .medium))
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                    Button("Tinjau dan hapus") { showReview = true }
                        .buttonStyle(.borderedProminent)
                        .tint(Theme.trash)
                }
            }
            .padding()
            ConfettiView()
        }
    }
}

// MARK: - Layar tinjau (konfirmasi kedua)

struct ReviewView: View {
    @EnvironmentObject var store: PhotoStore
    @Environment(\.dismiss) private var dismiss
    @State private var confirm = false
    @State private var working = false

    private let columns = [GridItem(.adaptive(minimum: 100), spacing: 6)]
    private var name: String { store.kind?.nameID ?? "foto" }

    var body: some View {
        NavigationStack {
            ZStack {
                AppBackground()
                if store.trash.isEmpty {
                    Text("Antrean hapus kosong 🙌")
                        .font(Theme.rounded(17, .semibold))
                        .foregroundColor(.secondary)
                } else {
                    ScrollView {
                        LazyVGrid(columns: columns, spacing: 6) {
                            ForEach(store.trash, id: \.localIdentifier) { asset in
                                Thumb(asset: asset)
                                    .overlay(alignment: .topTrailing) {
                                        Image(systemName: "xmark.circle.fill")
                                            .font(.system(size: 20))
                                            .foregroundColor(.white)
                                            .shadow(radius: 2)
                                            .padding(5)
                                    }
                                    .onTapGesture { store.restore(asset) }
                            }
                        }
                        .padding(8)
                    }
                }
            }
            .navigationTitle("Tinjau dulu ya 👀")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Tutup") { dismiss() }
                }
            }
            .safeAreaInset(edge: .bottom) {
                if !store.trash.isEmpty {
                    VStack(spacing: 8) {
                        Text("Ketuk sebuah \(name) untuk membatalkan penghapusannya")
                            .font(Theme.rounded(12, .medium))
                            .foregroundColor(.secondary)
                        Button {
                            confirm = true
                        } label: {
                            HStack {
                                if working { ProgressView().tint(.white) }
                                Text("Hapus \(store.trash.count) \(name)")
                                    .font(Theme.rounded(17, .heavy))
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Theme.trash)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 18))
                        }
                        .buttonStyle(BounceButtonStyle())
                        .disabled(working)
                    }
                    .padding()
                    .background(.bar)
                }
            }
            .alert("Hapus \(store.trash.count) \(name)?", isPresented: $confirm) {
                Button("Batal", role: .cancel) {}
                Button("Hapus", role: .destructive) {
                    Task {
                        working = true
                        let ok = await store.commitDelete()
                        working = false
                        if ok {
                            UINotificationFeedbackGenerator().notificationOccurred(.success)
                            dismiss()
                        }
                    }
                }
            } message: {
                Text("Dipindah ke album Dihapus Baru-baru Ini dan masih bisa dipulihkan selama 30 hari. iOS akan meminta konfirmasi sekali lagi.")
            }
        }
    }
}
