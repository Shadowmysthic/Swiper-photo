package com.swipefoto.app

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

object MediaRepository {

    private fun collection(kind: MediaKind): Uri =
        if (kind == MediaKind.VIDEO) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    fun count(context: Context, kind: MediaKind): Int {
        val cursor = context.contentResolver.query(
            collection(kind),
            arrayOf(MediaStore.MediaColumns._ID),
            null,
            null,
            null
        )
        return cursor?.use { it.count } ?: 0
    }

    /** Memuat semua foto atau video (item yang sudah di sampah tidak ikut). */
    fun load(context: Context, kind: MediaKind): List<MediaFile> {
        val isVideo = kind == MediaKind.VIDEO
        val base = collection(kind)

        val columns = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.RELATIVE_PATH
        )
        if (isVideo) columns.add(MediaStore.MediaColumns.DURATION)

        val result = ArrayList<MediaFile>()
        val cursor = context.contentResolver.query(base, columns.toTypedArray(), null, null, null)
        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val takenCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val pathCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val durCol = if (isVideo) c.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION) else -1

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val taken = c.getLong(takenCol)
                val added = c.getLong(addedCol) * 1000L
                val path = c.getString(pathCol) ?: ""
                result.add(
                    MediaFile(
                        id = id,
                        uri = ContentUris.withAppendedId(base, id),
                        isVideo = isVideo,
                        dateMillis = if (taken > 0) taken else added,
                        sizeBytes = c.getLong(sizeCol),
                        durationMs = if (durCol >= 0) c.getLong(durCol) else 0L,
                        isScreenshot = path.contains("screenshot", ignoreCase = true)
                    )
                )
            }
        }
        return result
    }
}
