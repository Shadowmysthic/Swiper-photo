package com.swipefoto.app

import android.Manifest
import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun requiredPermissions(): Array<String> =
    when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

@Composable
fun App(vm: SwipeViewModel) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.checkAccess() }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        vm.checkAccess()
        if (!vm.hasAccess) permissionLauncher.launch(requiredPermissions())
    }

    Box(Modifier.fillMaxSize().background(AppBackground)) {
        when {
            !vm.hasAccess -> DeniedScreen { permissionLauncher.launch(requiredPermissions()) }
            vm.kind == null -> MenuScreen(vm)
            else -> {
                DeckScreen(vm)
                if (vm.showReview) ReviewScreen(vm)
            }
        }
    }

    vm.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { vm.errorMessage = null },
            confirmButton = { TextButton(onClick = { vm.errorMessage = null }) { Text("OK") } },
            title = { Text("Oops") },
            text = { Text(message) }
        )
    }
    vm.freedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { vm.freedMessage = null },
            confirmButton = { TextButton(onClick = { vm.freedMessage = null }) { Text("Mantap") } },
            title = { Text("Selesai! 🎉") },
            text = { Text(message) }
        )
    }
}

@Composable
private fun DeniedScreen(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔒", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text("Akses galeri dibutuhkan", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "Izinkan akses ke foto dan video. Kalau izin sudah ditolak, buka Pengaturan > Aplikasi > Swipe Foto > Izin.",
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = Palette.Accent)) {
            Text("Beri izin")
        }
    }
}

// region Menu awal

@Composable
fun MenuScreen(vm: SwipeViewModel) {
    val transition = rememberInfiniteTransition(label = "bounce")
    val bounce by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bounceValue"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🧹", fontSize = 72.sp, modifier = Modifier.offset(y = bounce.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            "Yuk beres-beres galeri!",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Palette.Ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Geser kanan untuk simpan, geser kiri untuk hapus. Seru kayak main game!",
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(24.dp))
        MediaKind.values().forEach { kind ->
            KindCard(vm, kind)
            Spacer(Modifier.height(16.dp))
        }
        if (vm.totalFreedBytes > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                "✨ Total ruang dibebaskan sejauh ini: ${vm.formatSize(vm.totalFreedBytes)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Palette.Keep
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "✅ Tidak ada yang dihapus sebelum kamu konfirmasi",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun KindCard(vm: SwipeViewModel, kind: MediaKind) {
    val total = vm.libraryCounts[kind] ?: 0
    val done = minOf(vm.doneCounts[kind] ?: 0, total)
    val percent = if (total == 0) 0 else (done * 100L / total).toInt()
    val colors = if (kind == MediaKind.PHOTO) {
        listOf(Palette.Accent, Color(0xFFB86BFA))
    } else {
        listOf(Palette.Trash, Color(0xFFFF9959))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(colors))
            .clickable(enabled = total > 0) { vm.select(kind) }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(70.dp).clip(RoundedCornerShape(35.dp)).background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(kind.emoji, fontSize = 36.sp)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(kind.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "$total ${kind.nameId} • $percent% sudah dicek",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp
            )
        }
        Text("›", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

// endregion

// region Layar swipe

@Composable
fun DeckScreen(vm: SwipeViewModel) {
    val kind = vm.kind ?: return
    var confirmReset by remember { mutableStateOf(false) }

    BackHandler {
        if (vm.showReview) vm.showReview = false else vm.backToMenu()
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 8.dp)
    ) {
        DeckHeader(vm, kind) { confirmReset = true }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                vm.isLoading -> LoadingView(vm, kind)
                vm.totalCount == 0 -> EmptyView(vm, kind)
                vm.queue.isEmpty() -> DoneView(vm, kind)
                else -> {
                    val visible = vm.queue.take(2)
                    val topId = visible.first().id
                    visible.asReversed().forEach { item ->
                        androidx.compose.runtime.key(item.id) {
                            SwipeCard(
                                item = item,
                                sizeText = vm.formatSize(item.sizeBytes),
                                isTop = item.id == topId,
                                onSwipe = { delete -> vm.swipe(delete) }
                            )
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            val busy = vm.isLoading
            ActionButton("Hapus", "✕", Palette.Trash, Color.White, 70.dp, enabled = !busy && vm.queue.isNotEmpty()) {
                vm.swipe(true)
            }
            Spacer(Modifier.width(26.dp))
            Box(Modifier.padding(top = 9.dp)) {
                ActionButton("Undo", "↩", Color.White, Palette.Sun, 52.dp, enabled = !busy && vm.canUndo) {
                    vm.undo()
                }
            }
            Spacer(Modifier.width(26.dp))
            ActionButton("Simpan", "♥", Palette.Keep, Color.White, 70.dp, enabled = !busy && vm.queue.isNotEmpty()) {
                vm.swipe(false)
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Mulai ulang dari awal?") },
            text = { Text("Progres ${kind.nameId} direset. Tidak ada yang dihapus dari galeri.") },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; vm.resetProgress() }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun DeckHeader(vm: SwipeViewModel, kind: MediaKind, onReset: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val progress = if (vm.totalCount == 0) 0f else vm.reviewedCount.toFloat() / vm.totalCount
    val cheer = when {
        progress < 0.25f -> "Ayo mulai! 💪"
        progress < 0.5f -> "Mantap, lanjut! 🔥"
        progress < 0.75f -> "Sudah lewat setengah! 🚀"
        progress < 1f -> "Dikit lagi! 🏁"
        else -> "Bersih! ✨"
    }

    Column(
        Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIcon("🏠") { vm.backToMenu() }
            Spacer(Modifier.width(8.dp))
            Box {
                RoundIcon("⋯") { menuOpen = true }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Mulai ulang progres ${kind.nameId}") },
                        onClick = { menuOpen = false; onReset() }
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${kind.emoji} ${vm.queue.size} ${kind.nameId} lagi",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Palette.Ink
            )
            Spacer(Modifier.weight(1f))
            TrashChip(vm)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(maxOf(progress, 0.03f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Palette.Keep, Palette.Accent)))
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("💚 Simpan ${vm.keptCount}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            Text(cheer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            Text("🗑 Antre ${vm.trash.size}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortChip(vm, kind)
            FilterChip(vm, kind)
        }
    }
}

@Composable
private fun TrashChip(vm: SwipeViewModel) {
    val hasItems = vm.trash.isNotEmpty()
    val label = if (hasItems) "🗑 ${vm.trash.size} · ${vm.formatSize(vm.trashBytes)}" else "🗑 0"
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (hasItems) Color.White else Palette.Ink,
        modifier = Modifier
            .shadow(3.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(if (hasItems) Palette.Trash else Color.White)
            .clickable { vm.showReview = true }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun SortChip(vm: SwipeViewModel, kind: MediaKind) {
    var open by remember { mutableStateOf(false) }
    Box {
        Chip("↕ ${vm.currentSort.title}") { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortOrder.options(kind).forEach { option ->
                DropdownMenuItem(
                    text = { Text((if (option == vm.currentSort) "✓ " else "") + option.title) },
                    onClick = { open = false; vm.setSort(option) }
                )
            }
        }
    }
}

@Composable
private fun FilterChip(vm: SwipeViewModel, kind: MediaKind) {
    var open by remember { mutableStateOf(false) }
    Box {
        Chip("🎯 ${vm.filter.title}", highlighted = vm.filter != MediaFilter.ALL) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MediaFilter.options(kind).forEach { option ->
                DropdownMenuItem(
                    text = { Text((if (option == vm.filter) "✓ " else "") + option.title) },
                    onClick = { open = false; vm.applyFilter(option) }
                )
            }
        }
    }
}

@Composable
private fun LoadingView(vm: SwipeViewModel, kind: MediaKind) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Palette.Accent)
        Spacer(Modifier.height(12.dp))
        Text("Menyiapkan ${kind.nameId}...", fontWeight = FontWeight.SemiBold, color = Color.Gray)
    }
}

@Composable
private fun EmptyView(vm: SwipeViewModel, kind: MediaKind) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Text("🤷", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text("Tidak ada yang cocok", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tidak ada ${kind.nameId} dengan filter \"${vm.filter.title}\". Coba pilih filter lain.",
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        if (vm.filter != MediaFilter.ALL) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { vm.applyFilter(MediaFilter.ALL) },
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Accent)
            ) { Text("Tampilkan semua") }
        }
    }
}

@Composable
private fun DoneView(vm: SwipeViewModel, kind: MediaKind) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("🎉", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Semua ${kind.nameId} sudah dicek!",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Palette.Ink,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            if (vm.trash.isEmpty()) {
                Text("Tidak ada yang perlu dihapus. Galerimu sudah rapi ✨", textAlign = TextAlign.Center, color = Color.Gray)
            } else {
                Text(
                    "Ada ${vm.trash.size} ${kind.nameId} di antrean hapus (sekitar ${vm.formatSize(vm.trashBytes)}). Tinjau dulu sebelum dihapus ya.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { vm.showReview = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Trash)
                ) { Text("Tinjau dan hapus") }
            }
        }
        Confetti(Modifier.fillMaxSize())
    }
}

// endregion

// region Layar tinjau (konfirmasi kedua)

@Composable
fun ReviewScreen(vm: SwipeViewModel) {
    val ctx = LocalContext.current
    val kind = vm.kind ?: return
    var confirm by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> vm.onTrashResult(result.resultCode == Activity.RESULT_OK) }

    Box(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { vm.showReview = false }) { Text("Tutup") }
                Spacer(Modifier.weight(1f))
                Text("Tinjau dulu ya 👀", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(64.dp))
            }

            if (vm.trash.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Antrean hapus kosong 🙌", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(100.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(vm.trash, key = { it.id }) { item ->
                        Thumb(item, vm.formatSize(item.sizeBytes)) { vm.restore(item) }
                    }
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "💾 Perkiraan ruang yang dibebaskan: ${vm.formatSize(vm.trashBytes)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Palette.Ink
                    )
                    Text(
                        "Ruang baru benar-benar kosong setelah Sampah di galeri dikosongkan, atau otomatis setelah 30 hari.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Ketuk sebuah ${kind.nameId} untuk membatalkan penghapusannya",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Button(
                        onClick = { confirm = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Palette.Trash)
                    ) {
                        Text("Hapus ${vm.trash.size} ${kind.nameId}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Hapus ${vm.trash.size} ${kind.nameId}?") },
            text = {
                Text(
                    "Sekitar ${vm.formatSize(vm.trashBytes)} dipindah ke Sampah dan masih bisa dipulihkan selama 30 hari. Android akan meminta konfirmasi sekali lagi."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    try {
                        val request = MediaStore.createTrashRequest(
                            ctx.contentResolver,
                            vm.trash.map { it.uri },
                            true
                        )
                        launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                    } catch (e: Exception) {
                        vm.errorMessage = "Gagal membuka konfirmasi sistem: ${e.message}"
                    }
                }) { Text("Hapus", color = Palette.Trash) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Batal") }
            }
        )
    }
}

// endregion
