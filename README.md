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

Ketuk ikon cuaca di homescreen atau grid Super App untuk mengaktifkan cuaca.
Aplikasi meminta izin **lokasi perkiraan** dan memakai penyedia lokasi jaringan;
koordinat dibulatkan ke dua desimal dan dikirim via HTTPS ke Open-Meteo untuk
suhu dan kode cuaca saat ini. Tidak ada pelacakan latar belakang atau penyimpanan
koordinat. Selama halaman aktif, data diperiksa paling sering setiap 30 menit;
ketuk ikon untuk memperbarui manual. Cache suhu/kondisi terakhir disimpan lokal,
tidak dibackup, dan hanya ditampilkan hingga dua jam. Tanpa izin, lokasi, atau
koneksi, tampilan menunjukkan `--°` (atau data terakhir yang masih berlaku).
Izin dapat dicabut melalui setelan Android. Preview browser juga meminta izin
setelah ikon diketuk, dan memerlukan HTTPS, geolocation dan akses Open-Meteo;
nilai awal `--°` bukan simulasi suhu nyata. Sebelum data diterima, gambar awan dan matahari adalah ikon umum, bukan kondisi cuaca aktual.
