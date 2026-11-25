# Swipe Foto untuk Android

Fiturnya sama dengan versi iPhone: menu Foto atau Video, swipe kanan simpan dan kiri hapus, antrean hapus yang ditinjau dulu, filter, urutan, dan perkiraan ruang.

## Syarat
- Android 11 atau lebih baru (butuh fitur Sampah bawaan Android)
- Akun GitHub yang sama dengan repositori Swiper-photo

## Cara membuat APK
1. Di repositori Swiper-photo, klik Add file > Upload files lalu seret folder `android` dari zip ini. Klik Commit changes
2. Buat file `.github/workflows/android.yml` di root repositori (Add file > Create new file, ketik nama lengkap dengan garis miring) lalu tempel isinya dari zip. Klik Commit changes
3. Buka tab Actions, tunggu workflow "Build APK" selesai (sekitar 4 sampai 8 menit untuk build pertama)
4. Klik run yang hijau, gulir ke Artifacts, unduh `SwipeFoto-apk`, lalu ekstrak untuk mendapat `app-debug.apk`

## Cara memasang di Android
1. Pindahkan `app-debug.apk` ke ponsel (kabel USB, Google Drive, atau Telegram ke diri sendiri)
2. Ketuk file APK. Kalau diminta, izinkan "Pasang aplikasi tidak dikenal" untuk aplikasi yang kamu pakai membuka file
3. Ketuk Pasang. Kalau Play Protect memberi peringatan, pilih "Pasang saja"
4. Buka Swipe Foto dan izinkan akses foto dan video. Pilih "Izinkan semua" agar semua galeri terbaca

APK tidak punya masa aktif 7 hari seperti di iPhone.

## Cara pakai
- Geser kartu ke kanan atau tekan tombol hijau: simpan
- Geser kiri atau tekan tombol merah: masuk antrean hapus, belum terhapus
- Tombol Undo membatalkan swipe terakhir
- Ketuk chip tempat sampah di kanan atas untuk meninjau
- Chip urutan dan filter ada di bawah bar progres

## Pengaman sebelum foto hilang
1. Swipe kiri hanya menampung
2. Layar tinjau menampilkan semua item, ketuk untuk mengeluarkan, lalu tombol Hapus meminta konfirmasi
3. Android menampilkan dialog konfirmasi sistem. Item masuk ke Sampah galeri dan bisa dipulihkan 30 hari
