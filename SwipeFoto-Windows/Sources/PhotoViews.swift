import SwiftUI
import Photos

/// Menampilkan satu foto dari PHAsset (mendukung foto iCloud).
struct PhotoImage: View {
    let asset: PHAsset
    var targetSize = CGSize(width: 1200, height: 1200)
    var fill = false

    @State private var image: UIImage?

    var body: some View {
        ZStack {
            if let image {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: fill ? .fill : .fit)
            } else {
                ProgressView()
            }
        }
        .onAppear { load() }
    }

    private func load() {
        let options = PHImageRequestOptions()
        options.deliveryMode = .opportunistic
        options.resizeMode = .fast
        options.isNetworkAccessAllowed = true
        PHImageManager.default().requestImage(
            for: asset,
            targetSize: targetSize,
            contentMode: .aspectFit,
            options: options
        ) { result, _ in
            if let result { self.image = result }
        }
    }
}

/// Kartu yang bisa digeser. Kanan = simpan, kiri = masuk antrean hapus.
struct SwipeCard: View {
    let asset: PHAsset
    let isTop: Bool
    let onSwipe: (_ delete: Bool) -> Void

    @State private var offset = CGSize.zero
    private let threshold: CGFloat = 110

    var body: some View {
        PhotoImage(asset: asset)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .overlay(stamps)
            .shadow(color: .black.opacity(0.2), radius: 8, y: 4)
            .scaleEffect(isTop ? 1 : 0.94)
            .offset(offset)
            .rotationEffect(.degrees(Double(offset.width) / 15))
            .gesture(drag, including: isTop ? .all : .none)
            .animation(.spring(), value: isTop)
    }

    private var stamps: some View {
        let progress = min(abs(offset.width) / threshold, 1)
        return ZStack {
            stamp("SIMPAN", color: .green)
                .opacity(offset.width > 0 ? progress : 0)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            stamp("HAPUS", color: .red)
                .opacity(offset.width < 0 ? progress : 0)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
        }
        .padding(20)
        .allowsHitTesting(false)
    }

    private func stamp(_ text: String, color: Color) -> some View {
        Text(text)
            .font(.system(size: 30, weight: .heavy, design: .rounded))
            .foregroundColor(color)
            .padding(.horizontal, 12)
            .padding(.vertical, 4)
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(color, lineWidth: 4))
            .rotationEffect(.degrees(color == .green ? -12 : 12))
    }

    private var drag: some Gesture {
        DragGesture()
            .onChanged { offset = $0.translation }
            .onEnded { value in
                if value.translation.width > threshold {
                    fly(direction: 1, delete: false)
                } else if value.translation.width < -threshold {
                    fly(direction: -1, delete: true)
                } else {
                    withAnimation(.spring()) { offset = .zero }
                }
            }
    }

    private func fly(direction: CGFloat, delete: Bool) {
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        withAnimation(.easeIn(duration: 0.22)) {
            offset = CGSize(width: direction * 700, height: offset.height)
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.22) {
            onSwipe(delete)
        }
    }
}

/// Thumbnail persegi untuk grid.
struct Thumb: View {
    let asset: PHAsset

    var body: some View {
        Color.clear
            .aspectRatio(1, contentMode: .fit)
            .overlay(
                PhotoImage(asset: asset, targetSize: CGSize(width: 300, height: 300), fill: true)
            )
            .clipped()
    }
}
