import Photos
import UIKit

/// Mengelola antrean foto, hasil swipe, dan penghapusan sungguhan lewat PhotoKit.
@MainActor
final class PhotoStore: ObservableObject {
    enum Access { case unknown, granted, denied }

    @Published private(set) var access: Access = .unknown
    @Published private(set) var queue: [PHAsset] = []   // belum direview
    @Published private(set) var trash: [PHAsset] = []   // antrean hapus (swipe kiri)
    @Published private(set) var keptCount = 0           // swipe kanan
    @Published private(set) var totalCount = 0
    @Published var errorMessage: String?

    private struct Step {
        let asset: PHAsset
        let deleted: Bool
    }

    private var history: [Step] = []
    private var keptIDs: Set<String>
    private var trashIDs: [String]
    private let defaults = UserDefaults.standard

    init() {
        keptIDs = Set(defaults.stringArray(forKey: "keptIDs") ?? [])
        trashIDs = defaults.stringArray(forKey: "trashIDs") ?? []
    }

    var canUndo: Bool { !history.isEmpty }
    var reviewedCount: Int { keptCount + trash.count }

    // MARK: - Izin dan muat foto

    func start() async {
        var status = PHPhotoLibrary.authorizationStatus(for: .readWrite)
        if status == .notDetermined {
            status = await PHPhotoLibrary.requestAuthorization(for: .readWrite)
        }
        switch status {
        case .authorized, .limited:
            access = .granted
            reload()
        default:
            access = .denied
        }
    }

    func reload() {
        let options = PHFetchOptions()
        options.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
        let result = PHAsset.fetchAssets(with: .image, options: options)

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

    /// Dipakai di layar tinjau: foto dikeluarkan dari antrean hapus dan dianggap disimpan.
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
            errorMessage = "Penghapusan dibatalkan atau gagal. Tidak ada foto yang terhapus, antrean hapus tetap tersimpan."
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
        defaults.set(Array(keptIDs), forKey: "keptIDs")
        defaults.set(trashIDs, forKey: "trashIDs")
    }
}
