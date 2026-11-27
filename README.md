# GT30 PRO Launcher

Launcher Android native yang dirancang **khusus untuk Infinix GT 30 Pro** (model X6873) —
homescreen dengan wallpaper kustom, laci aplikasi, dan pusat **Super App**.

## Perangkat target

| Spesifikasi | Nilai |
|---|---|
| Layar | 6,78" AMOLED, 1224 × 2720 px (≈ 445 × 989 dp), 144 Hz, rasio 20:9 |
| Kamera depan | 13 MP, punch-hole tengah atas |
| Sistem | Android 15 (XOS 15), API 35 |
| Bodi | 163,7 × 75,8 × 8,0 mm |

## Fitur v0.1

- **Homescreen minimalis** — wallpaper render GT 30 Pro dengan jam, tanggal,
  baterai, animasi jalur dan ikon cuaca dengan suhu (sesuai preview).
- **Akses via gesture** — swipe ke atas = laci aplikasi; double-tap = Super App;
  long-press = menu pintasan (Super App / Semua Aplikasi / Setelan).
- **App Drawer** — daftar semua aplikasi terpasang + pencarian real-time.
- **Super App** — grid ikon untuk Jam, Kalender dan cuaca (tanpa label suhu).
  Tanggal di homescreen dapat diketuk untuk langsung membuka Kalender; tombol
  kembali mengembalikan ke homescreen. Kalender memakai font Doto; tema dapat
  dipilih lewat ikon titik: Sistem (layar berhuruf A, default mengikuti perangkat),
  Terang (matahari), atau Gelap (bulan). Ketuk tanggal untuk melihat detail
  libur dan memilih tanggal acara baru; tombol tambah di kanan bulan membuat acara.
  Agenda mingguan menampilkan seluruh acara Senin–Minggu yang terlihat, urut
  tanggal dan waktu (acara tanpa waktu terakhir pada hari itu). Agenda bulanan
  hanya menampilkan acara hari ini, walaupun bulan atau tanggal lain dipilih. Kalender
  terbuka satu minggu, panah bawah membuka tampilan bulan; swipe pada kisi
  berpindah minggu atau bulan sesuai tampilan. Ketuk nama bulan untuk navigasi
  tanpa gestur. Tanggal berbentuk squircle: garis merah menandai libur nasional,
  amber peringatan, biru acara; beberapa garis berarti beberapa penanda. Akhir
  pekan dan libur nasional tetap merah. Tahun berada di sebelah tombol kembali;
  informasi tambahan tersedia lewat ikon **Info kalender**.
  Data libur terkurasi dari GitHub diperiksa setiap kali kalender dibuka (permintaan
  bersamaan digabung); jika offline atau terbatas, data terakhir/bawaan dipakai.
  Sumber diperbarui manual, bukan verifikasi otomatis pemerintah. Acara disimpan
  secara lokal tanpa sinkronisasi atau cadangan; form mencakup jenis, tanggal dan
  waktu mulai/selesai (maksimum 366 hari), pengulangan dan pengingat perangkat.
  Pengulangan khusus adalah setiap 1–999 hari, minggu, bulan atau tahun; tanggal
  yang tidak ada pada bulan/tahun berikutnya memakai hari terakhir bulan tersebut,
  dan kembali ke tanggal semula jika ada. Penghapusan menghapus seluruh rangkaian.
  Pengingat Android mendukung saat mulai, 10 menit, 1 jam, atau 1 hari sebelumnya;
  memerlukan izin notifikasi, dan dapat terlambat tanpa izin alarm tepat waktu.
  Pengingat dilewatkan jika waktunya sudah lewat dan dijadwalkan kembali setelah
  restart atau perubahan waktu/zona. Android dapat membatasi pengingat, misalnya
  setelah aplikasi dihentikan paksa. Preview browser tidak mengirim notifikasi;
  email tidak didukung. Acara lama tetap dapat ditampilkan dan dihapus.
- **Launcher penuh** — muncul di pemilih Home (tombol Home membuka launcher ini).

## Struktur proyek

```
app/src/main/          sumber aplikasi (Java + resource XML, tanpa dependensi eksternal)
app/src/main/assets/   wallpaper default
tools/                 skrip build lokal (tanpa Gradle) + generator ikon
preview/               mockup web 1:1 Infinix GT 30 Pro (diskusi desain)
.github/workflows/     CI build APK di GitHub Actions
```

## Unduh APK

APK siap pakai tersedia di folder [`apk/`](apk/) di repo ini
(mis. `apk/v0.1.0/GT30PROLauncher-v0.1.0-release.apk`) — unduh langsung dari
HP lewat GitHub. Rilis GitHub (`Releases`) otomatis berisi APK saat build CI
dijalankan dengan tag `v*`.

## Build

### Di GitHub (CI)
Push tag `v*` (mis. `v0.1.0`) atau jalankan workflow **Build APK** manual
(*Actions → Build APK → Run workflow*). Artifact `gt30pro-launcher-apks`
berisi APK debug & release. Tag `v*` otomatis membuat GitHub Release.

### Lokal (tanpa Android SDK/Gradle)
```bash
bash tools/bootstrap_toolchain.sh   # sekali jalan: aapt2, d8, JDK, android.jar
python3 tools/gen_icons.py          # opsional: regenerate ikon
bash tools/build_local.sh           # hasil: dist/*.apk
```
Pipeline: `aapt2 compile/link → javac → d8 → signing v1+v2+v3 → verifikasi`.

## Signing

- **Debug** — keystore debug standar (`~/.android/debug.keystore`).
- **Release** — `~/.android/gt30pro-release.keystore` (otomatis dibuat untuk
  build). ⚠️ **Ganti dengan keystore milik Anda sendiri** sebelum rilis ke
  Play Store — keystore saat ini hanya untuk sideloading.

## Stack

Java (source/target 8) + framework Android (tanpa androidx) — dipilih agar bisa
dibuild mandiri tanpa dependensi eksternal (aapt2 + javac + d8 + signing v1/v2/v3
semuanya dari toolchain ringan). Arsitektur disiapkan untuk dimigrasi ke
Kotlin + Jetpack Compose saat pengembangan penuh.

## Desain

Mengikuti mockup `preview/index.html` (proporsi 1:1 perangkat nyata):
tema gelap `#0A0C0F` dengan aksen oranye `#F7A23B` (nuansa Dark Flare).

## Cuaca berbasis lokasi

Ketuk ikon cuaca di homescreen untuk mengaktifkan cuaca. Ikon cuaca di grid
Super App membuka halaman prakiraan: cuaca dan suhu saat ini, prakiraan 24 jam
dan 7 hari, peluang hujan, suhu terasa, kelembapan, angin, tekanan udara, serta
matahari terbit dan terbenam. Halaman berlatar langit; ketuk ikon perbarui di kanan
judul CUACA untuk meminta izin atau menyegarkan data. Radar, kualitas udara dan notifikasi
cuaca belum tersedia.
Jika ikon homescreen sulit terlihat, tekan lama area kosong homescreen lalu
pilih **Aktifkan / perbarui cuaca**.
Aplikasi meminta izin **lokasi perkiraan** dan memakai penyedia lokasi jaringan;
koordinat dibulatkan ke dua desimal dan dikirim via HTTPS ke Open-Meteo untuk
suhu, kondisi dan prakiraan cuaca. Setelah persetujuan, Android juga memakai
layanan geocoding perangkat untuk mencari nama kota/daerah dari koordinat yang
sama; penyedia layanan tersebut bergantung pada perangkat. Jika tidak disetujui
atau tidak tersedia, koordinat dua desimal ditampilkan. Browser preview menampilkan
koordinat perkiraan tanpa mencari nama kota. Nama dan koordinat lokasi tidak
disimpan, dan tidak ada pelacakan latar belakang. Selama halaman aktif, data diperiksa paling sering setiap 30 menit;
ketuk ikon untuk memperbarui manual. Cache suhu/kondisi terakhir disimpan lokal,
tidak dibackup, dan hanya ditampilkan hingga dua jam. Tanpa izin, lokasi, atau
koneksi, tampilan menunjukkan `--°` (atau data terakhir yang masih berlaku,
yang dapat berasal dari lokasi sebelumnya).
Izin dapat dicabut melalui setelan Android. Preview browser juga meminta izin
setelah ikon diketuk, dan memerlukan HTTPS, geolocation dan akses Open-Meteo;
nilai awal `--°` bukan simulasi suhu nyata. Sebelum data diterima, gambar awan dan matahari adalah ikon umum, bukan kondisi cuaca aktual.
