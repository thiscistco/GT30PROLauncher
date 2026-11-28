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

Saat membuka **Jam**, HUD waktu lokal HH:mm:ss tampil di tengah dalam lingkaran
jam analog di bagian atas bingkai squircle. Angka jam putih berlatar gelap,
dan ikon Alarm, Jam Dunia, Timer, dan Stopwatch rata tengah di bawah lingkaran.
Fitur terbuka sebagai panel pada halaman yang sama; daftar alarm menampilkan
hitung mundur menuju alarm berikutnya. Editor alarm menyediakan pilihan hari
Senin–Minggu dalam satu baris, pemilih waktu roda 24 jam, serta ikon nada dan
getar. Editor menyesuaikan ruang yang tersedia agar lebih ringkas; pada layar
kecil kontennya tetap dapat digulir. Ketuk waktu pada daftar untuk mengubah
alarm; slider dan ikon tempat
sampah berada di kanan.
Jam Dunia menampilkan maksimal lima kota; pilihan sebelumnya yang melebihi batas
ditampilkan lima kota pertama menurut urutan pilihan, tanpa mengubah data tersimpan
sampai pilihan baru disimpan. Kontrol Jam Dunia, Timer, dan Stopwatch tetap di
baris judul saat daftar digulir; lap diurutkan dari pertama ke terakhir. Pemilihan kota memakai slider, kota Indonesia lebih dahulu lalu kota lain
berdasarkan perkiraan jarak dari Jakarta; timer memakai roda jam–menit–detik.
Ketuk lagi ikon aktif atau gunakan kembali untuk menuju HUD.

## Desain aplikasi bawaan

Jam, Kalender, Cuaca, Super App, dan laci aplikasi memakai palet dan kontrol
yang sama. Pilihan tema pada halaman-halaman tersebut berlaku bersama; default
mengikuti perangkat. Palet indikator homescreen mengikuti mode sistem secara terpisah.
Aplikasi pihak ketiga tetap menggunakan desainnya sendiri. Panduan untuk
aplikasi bawaan baru tersedia di `docs/app-ui.md`.

## Fitur v0.1

- **Homescreen minimalis** — wallpaper render GT 30 Pro dengan jam, tanggal,
  baterai, animasi jalur dan ikon cuaca dengan suhu (sesuai preview), tanpa lapisan kaca atau efek pantulan.
  Warna indikator mengikuti mode terang/gelap; ikon non-gunmetal memakai amber (#FFCD6A).
  Jam digeser naik 2.182 piksel dan ke kanan 750 piksel gambar, serta diperbesar 14% dari ukuran awal. Menit digeser 1 px ke kiri dan atas; lima titik gunmetal berdetak bergantian.
  Angka indikator baterai lama di sebelah kanan disembunyikan; ikon pengisian tetap di bawah dan informasi baterai tersedia pada label dua kolom.
- **Info baterai homescreen** — label dua kolom tanpa judul atau latar memakai font Doto tebal dan nama item berwarna amber seperti ikon, digeser naik 97 piksel CSS dan ke kiri 5 piksel CSS, dengan kolom kedua 25 piksel CSS ke kiri dari posisi semula, dan menampilkan level, status, sumber daya, kondisi yang dilaporkan Android, suhu, tegangan, arus saat ini/rata-rata, muatan dan energi tersisa, siklus, teknologi, keberadaan baterai, status baterai rendah, dan perkiraan waktu hingga penuh. Data diperbarui selama homescreen terbuka; arus/energi diperiksa kembali setiap 15 detik. Angka muatan adalah **sisa**, bukan kapasitas desain atau kapasitas maksimum; kondisi “Baik” bukan persentase kesehatan baterai. Estimasi penuh berasal dari Android, bukan hasil perhitungan launcher. Tidak semua perangkat/versi Android melaporkan semua nilai; yang tidak tersedia ditampilkan sebagai “Tidak tersedia”. Arus bertanda mengikuti laporan perangkat dan dapat bervariasi. Preview memakai data **simulasi** dan tidak membaca baterai perangkat.
- **Akses via gesture** — swipe ke atas dari mana saja di homescreen = Super App; dua swipe ke atas dalam 2 detik membuka daftar aplikasi; swipe ke bawah di kiri = panel notifikasi, di kanan = panel kontrol Android; double-tap pada area kosong homescreen mengunci layar;
  long-press = menu pintasan (Super App / Semua Aplikasi / Setelan). Panel sistem menggunakan API Android non-publik: versi Android atau produsen tertentu dapat memblokirnya; jika gagal, geser dari tepi atas layar. Preview hanya menyimulasikan panel dan kunci layar. Kunci layar memerlukan persetujuan admin perangkat Android; izin dapat dicabut di Setelan. Pada beberapa perangkat, penguncian administratif dapat mewajibkan PIN saat membuka kembali.
- **App Drawer** — daftar semua aplikasi terpasang + pencarian real-time.
- **Super App** — grid ikon untuk Jam, Kalender, Cuaca, Jadwal Mengajar dan Setelan. Tekan dan tahan lalu seret ikon ke ikon lain untuk menyimpan urutannya. Preview menyimpan urutannya terpisah di browser (keyboard: Alt + panah).
  Tanggal di homescreen dapat diketuk untuk langsung membuka Kalender; tombol
  kembali mengembalikan ke homescreen. Kalender memakai font Doto; tema dapat
  dipilih lewat ikon titik: Sistem (layar berhuruf A, default mengikuti perangkat),
  Terang (matahari), atau Gelap (lingkaran). Ketuk tanggal untuk melihat detail
  libur dan memilih tanggal acara baru; tombol tambah di kanan bulan membuat acara.
  Agenda mingguan menampilkan seluruh acara dan libur/peringatan Senin–Minggu
  yang terlihat, termasuk yang sudah lewat. Libur/peringatan berada di atas acara;
  acara pribadi diurutkan menurut tanggal dan waktu. Nama yang terpotong bergerak
  otomatis, kecuali animasi dikurangi/dimatikan; ketuk acara untuk rincian lengkap. Agenda bulanan hanya menampilkan acara serta libur/peringatan hari ini,
  walaupun bulan lain dipilih; detail libur tanggal terpilih tetap terlihat. Kalender
  terbuka satu minggu, panah bawah membuka tampilan bulan; swipe pada kisi
  berpindah minggu atau bulan sesuai tampilan. Ketuk label bulan atau ikon kalender
  di dalamnya untuk kembali ke hari ini; tekan lama label untuk navigasi tanpa swipe
  (di preview juga bisa memakai Shift+F10 atau menu konteks).
  Tanggal berbentuk squircle: garis merah menandai libur nasional,
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
  Pengingat Android mendukung saat mulai, 10 menit, 1 jam, 1 hari sebelumnya,
  atau 1–999 menit, jam, atau hari sesuai pilihan;
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

Ketuk ikon cuaca di homescreen atau di grid Super App untuk membuka halaman
prakiraan: cuaca dan suhu saat ini, prakiraan 24 jam
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

## Jadwal Mengajar

Buka Jadwal Mengajar dari ikon di grid Super App. Tabel memiliki kolom Senin–Jumat;
dua belas baris jam tersedia sebagai awal, termasuk Istirahat 09.45–10.30 dan
ISHOMA 11.50–12.50. Ketuk jam untuk mengubahnya atau menambahkan jam lain (format 24 jam).
Ketuk jam untuk mengubah atau menghapus baris; menghapus baris juga menghapus
semua jadwal di dalamnya. Jam harus berakhir pada hari yang sama dan tidak boleh
bertumpang tindih; urutan otomatis menurut jam mulai. Ketuk sel untuk memasukkan
kelas/catatan dan pelajaran/kegiatan; gunakan ikon tempat sampah untuk menghapus isi.
Warna background dapat dipilih per jadwal. Warna otomatis sama untuk pasangan kelas dan
pelajaran yang sama; warna manual pertama untuk pasangan tersebut dalam urutan
tabel menjadi acuannya. Warna manual lain tetap dapat berbeda.
Tabel menyesuaikan layar agar 12 baris dan lima hari terlihat bersamaan; layar
kecil atau baris tambahan dapat memerlukan gulir vertikal. Nama baris seperti
Istirahat dapat diubah lewat jam; kosongkan namanya untuk mengisi tiap hari.
Baris yang sudah tersimpan dipertahankan: baris bawaan yang bertabrakan tidak
ditambahkan. Jadwal berulang setiap minggu,
disimpan lokal pada perangkat, tidak tersinkron ke kalender dan tidak mengirim
pengingat. Preview menggunakan penyimpanan browser yang terpisah.

### Perintah suara dan dikte (Android)

Ketuk ikon gelombang suara bergaya titik di kiri atas ikon pengisian daya pada beranda atau di header aplikasi bawaan untuk mengaktifkan atau menghentikan mode **Perintah**. Tekan lama ikon untuk memilih **Perintah**, **Mengetik**, atau **Hentikan**. Ikon beranda berwarna amber seperti cuaca dan jam; pada header aplikasi, warna aktif membedakan mode perintah dan mengetik. Notifikasi juga menunjukkan mode dan menyediakan tombol berhenti. Saat layanan pengenalan mengirim tingkat suara, kolom titik ikon bergerak mengikuti input; animasi dapat tidak muncul bila penyedia tidak mengirim tingkat suara atau animasi perangkat dinonaktifkan. Saat launcher/aplikasi bawaan memiliki fokus, tekan volume naik dua kali cepat untuk mengaktifkan/menonaktifkan mode perintah; volume turun dua kali untuk mode mengetik. Tekanan volume pertama tetap dapat mengubah volume. Saat pertama mengaktifkan, baca persetujuan dan izinkan mikrofon serta notifikasi; notifikasi launcher harus aktif agar tersedia tombol **Hentikan**.

Preview browser menampilkan ikon gelombang suara di kiri atas ikon pengisian daya di homescreen. Ketuk untuk demo visual singkat dan info: animasi preview adalah simulasi, tidak mengakses mikrofon atau menjalankan perintah suara yang tersedia di APK Android.

- Mode perintah: perintah umum diawali **“yo”**, misalnya “yo buka kalender”, “yo buka jadwal mengajar”, “yo buka timer”, “yo buka alarm”, “yo buka jam dunia”, “yo buka stopwatch”, “yo buka cuaca”, “yo buka super app”, “yo buka semua aplikasi”, “yo buka beranda”. Di Kalender: “yo tambah acara”, “yo hari ini”, “yo minggu berikutnya/sebelumnya” atau “yo bulan berikutnya/sebelumnya” mengikuti tampilan. Di Jam: “yo tambah alarm”, “yo mulai stopwatch”, “yo jeda stopwatch”, “yo catat lap”. “yo selesai” atau “yo berhenti mendengarkan” menghentikan mode perintah. Khusus **“kalender ada acara minggu ini?”**, **“set alarm”**, **“set timer”**, **“set jam dunia”**, dan **“set stopwatch”**, awalan “yo” boleh dipakai atau dilewati. “Set alarm” membuka editor alarm dan “set timer” membuka pengaturan timer; isi waktu/durasi dan tekan simpan/mulai sendiri. Perintah dengan waktu atau durasi yang disebutkan belum dipahami. Perintah yang tidak dikenal diabaikan; tidak ada perintah hapus/simpan otomatis.
- Jawaban suara Kalender membaca acara lokal minggu ini (Senin–Minggu, termasuk acara yang sudah lewat dan pengulangan); maksimal lima acara dibacakan, sisanya ditunjukkan jumlahnya. Jawaban hanya dibacakan saat launcher di depan dan perangkat tidak terkunci; saat di latar belakang ketuk notifikasi, lalu tanyakan lagi. Pasang suara **Bahasa Indonesia offline** pada mesin text-to-speech Android. Launcher memilih suara yang ditandai offline oleh mesin; gunakan mesin yang dipercaya. Pengenalan suara ditunda saat jawaban dibacakan dan dilanjutkan sesudahnya. Jika suara offline tidak tersedia, tampil pesan kesalahan, bukan jawaban palsu.
- Mode mengetik: awali setiap ucapan dengan **“ketik”**, misalnya “ketik rapat kelas”. Dikte dimasukkan pada posisi kursor/seleksi di kolom teks aktif, atau pada akhir kolom teks pertama yang terlihat jika belum ada yang aktif. Form Kalender dan Jadwal didukung. Kolom sandi dan kolom nonteks tidak didikte; aplikasi pihak ketiga tidak dapat menerima dikte ini.

Menu **Setelan** di Super App menyediakan pilihan launcher, lampu jalur, tautan izin aplikasi dan text-to-speech Android, serta tombol mulai/hentikan voice command dan voice typing. Pengenalan khusus suara pemilik belum tersedia; suara orang lain dapat memicu perintah. Setelan juga menyediakan unduhan opsional Qwen3 0.6B Q8 resmi (639 MB, diverifikasi SHA-256, ruang kosong sekitar 740 MB), pembatalan dan penghapusan. Download di penyimpanan privat aplikasi, dapat dihentikan OS dan harus diulang jika terputus; cadangan Android mengecualikan file model. Server unduhan melihat alamat IP perangkat. Runtime AI belum terintegrasi, jadi model yang diunduh belum dapat digunakan.

Fitur di atas menggunakan aturan perintah dan data Kalender lokal, **bukan AI generatif**. Runtime AI lokal belum terintegrasi; tidak ada pemahaman pertanyaan bebas atau pembacaan layar umum. Meskipun jawaban memilih suara TTS yang ditandai offline, pengenalan suara Android dapat menggunakan jaringan dan mengirim audio ke penyedianya.

Layanan mikrofon mencoba melanjutkan pengenalan di latar belakang dan menampilkan notifikasi dengan tombol berhenti. Ini **bukan jaminan mendengar terus-menerus**: penyedia suara Android tidak dirancang untuk pengenalan tanpa batas, dapat memerlukan internet, berbunyi, gagal, dibatasi sistem, atau dihentikan saat mikrofon tidak tersedia; kesalahan berulang menghentikan layanan. Suara dapat dikirim ke penyedia pengenalan perangkat. Tidak ada restart otomatis setelah layanan dihentikan sistem/reboot. Perintah membuka halaman saat di latar belakang memerlukan ketukan notifikasi, bukan membuka halaman diam-diam; dikte latar belakang dibuang. Tombol volume tidak dapat dijamin ditangkap di aplikasi lain atau layar terkunci. Perubahan data seperti penyimpanan dan penghapusan tetap memerlukan tindakan pengguna.

### Tampilan v0.1.211
Informasi baterai dan perangkat tampil langsung di homescreen; properti yang tidak dilaporkan Android ditulis `-`. CPU memakai nama SoC jika tersedia, atau ABI yang diberi label sebagai fallback. Angka di preview hanyalah simulasi. Di aplikasi bawaan, kontrol suara berada pada tombol mengambang kanan bawah; tombol homescreen tetap di tempatnya. Kalender mingguan dapat ditarik ke bawah untuk membuka bulan. Transisi kalender dan indikator fitur Jam mengikuti pengaturan animasi sistem; gaya kaca Android menggunakan transparansi dan gradien, bukan blur konten layar.

### Penyesuaian v0.1.212
Menit homescreen bergeser 1 px ke kanan, panel informasi turun 50 px, dan tombol suara mengambang di aplikasi bergeser 10 px ke kiri dan ke atas (berdasarkan skala preview). Ikon daftar modul Super App kembali tanpa latar kaca; kontrol di dalam aplikasi tetap bergaya kaca.

### Penyesuaian v0.1.213
Suhu homescreen diturunkan 8 px dan label informasi dinaikkan 25 px (berdasarkan skala preview). Titik pemisah dipusatkan di antara jam dan menit dan memudar hilang-muncul berurutan; animasi berhenti jika animasi sistem dinonaktifkan. Tombol kembali dan tema tidak lagi bergaya kaca.

### Penyesuaian v0.1.214
Titik pembatas dinaikkan 0,5 px (berdasarkan skala preview). Titik muncul satu per satu dan tetap terlihat; setelah titik kelima memudar, semuanya menghilang dan siklus berulang. Nama perangkat di preview adalah contoh GT30Pro; pada Android nama tetap berasal dari perangkat.

### Penyesuaian v0.1.215
Label informasi homescreen diturunkan 10 px berdasarkan skala preview; posisi dan ukuran elemen lainnya tetap.

### Penyesuaian v0.1.216
Angka jam dan menit digeser 0,5 px ke kanan tanpa memindahkan titik. Setelah titik terakhir memudar, titik pertama langsung memulai siklus berikutnya. Informasi perangkat disusun: perangkat, Android, RAM, penyimpanan di kiri; CPU dan CPU core di kanan.

### Penyesuaian v0.1.217
Label informasi homescreen diturunkan 20 px berdasarkan skala preview; posisi elemen lainnya tetap.

### Catatan v0.1.218
Super App memiliki aplikasi **Catatan**; lihat fitur dan batasan terkini pada bagian v0.1.221. Di Android catatan disimpan di basis data privat perangkat tanpa sinkronisasi dan dikecualikan dari backup Android; menghapus aplikasi menghapus catatan. Preview menyimpan data terpisah di penyimpanan browser, bukan di perangkat Android; menghapus data browser juga menghapus catatan preview.

### Penyesuaian v0.1.219
Hanya angka suhu di homescreen digeser 5 px ke kanan dan 5 px ke bawah berdasarkan skala preview; ikon cuaca tidak dipindahkan.

### Penyesuaian v0.1.220
Angka suhu di homescreen digeser 2 px ke kiri dan 2 px ke atas berdasarkan skala preview. Ikon cuaca homescreen diperkecil 15% dari pusatnya; ikon cuaca di Super App tidak berubah.

### Catatan v0.1.221 (tahap pertama)
Catatan tersimpan otomatis setelah mengetik dan saat halaman meninggalkan fokus. Tambah pencarian judul/isi/label, sematan, arsip, sampah dan pemulihan, label tunggal, warna, daftar/kotak dan checklist. Gunakan menu catatan untuk mengubah properti; pada Sampah tersedia hapus permanen. Catatan lama dimigrasikan tanpa dihapus. Di Android, bagikan teks atau ekspor TXT/PDF melalui pemilih dokumen (penyedia Google Drive dapat dipilih jika tersedia). Ini **bukan sinkronisasi Google Drive**. Preview menggunakan penyimpanan browser terpisah dan tidak menyediakan ekspor atau berbagi.

Editor visual kaya format, lampiran gambar/kamera/audio/sketsa, pengingat catatan, kunci perangkat/biometrik, serta sinkronisasi Google Drive dan backup/impor keseluruhan belum tersedia. Ekspor tidak otomatis mencadangkan semua catatan. Catatan tetap disimpan tanpa enkripsi tambahan di database privat aplikasi; jangan menganggapnya terkunci. Cadangan Android otomatis masih mengecualikan database catatan. Menghapus aplikasi menghapus data lokal.

### Catatan v0.1.222
Android menambahkan pengingat satu kali per catatan, dengan notifikasi privat (teks catatan tidak tampil dalam notifikasi). Notifikasi harus diizinkan; tanpa izin alarm tepat waktu pengingat bisa terlambat. Pengingat dipulihkan setelah perangkat dimulai ulang, tetapi tetap bergantung pada pengaturan baterai/perangkat. Mengirim catatan ke sampah membatalkan pengingatnya. Pengingat tidak termasuk dalam cadangan dan harus diatur lagi setelah impor.

Menu **Cadangkan / pulihkan** menyimpan semua catatan, termasuk arsip dan sampah, ke berkas JSON melalui pemilih dokumen; pilih Google Drive jika penyedia tersedia. Cadangan **tidak terenkripsi** dan tidak otomatis tersinkronisasi; pastikan unggahan Drive selesai. Pulihkan menambahkan salinan tanpa menimpa data yang sudah ada; mengimpor berulang membuat duplikat. Batas satu berkas 16 MB dan 5.000 catatan. Preview tidak mendukung cadangan atau pengingat dan menyimpan data browser secara terpisah. Tampilan staggered ditambahkan pada Android dan preview.

Belum tersedia: folder/kategori, editor visual kaya format, lampiran gambar/kamera/audio/sketsa, kunci dengan kredensial/biometrik, dan sinkronisasi otomatis Google Drive. Catatan lokal tidak mendapat enkripsi tambahan; penguncian tidak boleh diasumsikan aktif.

### Catatan v0.1.223
Saat membuat atau mengedit catatan, ikon tag membuka pilihan tag yang sudah dipakai atau membuat tag baru. Satu catatan masih memiliki satu tag; tag baru tersimpan bersama catatan ketika judul atau isi diisi. Ikon dengan nama aksesibilitas menggantikan tombol lokasi, tampilan, catatan baru, tag, checklist dan selesai. Preview memakai data browser terpisah. Glassmorphism dibatasi pada kontrol ikon Jam; Kalender, Jadwal Mengajar, Cuaca dan Catatan menggunakan kontrol tanpa efek kaca.

### Catatan v0.1.224
Satu catatan dapat menyimpan sampai 20 tag, masing-masing hingga 60 karakter. Catatan lama dan cadangan versi sebelumnya tetap dapat dibuka; tag lama menjadi satu tag. Pemilihan tag dan format tersimpan bersama isi. Di Android toolbar editor menyediakan tebal, miring dan garis bawah untuk teks yang dipilih, serta awalan daftar dan tabel teks sederhana (bukan tabel dengan sel atau lebar otomatis). Ekspor TXT/PDF dan berbagi tetap berupa teks biasa tanpa format. Cadangan JSON baru menyertakan tag dan format; versi lama aplikasi tidak dapat membuka format cadangan baru.

Preview mendukung beberapa tag tetapi belum memiliki editor format visual. Pengambilan foto kamera dan penyisipan gambar atau file belum tersedia. Catatan tetap lokal dan tidak terkunci atau tersinkronisasi otomatis.
