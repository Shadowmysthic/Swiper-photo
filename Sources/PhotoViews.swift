import SwiftUI
import Photos
import AVFoundation

/// Menampilkan gambar dari PHAsset (foto atau poster video, mendukung iCloud).
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

struct DurationBadge: View {
    let seconds: TimeInterval

    private var text: String {
        let total = Int(seconds.rounded())
        let h = total / 3600
        let m = (total % 3600) / 60
        let s = total % 60
        if h > 0 { return String(format: "%d:%02d:%02d", h, m, s) }
        return String(format: "%d:%02d", m, s)
    }

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: "video.fill")
            Text(text)
        }
        .font(.system(size: 12, weight: .bold, design: .rounded))
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color.black.opacity(0.55))
        .foregroundColor(.white)
        .clipShape(Capsule())
    }
}

// MARK: - Pemutar video

final class PlayerUIView: UIView {
    override class var layerClass: AnyClass { AVPlayerLayer.self }
    var playerLayer: AVPlayerLayer { layer as! AVPlayerLayer }
}

struct PlayerLayerView: UIViewRepresentable {
    let player: AVPlayer

    func makeUIView(context: Context) -> PlayerUIView {
        let view = PlayerUIView()
        view.playerLayer.videoGravity = .resizeAspect
        view.playerLayer.player = player
        return view
    }

    func updateUIView(_ uiView: PlayerUIView, context: Context) {
        uiView.playerLayer.player = player
    }
}

/// Memutar video otomatis (tanpa suara) di kartu paling atas.
struct VideoPlayerView: View {
    let asset: PHAsset

    @State private var player: AVPlayer?
    @State private var muted = true
    @State private var paused = false

    var body: some View {
        ZStack {
            if let player {
                PlayerLayerView(player: player)
                    .onTapGesture { togglePlay() }
            }
            if paused {
                Image(systemName: "play.circle.fill")
                    .font(.system(size: 64))
                    .foregroundColor(.white.opacity(0.9))
                    .shadow(radius: 4)
                    .allowsHitTesting(false)
            }
        }
        .overlay(alignment: .bottomTrailing) {
            if player != nil {
                Button {
                    toggleMute()
                } label: {
                    Image(systemName: muted ? "speaker.slash.fill" : "speaker.wave.2.fill")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 34, height: 34)
                        .background(Circle().fill(Color.black.opacity(0.55)))
                }
                .padding(12)
            }
        }
        .onAppear { load() }
        .onDisappear { player?.pause() }
        .onReceive(NotificationCenter.default.publisher(for: .AVPlayerItemDidPlayToEndTime)) { _ in
            player?.seek(to: .zero)
            if !paused { player?.play() }
        }
    }

    private func load() {
        let options = PHVideoRequestOptions()
        options.isNetworkAccessAllowed = true
        options.deliveryMode = .automatic
        PHImageManager.default().requestPlayerItem(forVideo: asset, options: options) { item, _ in
            DispatchQueue.main.async {
                guard let item else { return }
                let newPlayer = AVPlayer(playerItem: item)
                newPlayer.isMuted = true
                self.player = newPlayer
                newPlayer.play()
            }
        }
    }

    private func togglePlay() {
        guard let player else { return }
        if paused {
            player.play()
        } else {
            player.pause()
        }
        paused.toggle()
    }

    private func toggleMute() {
        muted.toggle()
        player?.isMuted = muted
        if !muted {
            try? AVAudioSession.sharedInstance().setCategory(.playback)
            try? AVAudioSession.sharedInstance().setActive(true)
        }
    }
}

// MARK: - Kartu swipe

/// Kartu yang bisa digeser. Kanan = simpan, kiri = masuk antrean hapus.
struct SwipeCard: View {
    let asset: PHAsset
    let isTop: Bool
    let onSwipe: (_ delete: Bool) -> Void

    @State private var offset = CGSize.zero
    private let threshold: CGFloat = 110

    private var isVideo: Bool { asset.mediaType == .video }

    var body: some View {
        cardContent
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 28))
            .overlay(stamps)
            .shadow(color: Theme.accent.opacity(0.25), radius: 14, y: 8)
            .scaleEffect(isTop ? 1 : 0.94)
            .offset(offset)
            .rotationEffect(.degrees(Double(offset.width) / 15))
            .gesture(drag, including: isTop ? .all : .none)
            .animation(.spring(), value: isTop)
    }

    private var cardContent: some View {
        ZStack {
            PhotoImage(asset: asset)
            if isVideo && isTop {
                VideoPlayerView(asset: asset)
            }
        }
        .overlay(alignment: .bottomLeading) {
            if isVideo {
                DurationBadge(seconds: asset.duration)
                    .padding(12)
                    .allowsHitTesting(false)
            }
        }
    }

    private var stamps: some View {
        let progress = min(abs(offset.width) / threshold, 1)
        return ZStack {
            stamp("SIMPAN 💚", color: Theme.keep)
                .opacity(offset.width > 0 ? progress : 0)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            stamp("HAPUS 🗑", color: Theme.trash)
                .opacity(offset.width < 0 ? progress : 0)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
        }
        .padding(20)
        .allowsHitTesting(false)
    }

    private func stamp(_ text: String, color: Color) -> some View {
        Text(text)
            .font(.system(size: 26, weight: .heavy, design: .rounded))
            .foregroundColor(color)
            .padding(.horizontal, 12)
            .padding(.vertical, 4)
            .background(Color.white.opacity(0.85))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(color, lineWidth: 4))
            .rotationEffect(.degrees(color == Theme.keep ? -10 : 10))
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

/// Thumbnail persegi untuk grid tinjau.
struct Thumb: View {
    let asset: PHAsset

    var body: some View {
        Color.clear
            .aspectRatio(1, contentMode: .fit)
            .overlay(
                PhotoImage(asset: asset, targetSize: CGSize(width: 300, height: 300), fill: true)
            )
            .overlay(alignment: .bottomLeading) {
                if asset.mediaType == .video {
                    DurationBadge(seconds: asset.duration)
                        .padding(4)
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
