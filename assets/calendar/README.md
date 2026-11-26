# Data kalender Indonesia

`indonesia.json` adalah feed terkurasi, bukan sumber pemerintah yang memperbarui
sendiri. Aplikasi mengecek salinan file ini di branch `arena/f57090c0-gt30prolauncher`
di GitHub ketika kalender dibuka (maksimal sekali per 12 jam), dan dapat dicek
manual. Tidak ada pemeriksaan ketika aplikasi ditutup. Pembaruan baru akan muncul
hanya setelah pengelola memverifikasi dan memperbarui file di branch tersebut;
jika sumber tidak tersedia, aplikasi mempertahankan data terakhir atau bawaan APK.

Data hari libur nasional dan cuti bersama 2026–2027 mengacu pada SKB tiga menteri:
- [2026 — Sekretariat Negara](https://setneg.go.id/baca/index/inilah_skb_3_menteri_libur_nasional_dan_cuti_bersama_2026)
- [2027 — Sekretariat Negara](https://setneg.go.id/baca/index/inilah_skb_3_menteri_libur_nasional_dan_cuti_bersama_2027)

Tanggal Idulfitri dan Iduladha 2027 masih mengikuti penetapan Menteri Agama.
Jadwal dapat direvisi dan cuti bersama dapat bergantung pada aturan instansi.
Peringatan nasional adalah pilihan, bukan daftar lengkap dan bukan otomatis libur.
Tahun di luar cakupan dinyatakan belum tersedia. Data acara pribadi disimpan
terpisah secara lokal dan tidak dimasukkan dalam permintaan pembaruan.

Untuk mengubah feed: verifikasi semua sumber, ubah file ini dan naikkan bilangan
bulat `revision` (wajib lebih besar dari sebelumnya). Jangan hapus cakupan tahun
lama; app akan menolak feed yang menghilangkan tahun yang sudah dimiliki. Jalankan
`python3 tools/generate_calendar_data.py` dan tes kalender sebelum commit/push.
