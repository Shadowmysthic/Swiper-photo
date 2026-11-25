package com.swipefoto.app

import android.net.Uri

const val LARGE_VIDEO_BYTES = 100L * 1024 * 1024

enum class MediaKind(val title: String, val nameId: String, val emoji: String) {
    PHOTO("Foto", "foto", "📸"),
    VIDEO("Video", "video", "🎬")
}

enum class SortOrder(val title: String) {
    NEWEST("Terbaru dulu"),
    OLDEST("Terlama dulu"),
    LARGEST("Terbesar dulu");

    companion object {
        fun options(kind: MediaKind): List<SortOrder> =
            if (kind == MediaKind.PHOTO) listOf(NEWEST, OLDEST) else listOf(NEWEST, OLDEST, LARGEST)
    }
}

enum class MediaFilter(val title: String) {
    ALL("Semua"),
    SCREENSHOT("Screenshot"),
    LARGE("Video besar (100 MB+)");

    companion object {
        fun options(kind: MediaKind): List<MediaFilter> =
            if (kind == MediaKind.PHOTO) listOf(ALL, SCREENSHOT) else listOf(ALL, LARGE)
    }
}

data class MediaFile(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val dateMillis: Long,
    val sizeBytes: Long,
    val durationMs: Long,
    val isScreenshot: Boolean
)
