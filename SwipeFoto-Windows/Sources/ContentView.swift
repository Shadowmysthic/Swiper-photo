import SwiftUI
import Photos

struct ContentView: View {
    @EnvironmentObject var store: PhotoStore
    @State private var showReview = false
    @State private var confirmReset = false

    var body: some View {
        Group {
            switch store.access {
            case .unknown: ProgressView("Memuat...")
            case .denied: deniedView
            case .granted: deck
            }
        }
        .task { await store.start() }
        .sheet(isPresented: $showReview) {
            ReviewView().environmentObject(store)
        }
        .alert("Gagal", isPresented: Binding(
            get: { store.errorMessage != nil },
            set: { if !$0 { store.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(store.errorMessage ?? "")
        }
        .confirmationDialog("Mulai ulang dari awal?", isPresented: $confirmReset, titleVisibility: .visible) {
            Button("Reset progres (foto tidak dihapus)", role: .destructive) { store.resetProgress() }
            Button("Batal", role: .cancel) {}
        }
    }

    // MARK: - Deck

    private var deck: some View {
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
    }

    private var header: some View {
        VStack(spacing: 8) {
            HStack {
                Menu {
                    Button("Mulai ulang progres", role: .destructive) { confirmReset = true }
                } label: {
                    Image(systemName: "ellipsis.circle").font(.title2)
                }
                Spacer()
                Text("Sisa \(store.queue.count) foto")
                    .font(.headline)
                Spacer()
                Button {
                    showReview = true
                } label: {
                    Label("\(store.trash.count)", systemImage: "trash")
                        .font(.subheadline.bold())
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(store.trash.isEmpty ? Color.gray.opacity(0.25) : Color.red)
                        .foregroundColor(store.trash.isEmpty ? .primary : .white)
                        .clipShape(Capsule())
                }
            }
            ProgressView(value: Double(store.reviewedCount), total: Double(max(store.totalCount, 1)))
                .tint(.green)
            Text("Disimpan \(store.keptCount)  •  Antre hapus \(store.trash.count)")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(.horizontal)
    }

    private var buttons: some View {
        HStack(spacing: 28) {
            circleButton("xmark", color: .red) {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                store.swipe(delete: true)
            }
            circleButton("arrow.uturn.backward", color: .orange, size: 50) {
                store.undo()
            }
            .opacity(store.canUndo ? 1 : 0.35)
            .disabled(!store.canUndo)
            circleButton("checkmark", color: .green) {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                store.swipe(delete: false)
            }
        }
        .disabled(store.queue.isEmpty && !store.canUndo)
        .padding(.bottom, 6)
    }

    private func circleButton(_ icon: String, color: Color, size: CGFloat = 64, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: size * 0.4, weight: .bold))
                .foregroundColor(color)
                .frame(width: size, height: size)
                .background(Circle().fill(Color(.systemBackground)))
                .overlay(Circle().stroke(color, lineWidth: 2.5))
                .shadow(radius: 3, y: 2)
        }
    }

    private var doneView: some View {
        VStack(spacing: 14) {
            Image(systemName: "party.popper")
                .font(.system(size: 56))
            Text("Semua foto sudah direview")
                .font(.title3.bold())
            Text("Ada \(store.trash.count) foto di antrean hapus. Tinjau dulu sebelum dihapus.")
                .multilineTextAlignment(.center)
                .foregroundColor(.secondary)
            if !store.trash.isEmpty {
                Button("Tinjau dan hapus") { showReview = true }
                    .buttonStyle(.borderedProminent)
                    .tint(.red)
            }
        }
        .padding()
    }

    private var deniedView: some View {
        VStack(spacing: 14) {
            Image(systemName: "lock.fill").font(.system(size: 48))
            Text("Akses foto dibutuhkan").font(.title3.bold())
            Text("Buka Pengaturan lalu izinkan akses ke Foto (pilih Semua Foto).")
                .multilineTextAlignment(.center)
                .foregroundColor(.secondary)
            Button("Buka Pengaturan") {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}

// MARK: - Layar tinjau (konfirmasi kedua)

struct ReviewView: View {
    @EnvironmentObject var store: PhotoStore
    @Environment(\.dismiss) private var dismiss
    @State private var confirm = false
    @State private var working = false

    private let columns = [GridItem(.adaptive(minimum: 100), spacing: 4)]

    var body: some View {
        NavigationStack {
            Group {
                if store.trash.isEmpty {
                    Text("Antrean hapus kosong")
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        LazyVGrid(columns: columns, spacing: 4) {
                            ForEach(store.trash, id: \.localIdentifier) { asset in
                                Thumb(asset: asset)
                                    .overlay(alignment: .topTrailing) {
                                        Image(systemName: "xmark.circle.fill")
                                            .foregroundColor(.white)
                                            .shadow(radius: 2)
                                            .padding(4)
                                    }
                                    .onTapGesture { store.restore(asset) }
                            }
                        }
                        .padding(4)
                    }
                }
            }
            .navigationTitle("Tinjau Sebelum Hapus")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Tutup") { dismiss() }
                }
            }
            .safeAreaInset(edge: .bottom) {
                if !store.trash.isEmpty {
                    VStack(spacing: 8) {
                        Text("Ketuk sebuah foto untuk membatalkan penghapusannya")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Button {
                            confirm = true
                        } label: {
                            HStack {
                                if working { ProgressView().tint(.white) }
                                Text("Hapus \(store.trash.count) foto")
                                    .bold()
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.red)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                        }
                        .disabled(working)
                    }
                    .padding()
                    .background(.bar)
                }
            }
            .alert("Hapus \(store.trash.count) foto?", isPresented: $confirm) {
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
                Text("Foto dipindah ke album Dihapus Baru-baru Ini dan masih bisa dipulihkan selama 30 hari. iOS akan meminta konfirmasi sekali lagi.")
            }
        }
    }
}
