# Data kalender Indonesia

`indonesia.json` adalah snapshot offline, bukan layanan yang diperbarui otomatis.
Data hari libur nasional dan cuti bersama 2026–2027 bersumber dari SKB tiga menteri:

- [2026 — Sekretariat Negara](https://setneg.go.id/baca/index/inilah_skb_3_menteri_libur_nasional_dan_cuti_bersama_2026)
- [2027 — Sekretariat Negara](https://setneg.go.id/baca/index/inilah_skb_3_menteri_libur_nasional_dan_cuti_bersama_2027)

Tanggal Idulfitri dan Iduladha 2027 masih mengikuti penetapan Menteri Agama.
Jadwal dapat direvisi dan cuti bersama dapat bergantung pada aturan instansi.
Ada pula pilihan peringatan nasional (misalnya Kartini dan Hari Pahlawan), yang
**bukan** otomatis hari libur nasional. Peringatan ini bukan daftar lengkap.
Untuk tahun di luar cakupan, aplikasi menampilkan informasi bahwa data belum tersedia.

Setelah pembaruan data yang telah diverifikasi, jalankan
`python3 tools/generate_calendar_data.py` untuk menghasilkan salinan Android dan preview,
lalu jalankan `python3 tests/test-calendar-data.py` dan tes kalender preview.
