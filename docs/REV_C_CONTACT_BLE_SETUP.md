# IgniTra Rev C — Setup kontak, starter, BLE, dan timing

## Urutan setup

1. Pasang Core dan modul AUX pada header 2.54 mm sesuai tanda pin 1.
2. Hubungkan JKEYLESS pin 1 ke aki konstan melalui sekring kontrol 1 A. K1 menginjeksikan daya ke VIN_PROT; jangan hubungkan keluarannya kembali ke J1.5.
3. J1.5 tetap berasal dari kontak mekanis. Firmware membaca posisinya melalui QIGN_SENSE dan U7/P3.
4. Pilih profil di Setup:
   - NS200: starter tetap melalui interlock OEM.
   - UNIVERSAL MANUAL: J1.9 wajib menjadi NEUTRAL_IN aktif sebelum starter.
   - UNIVERSAL MATIC: pemeriksaan netral tidak diwajibkan; interlock rem/standar samping OEM tetap dianjurkan.
5. Pastikan aplikasi menampilkan status AUX siap, lalu uji berurutan: kontak mekanis ON/OFF, KONTAK ON aplikasi, START ENGINE, dan STOP ENGINE.

## Tombol tunggal

- Kontak OFF dan mesin mati: **KONTAK ON**.
- Kontak sudah aktif tetapi RPM belum hidup: **START ENGINE**.
- RPM mesin minimal 500: **STOP ENGINE**.

STOP ENGINE mengirim AUX,ALL,OFF. Firmware melepas starter, mematikan izin pengapian/charger HV, lalu melepas K1. Kontak mekanis ON tetap terlihat sebagai sumber MECHANICAL; tombol menjadi START ENGINE agar mesin dapat dinyalakan kembali tanpa menambah tombol.

## Preset timing idle

STANDARD, SOFT, RESPONSIVE, KUDA, DRUMBAND, FOMO, dan CUSTOM dapat dipilih pada Maps. Intensitas dibatasi 0–10 dan rentang aktif 500–5000 RPM. Firmware hanya menerapkan pola idle pada TPS rendah; map advance utama tetap menjadi batas dasar.

## Pemulihan BLE

Aplikasi tidak lagi membatalkan GATT hanya karena masuk background. Saat kembali aktif, koneksi sehat menerima PING. Attempt yang masih menggantung diputus dan dibuka ulang oleh watchdog setelah 8 detik, sehingga status tidak menetap pada “Menghubungkan...”.

Jika belum pulih:

1. pastikan Bluetooth dan izin Nearby Devices aktif;
2. tunggu watchdog 8 detik;
3. ketuk Putus lalu Hubungkan kembali;
4. jangan hapus binding perangkat kecuali serial firmware memang berubah.

## Notifikasi Android

Status bar hanya dipakai untuk perubahan mesin hidup/mati dan fault keselamatan. Perubahan modul, sinkronisasi, dan informasi setup biasa tetap tampil di dalam aplikasi.
