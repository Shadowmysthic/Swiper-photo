import Photos
import UIKit

enum MediaKind: String, CaseIterable {
    case photo, video

    var mediaType: PHAssetMediaType { self == .photo ? .image : .video }
    var nameID: String { self == .photo ? "foto" : "video" }
    var titleID: String { self == .photo ? "Foto" : "Video" }
    var emoji: String { self == .photo ? "📸" : "🎬" }
}

/// Mengelola antrean media, hasil swipe, dan penghapusan sungguhan lewat PhotoKit.
@MainActor
final class PhotoStore: ObservableObject {
    enum Access { case unknown, granted, denied }

    @Published private(set) var access: Access = .unknown
    @Published private(set) var kind: MediaKind?
    @Published private(set) var queue: [PHAsset] = []   // belum direview
    @Published private(set) var trash: [PHAsset] = []   // antrean hapus (swipe kiri)
    @Published private(set) var keptCount = 0           // swipe kanan
    @Published private(set) var totalCount = 0
    @Published private(set) var libraryCounts: [MediaKind: Int] = [:]
    @Published private(set) var doneCounts: [MediaKind: Int] = [:]
    @Published var errorMessage: String?

    private struct Step {
        let asset: PHAsset
        let deleted: Bool
    }

    private var history: [Step] = []
    private var keptIDs: Set<String> = []
    private var trashIDs: [String] = []
    private let defaults = UserDefaults.standard

    var canUndo: Bool { !history.isEmpty }
    var reviewedCount: Int { keptCount + trash.count }

    // MARK: - Izin dan menu

    func start() async {
        var status = PHPhotoLibrary.authorizationStatus(for: .readWrite)
        if status == .notDetermined {
            status = await PHPhotoLibrary.requestAuthorization(for: .readWrite)
        }
        switch status {
        case .authorized, .limited:
            access = .granted
            refreshCounts()
        default:
            access = .denied
        }
    }

    func refreshCounts() {
        for kind in MediaKind.allCases {
            libraryCounts[kind] = PHAsset.fetchAssets(with: kind.mediaType, options: nil).count
            let kept = defaults.stringArray(forKey: key("kept", kind))?.count ?? 0
            let trashed = defaults.stringArray(forKey: key("trash", kind))?.count ?? 0
            doneCounts[kind] = kept + trashed
        }
    }

    func select(_ kind: MediaKind) {
        self.kind = kind
        reload()
    }

    func backToMenu() {
        kind = nil
        queue = []
        trash = []
        history = []
        refreshCounts()
    }

    // MARK: - Muat media

    private func key(_ name: String, _ kind: MediaKind) -> String {
        "\(name)_\(kind.rawValue)"
    }

    func reload() {
        guard let kind else { return }
        keptIDs = Set(defaults.stringArray(forKey: key("kept", kind)) ?? [])
        trashIDs = defaults.stringArray(forKey: key("trash", kind)) ?? []

        let options = PHFetchOptions()
        options.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
        let result = PHAsset.fetchAssets(with: kind.mediaType, options: options)

        var all: [PHAsset] = []
        result.enumerateObjects { asset, _, _ in all.append(asset) }

        var byID: [String: PHAsset] = [:]
        for asset in all { byID[asset.localIdentifier] = asset }

        trash = trashIDs.compactMap { byID[$0] }
        trashIDs = trash.map { $0.localIdentifier }
        keptIDs = keptIDs.filter { byID[$0] != nil }
        keptCount = keptIDs.count
        totalCount = all.count

        let trashSet = Set(trashIDs)
        queue = all.filter {
            !keptIDs.contains($0.localIdentifier) && !trashSet.contains($0.localIdentifier)
        }
        history = []
        persist()
    }

    // MARK: - Swipe

    /// delete = true untuk swipe kiri (masuk antrean hapus), false untuk swipe kanan (simpan).
    func swipe(delete: Bool) {
        guard !queue.isEmpty else { return }
        let asset = queue.removeFirst()
        history.append(Step(asset: asset, deleted: delete))
        if delete {
            trash.append(asset)
            trashIDs.append(asset.localIdentifier)
        } else {
            keptIDs.insert(asset.localIdentifier)
            keptCount = keptIDs.count
        }
        persist()
    }

    func undo() {
        guard let step = history.popLast() else { return }
        let id = step.asset.localIdentifier
        queue.insert(step.asset, at: 0)
        if step.deleted {
            trash.removeAll { $0.localIdentifier == id }
            trashIDs.removeAll { $0 == id }
        } else {
            keptIDs.remove(id)
            keptCount = keptIDs.count
        }
        persist()
    }

    /// Dipakai di layar tinjau: item dikeluarkan dari antrean hapus dan dianggap disimpan.
    func restore(_ asset: PHAsset) {
        let id = asset.localIdentifier
        trash.removeAll { $0.localIdentifier == id }
        trashIDs.removeAll { $0 == id }
        history.removeAll { $0.asset.localIdentifier == id }
        keptIDs.insert(id)
        keptCount = keptIDs.count
        persist()
    }

    // MARK: - Penghapusan sungguhan

    func commitDelete() async -> Bool {
        let items = trash
        guard !items.isEmpty else { return true }
        do {
            try await Self.delete(items)
            trash = []
            trashIDs = []
            history = []
            totalCount = max(0, totalCount - items.count)
            persist()
            return true
        } catch {
            errorMessage = "Penghapusan dibatalkan atau gagal. Tidak ada yang terhapus, antrean hapus tetap tersimpan."
            return false
        }
    }

    nonisolated private static func delete(_ assets: [PHAsset]) async throws {
        let array = assets as NSArray
        try await PHPhotoLibrary.shared().performChanges {
            PHAssetChangeRequest.deleteAssets(array)
        }
    }

    // MARK: - Ulang dari awal

    func resetProgress() {
        keptIDs = []
        trashIDs = []
        persist()
        reload()
    }

    private func persist() {
        guard let kind else { return }
        defaults.set(Array(keptIDs), forKey: key("kept", kind))
        defaults.set(trashIDs, forKey: key("trash", kind))
    }
}
