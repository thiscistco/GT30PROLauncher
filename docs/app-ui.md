# Desain aplikasi bawaan launcher

Kalender adalah acuan untuk seluruh halaman milik launcher. `AppUi` adalah
sumber palet dan komponen Android: Doto tebal, latar dan kartu, teks kontras,
kontrol squircle, header panah kembali + judul + pilihan tema, serta warna
status/navigation bar. Panah kembali memakai `AppUi.backButton` yang sama
dengan Kalender (ikon 24 dp, target sentuh 48 dp, padding halaman 20 dp kiri
dan 16 dp atas ditambah inset sistem). Gunakan `new AppUi(activity)`, `ui.window(activity)`,
`ui.header(activity, font, title)`, `ui.surface(activity, ui.card/control)` dan
`ui.dialog(activity)` saat membuat halaman baru. Berikan target sentuh minimal
48 dp dan label aksesibilitas pada setiap ikon; pertahankan `SwipeBackActivity`
dan gesture yang sesuai.

Semua halaman berbagi pilihan `calendar_appearance/theme`: `system` (default),
`light`, atau `dark`. Nama preferensi yang lama dipertahankan agar pilihan
Kalender yang tersimpan tidak hilang. Halaman baru harus memperbarui tampilannya
saat kembali jika pilihan berubah; perubahan tema tidak boleh menghapus data
atau input yang belum disimpan. Tema ini tidak mengendalikan kaca homescreen,
yang mengikuti mode sistem sendiri, dan tidak mengubah aplikasi pihak ketiga.

Di preview, token `--app-*` di `preview/index.html` mencerminkan palet Android;
kontrol bersama menggunakan `.app-top`, `.app-back`, `.app-theme-*`. Untuk
halaman baru, tambahkan ID halaman dan kontrol tema dengan pola `clockPage`,
`clockTheme`, `clockThemeIcon`, `clockThemeMenu`, `clockThemeSystem`,
`clockThemeLight`, `clockThemeDark`; daftarkan prefix dalam `preview/calendar.js`
dan selector palet di CSS. Gunakan warna token untuk isi dan pastikan kontras
terhadap latar di kedua mode. Biarkan data dan logika halaman terpisah dari
tampilan. Perbarui tes tema, status input, dan aksesibilitas jika menambah halaman.
