# Swipe Foto: Panduan untuk Windows 11

Karena tidak ada Mac, aplikasi dikompilasi di server Mac milik GitHub (gratis), lalu file IPA-nya dipasang ke iPhone dengan Sideloadly dari Windows.

## Yang dibutuhkan

1. Akun GitHub gratis (github.com)
2. iPhone dengan iOS 16 atau lebih baru dan kabel USB
3. Apple ID biasa
4. Sideloadly (sideloadly.io) untuk Windows
5. iTunes dan iCloud versi installer dari apple.com, bukan versi Microsoft Store. Sideloadly butuh ini agar Windows mengenali iPhone

## Bagian 1: Kompilasi di GitHub

1. Login ke GitHub, klik tanda + di kanan atas, pilih New repository
2. Nama: `swipe-foto`. Pilih **Public** agar menit build macOS gratis. Klik Create repository
3. Klik link "uploading an existing file"
4. Ekstrak zip proyek ini di Windows, lalu seret isinya ke halaman upload: folder `Sources` dan file `project.yml`
5. Klik Commit changes
6. Folder tersembunyi `.github` sering tidak ikut terseret. Buat manual: klik Add file > Create new file, ketik nama `.github/workflows/build.yml` (ketik garis miring supaya folder terbentuk), lalu tempel isi file `build.yml` dari zip. Klik Commit changes
7. Buka tab **Actions**. Build berjalan otomatis sekitar 3 sampai 6 menit. Jika tidak jalan, pilih "Build IPA" lalu Run workflow
8. Setelah centang hijau, klik run tersebut, gulir ke bagian Artifacts, unduh `SwipeFoto-ipa`, lalu ekstrak zipnya untuk mendapat `SwipeFoto.ipa`

Jika build gagal (tanda silang merah), klik langkah yang merah dan salin pesan errornya ke Claude untuk diperbaiki.

## Bagian 2: Pasang ke iPhone

1. Pasang iTunes dan iCloud dari apple.com, lalu Sideloadly
2. Sambungkan iPhone, ketuk "Percayai Komputer Ini"
3. Aktifkan Developer Mode: Pengaturan > Privasi & Keamanan > Mode Pengembang, nyalakan lalu restart
4. Buka Sideloadly, pilih iPhone kamu, seret `SwipeFoto.ipa` ke jendelanya, isi Apple ID, lalu klik Start
5. Jika muncul "Untrusted Developer", buka Pengaturan > Umum > VPN & Manajemen Perangkat, pilih Apple ID kamu, lalu Percayai
6. Buka Swipe Foto dan pilih **Izinkan Akses ke Semua Foto**

Apple ID gratis membuat aplikasi kedaluwarsa setelah 7 hari. Ulangi langkah 4 lagi untuk memperbarui, progres swipe tidak hilang. Disarankan memakai Apple ID khusus untuk sideloading jika kamu tidak nyaman memasukkan Apple ID utama.

## Cara pakai

| Aksi | Hasil |
|---|---|
| Geser kanan atau tombol hijau | Foto disimpan |
| Geser kiri atau tombol merah | Foto masuk antrean hapus, belum terhapus |
| Tombol oranye | Batalkan swipe terakhir |
| Ikon tempat sampah kanan atas | Layar tinjau |

Tiga lapis pengaman sebelum foto hilang:

1. Swipe kiri hanya menampung ke antrean
2. Layar tinjau menampilkan semua foto di antrean. Ketuk foto untuk mengeluarkannya, lalu tekan Hapus dan konfirmasi
3. iOS meminta konfirmasi sekali lagi, dan foto yang terhapus masuk ke Dihapus Baru-baru Ini selama 30 hari

Progres tersimpan otomatis. Menu titik tiga di kiri atas bisa mereset progres tanpa menghapus foto.

## Catatan

- Hanya foto yang diproses, video belum
- Foto iCloud yang belum diunduh akan diunduh sebentar saat ditampilkan
- Kode belum pernah dikompilasi sebelumnya, jadi build pertama bisa menampilkan error kecil
