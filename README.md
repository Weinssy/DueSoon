# DueSoon

> Tahu apa yang perlu menjadi perhatian Anda berikutnya.

DueSoon adalah aplikasi pengingat deadline Android yang minimal, local-first, dan fokus pada satu tujuan utama: membantu Anda mengetahui tugas mana yang paling penting untuk ditangani selanjutnya.

Aplikasi ini dirancang agar tetap tenang, jelas, cepat, andal, dan bebas dari kompleksitas yang tidak perlu. Semua data disimpan secara lokal di perangkat Anda, dengan fokus pada prioritas deadline dan manajemen tugas yang sederhana.

## Prinsip produk

"Deadline harus jelas. Yang lain harus tetap tenang."

DueSoon tidak membebani pengguna dengan fitur yang tidak relevan. Tidak ada akun, tidak ada sinkronisasi cloud, dan tidak ada kebutuhan untuk mengelola ekosistem yang rumit. Fokus utama aplikasi ini adalah membantu Anda tetap aware terhadap tenggat waktu yang paling dekat.

## Fitur utama

- Membuat, mengedit, menandai selesai, dan menghapus tugas
- Deadline opsional
- Status tugas seperti: Mendekati deadline, Perlu perhatian hari ini, Terlambat
- Smart Reminder
- Reminder kustom
- Notifikasi lokal
- Tampilan beranda, daftar tugas, dan kalender
- Pencarian dan penyaringan tugas
- Pengurutan tugas yang sederhana dan konsisten
- Tugas berulang
- Fitur snooze
- Backup dan ekspor data lokal
- Impor dan restore data secara aman
- Fokus pada data lokal dan keamanan saat pemulihan data

## Stack teknologi

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Coroutines
- Flow / StateFlow
- Navigation Compose
- DataStore
- Android Notification APIs
- WorkManager
- Gradle Kotlin DSL

## Arsitektur

UI → ViewModel → Repository → Room

Pendekatan arsitektur ini menjaga pemisahan tanggung jawab agar logika bisnis tetap terjaga dari UI dan akses data tetap konsisten melalui repository.

## Status proyek

Saat ini, fokus pengembangan DueSoon berada pada versi v1.3.0 dengan tema:

- Backup & Data Safety
- Ekspor data portable dalam format JSON
- Import data dari file backup
- Restore data dengan validasi yang aman
- Transaction Room yang aman
- Reconciliation notifikasi setelah operasi database berhasil

Ini menandakan bahwa proyek saat ini lebih menekankan pada keandalan data, keamanan restore, dan integritas data lokal dibandingkan fitur-fitur yang bersifat cloud atau multi-device.

## Prinsip lokal-first dan keamanan data

DueSoon tetap berorientasi pada perangkat lokal. Data pengguna tidak dipaksa untuk disinkronkan ke layanan eksternal, dan aplikasi dirancang agar tetap dapat berfungsi tanpa akun atau koneksi internet.

Pada tahap saat ini, fokus utama adalah:

- menjaga data tetap aman dan dapat dipulihkan
- memvalidasi seluruh backup sebelum restore
- memastikan operasi impor/restore tidak mengubah data secara parsial
- menjaga notifikasi tetap konsisten dengan tindakan pengguna

## Cara menjalankan

1. Clone repositori ini.
2. Buka project di Android Studio.
3. Hubungkan perangkat Android atau emulator.
4. Jalankan aplikasi dari modul `app`.

Pastikan Anda sudah menyiapkan lingkungan Android Studio dan SDK yang sesuai.

## Struktur project

```text
DueSoon/
├─ app/
│  ├─ src/
│  └─ build.gradle.kts
├─ docs/
├─ gradle/
├─ build.gradle.kts
├─ gradlew
├─ gradlew.bat
├─ settings.gradle.kts
├─ LICENSE
├─ README.md
└─ prd.md
```

## Roadmap saat ini

DueSoon saat ini berfokus pada stabilitas data dan pengalaman pengelolaan deadline yang minimal, dengan prioritas pengembangan pada:

- backup dan ekspor data
- validasi import/restore
- perlindungan data dari operasi yang tidak aman
- notifikasi lokal yang konsisten
- pengalaman pengelolaan tugas yang tetap simpel dan cepat

## Lisensi

Proyek ini dilisensikan di bawah [MIT License](LICENSE).

## Deskripsi GitHub yang direkomendasikan

Aplikasi pengingat deadline Android yang minimal, local-first, dan fokus pada tugas yang paling perlu ditangani berikutnya.

