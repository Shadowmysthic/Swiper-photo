package com.swipefoto.app

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.Formatter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Menyimpan antrean, hasil swipe, urutan, filter, dan perkiraan ruang. */
class SwipeViewModel(app: Application) : AndroidViewModel(app) {

    private data class Step(val item: MediaFile, val deleted: Boolean)

    private val prefs = app.getSharedPreferences("swipefoto", Context.MODE_PRIVATE)

    var hasAccess by mutableStateOf(false)
        private set
    var kind by mutableStateOf<MediaKind?>(null)
        private set
    var queue by mutableStateOf<List<MediaFile>>(emptyList())
        private set
    var trash by mutableStateOf<List<MediaFile>>(emptyList())
        private set
    var keptCount by mutableStateOf(0)
        private set
    var totalCount by mutableStateOf(0)
        private set
    var libraryCounts by mutableStateOf<Map<MediaKind, Int>>(emptyMap())
        private set
    var doneCounts by mutableStateOf<Map<MediaKind, Int>>(emptyMap())
        private set
    var sortOrder by mutableStateOf(
        runCatching { SortOrder.valueOf(prefs.getString("sort", "NEWEST") ?: "NEWEST") }
            .getOrDefault(SortOrder.NEWEST)
    )
        private set
    var filter by mutableStateOf(MediaFilter.ALL)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var totalFreedBytes by mutableStateOf(prefs.getLong("freed", 0L))
        private set
    var showReview by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var freedMessage by mutableStateOf<String?>(null)

    private var keptIds = mutableSetOf<Long>()
    private var trashIds = mutableListOf<Long>()
    private var scopeIds = setOf<Long>()
    private val history = ArrayDeque<Step>()
    private var loadJob: Job? = null

    val canUndo: Boolean get() = history.isNotEmpty()
    val reviewedCount: Int get() = maxOf(0, totalCount - queue.size)
    val trashBytes: Long get() = trash.sumOf { it.sizeBytes }

    val currentSort: SortOrder
        get() {
            val k = kind ?: return sortOrder
            return if (SortOrder.options(k).contains(sortOrder)) sortOrder else SortOrder.NEWEST
        }

    fun formatSize(bytes: Long): String = Formatter.formatShortFileSize(getApplication(), bytes)

    // region Izin dan menu

    fun checkAccess() {
        val ctx = getApplication<Application>()
        fun granted(permission: String) =
            ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

        val ok = if (Build.VERSION.SDK_INT >= 33) {
            granted(Manifest.permission.READ_MEDIA_IMAGES) ||
                granted(Manifest.permission.READ_MEDIA_VIDEO) ||
                (Build.VERSION.SDK_INT >= 34 && granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
        } else {
            granted(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val changed = ok != hasAccess
        hasAccess = ok
        if (ok && (changed || libraryCounts.isEmpty())) refreshCounts()
    }

    fun refreshCounts() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val counts = withContext(Dispatchers.IO) {
                MediaKind.values().associateWith { MediaRepository.count(ctx, it) }
            }
            libraryCounts = counts
            doneCounts = MediaKind.values().associateWith {
                loadIds("kept", it).size + loadIds("trash", it).size
            }
        }
    }

    fun select(kind: MediaKind) {
        this.kind = kind
        filter = MediaFilter.ALL
        reload()
    }

    fun backToMenu() {
        loadJob?.cancel()
        kind = null
        queue = emptyList()
        trash = emptyList()
        history.clear()
        isLoading = false
        showReview = false
        refreshCounts()
    }

    // endregion

    // region Urutan dan filter

    fun setSort(order: SortOrder) {
        if (order == sortOrder) return
        sortOrder = order
        prefs.edit().putString("sort", order.name).apply()
        reload()
    }

    fun setFilter(newFilter: MediaFilter) {
        if (newFilter == filter) return
        filter = newFilter
        reload()
    }

    // endregion

    // region Muat media

    private fun key(name: String, kind: MediaKind) = "${name}_${kind.name}"

    private fun loadIds(name: String, kind: MediaKind): List<Long> =
        (prefs.getString(key(name, kind), "") ?: "").split(",").mapNotNull { it.toLongOrNull() }

    private fun saveIds(name: String, kind: MediaKind, ids: Collection<Long>) {
        prefs.edit().putString(key(name, kind), ids.joinToString(",")).apply()
    }

    fun reload() {
        val k = kind ?: return
        keptIds = loadIds("kept", k).toMutableSet()
        trashIds = loadIds("trash", k).toMutableList()
        queue = emptyList()
        history.clear()
        isLoading = true

        val sort = currentSort
        val currentFilter = filter
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val ctx = getApplication<Application>()
            val all = withContext(Dispatchers.IO) { MediaRepository.load(ctx, k) }

            var scope = when (currentFilter) {
                MediaFilter.SCREENSHOT -> all.filter { it.isScreenshot }
                MediaFilter.LARGE -> all.filter { it.sizeBytes >= LARGE_VIDEO_BYTES }
                MediaFilter.ALL -> all
            }
            scope = when (sort) {
                SortOrder.NEWEST -> scope.sortedByDescending { it.dateMillis }
                SortOrder.OLDEST -> scope.sortedBy { it.dateMillis }
                SortOrder.LARGEST -> scope.sortedByDescending { it.sizeBytes }
            }

            val byId = all.associateBy { it.id }
            val trashItems = trashIds.mapNotNull { byId[it] }
            trashIds = trashItems.map { it.id }.toMutableList()
            keptIds = keptIds.filter { byId.containsKey(it) }.toMutableSet()
            scopeIds = scope.map { it.id }.toSet()
            totalCount = scope.size

            val trashSet = trashIds.toSet()
            queue = scope.filter { it.id !in keptIds && it.id !in trashSet }
            trash = trashItems
            updateKept()
            persist()
            isLoading = false
        }
    }

    private fun updateKept() {
        keptCount = keptIds.count { it in scopeIds }
    }

    // endregion

    // region Swipe

    /** delete = true untuk swipe kiri (masuk antrean hapus), false untuk swipe kanan (simpan). */
    fun swipe(delete: Boolean) {
        val first = queue.firstOrNull() ?: return
        queue = queue.drop(1)
        history.addLast(Step(first, delete))
        if (delete) {
            trash = trash + first
            trashIds.add(first.id)
        } else {
            keptIds.add(first.id)
            updateKept()
        }
        persist()
    }

    fun undo() {
        val step = history.removeLastOrNull() ?: return
        queue = listOf(step.item) + queue
        if (step.deleted) {
            trash = trash.filter { it.id != step.item.id }
            trashIds.remove(step.item.id)
        } else {
            keptIds.remove(step.item.id)
            updateKept()
        }
        persist()
    }

    /** Dipakai di layar tinjau: item dikeluarkan dari antrean hapus dan dianggap disimpan. */
    fun restore(item: MediaFile) {
        trash = trash.filter { it.id != item.id }
        trashIds.remove(item.id)
        history.removeAll { it.item.id == item.id }
        keptIds.add(item.id)
        updateKept()
        persist()
    }

    // endregion

    // region Pemindahan ke sampah

    /** Dipanggil setelah dialog konfirmasi sistem Android selesai. */
    fun onTrashResult(ok: Boolean) {
        if (!ok) return
        val freed = trashBytes
        val inScope = trash.count { it.id in scopeIds }
        trash = emptyList()
        trashIds.clear()
        history.clear()
        totalCount = maxOf(0, totalCount - inScope)
        totalFreedBytes += freed
        prefs.edit().putLong("freed", totalFreedBytes).apply()
        showReview = false
        persist()
        freedMessage = "Perkiraan ${formatSize(freed)} akan kosong setelah Sampah di galeri dikosongkan, atau otomatis setelah 30 hari."
    }

    // endregion

    fun resetProgress() {
        keptIds.clear()
        trashIds.clear()
        persist()
        reload()
    }

    private fun persist() {
        val k = kind ?: return
        saveIds("kept", k, keptIds)
        saveIds("trash", k, trashIds)
    }
}
