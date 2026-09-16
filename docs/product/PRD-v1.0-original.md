Status: Historical / Original Product Definition
Purpose: Records the original product vision and pre-development requirements.
Current authority: Not authoritative for current implementation.

# DueSoon

## Product Requirements Document (PRD) v1.0

**Platform:** Android Native
**Target:** Universal Users
**Design Direction:** Minimal & Clean
**Product Type:** Deadline & Reminder Application
**Status:** Product Definition / Pre-Development

---

# 1. Executive Summary

DueSoon adalah aplikasi Android native yang membantu pengguna mengingat tugas, pekerjaan, aktivitas pribadi, dan hal lain yang memiliki deadline.

Pengguna membuat sebuah task, menentukan deadline, kemudian DueSoon memberikan reminder sebelum deadline berdasarkan sistem Smart Reminder.

Fokus utama produk bukan menjadi task-management platform yang kompleks, tetapi menjadi:

> **Personal deadline reminder yang sederhana, cepat, dan dapat diandalkan.**

Core experience:

**Create Task → Set Deadline → Get Reminder → Complete Task**

---

# 2. Product Vision

### Vision

Menjadi aplikasi sederhana yang membantu pengguna tidak melewatkan hal-hal penting yang memiliki batas waktu.

### Long-Term Vision

DueSoon dapat berkembang menjadi:

> **A Personal Deadline Assistant**

yang bukan hanya menyimpan deadline, tetapi membantu pengguna memahami apa yang perlu diperhatikan dan kapan mereka perlu bertindak.

---

# 3. Problem Statement

Pengguna sering memiliki banyak tugas dan aktivitas dengan deadline berbeda.

Masalah yang terjadi:

* Pengguna lupa deadline.
* Pengguna baru menyadari deadline ketika sudah dekat.
* Reminder biasa terlalu sederhana atau terlalu banyak.
* Task manager sering terlalu kompleks untuk kebutuhan sederhana.
* Pengguna harus secara manual mengatur banyak reminder.
* Reminder yang sudah tidak relevan masih dapat muncul.

DueSoon menyelesaikan masalah tersebut dengan membuat deadline sebagai pusat pengalaman aplikasi.

---

# 4. Goals

## Primary Goals

1. Memudahkan pengguna membuat deadline.
2. Memberikan reminder tepat waktu.
3. Menampilkan task berdasarkan tingkat urgensi.
4. Mengurangi risiko pengguna melewatkan deadline.
5. Menyediakan pengalaman yang minimal dan tidak membingungkan.
6. Tetap berfungsi tanpa account dan tanpa koneksi internet untuk fungsi inti.

## Secondary Goals

1. Membuat pengguna dapat melihat seluruh deadline dalam satu tempat.
2. Memberikan kontrol terhadap reminder.
3. Menyediakan pengalaman yang nyaman untuk pengguna casual maupun pengguna dengan banyak task.

---

# 5. Non-Goals

Fitur berikut tidak menjadi bagian MVP:

* Social network
* Collaboration
* Chat
* Team management
* Habit tracker
* Pomodoro
* Gamification
* Project management kompleks
* Cloud synchronization
* Account system
* AI assistant
* Advanced analytics
* File management

Fitur tersebut dapat dipertimbangkan setelah MVP tervalidasi.

---

# 6. Target Users

DueSoon ditujukan untuk pengguna umum.

## Persona A — Student

Memiliki:

* tugas sekolah
* tugas kuliah
* ujian
* presentasi
* project

Kebutuhan:

> "Tolong ingatkan saya sebelum tugas dikumpulkan."

## Persona B — Professional

Memiliki:

* pekerjaan
* laporan
* meeting
* client deadline
* project

Kebutuhan:

> "Saya tidak boleh melewatkan deadline."

## Persona C — Personal User

Memiliki:

* pembayaran
* janji
* aktivitas pribadi
* administrasi
* berbagai deadline

Kebutuhan:

> "Saya ingin mengingat hal penting tanpa menggunakan sistem yang rumit."

---

# 7. Product Principles

## 7.1 Deadline First

Deadline merupakan informasi terpenting.

## 7.2 Minimal

Setiap elemen UI harus mempunyai tujuan.

## 7.3 Fast

Membuat task harus dapat dilakukan dengan cepat.

## 7.4 Calm

Reminder harus membantu, bukan membuat pengguna merasa terganggu.

## 7.5 Reliable

Reminder merupakan fitur inti sehingga harus dapat dipercaya.

## 7.6 Local First

Fungsi utama aplikasi tidak bergantung pada server atau internet.

---

# 8. Core User Journey

## Journey 1 — Create Task

```text
Open App
↓
Tap Add Task
↓
Enter Task Name
↓
Select Deadline
↓
Select Reminder
↓
Save
↓
Task Created
↓
Reminder Scheduled
```

## Journey 2 — Receive Reminder

```text
Reminder Time Arrives
↓
Android Notification
↓
User sees notification
↓
Open Task
↓
Complete / Snooze / Ignore
```

## Journey 3 — Complete Task

```text
Open Task
↓
Mark Complete
↓
Task Status = Completed
↓
Remaining reminders cancelled
```

## Journey 4 — Change Deadline

```text
Open Task
↓
Edit
↓
Change Deadline
↓
Save
↓
Existing reminders cancelled
↓
New reminders calculated
↓
New reminders scheduled
```

---

# 9. MVP Feature Scope

## Must Have

### Task Management

* Create task
* View task
* Edit task
* Delete task
* Mark task as completed
* Task title
* Optional description
* Deadline date
* Deadline time
* Category
* Priority

### Reminder

* Smart Reminder
* Custom Reminder
* Local notification
* Enable/disable reminders
* Notification cancellation
* Reminder rescheduling

### Task Status

* Upcoming
* Due Soon
* Overdue
* Completed

### Navigation

* Home
* Tasks
* Calendar
* Settings

---

## Should Have

* Search
* Category filtering
* Priority filtering
* Dark mode
* Notification preferences
* First-use onboarding
* Accessibility improvements

---

## Could Have

* Snooze
* Subtasks
* Recurring task
* Natural language task input
* Widgets

---

## Won't Have in MVP

* Account
* Cloud sync
* AI
* Collaboration
* Social
* Gamification
* Advanced statistics

---

# 10. Task Requirements

Setiap task minimal memiliki:

```text
Task
├── ID
├── Title
├── Description (optional)
├── Deadline
├── Category
├── Priority
├── Status
├── Created At
└── Completed At
```

## Title

Required.

Contoh:

> Math Homework

Tidak boleh kosong.

## Description

Optional.

Contoh:

> Kerjakan halaman 20–25.

## Deadline

Required untuk deadline task.

Format harus menyimpan tanggal dan waktu secara akurat.

## Category

Optional.

Contoh default:

* Personal
* School
* Work

Pengguna dapat membuat kategori tambahan pada tahap berikutnya jika diperlukan.

## Priority

MVP dapat menggunakan:

* Low
* Normal
* High

Priority tidak mengubah deadline tetapi memengaruhi visual urgency.

---

# 11. Task Without Deadline

DueSoon tetap mengizinkan task tanpa deadline.

Contoh:

> Bersihkan kamar

Task tersebut dapat disimpan, tetapi:

* tidak memiliki countdown
* tidak masuk Smart Deadline Reminder
* tidak dianggap overdue
* tidak mendapatkan reminder otomatis

Task tanpa deadline berfungsi sebagai task biasa.

Jika pengguna kemudian menambahkan deadline, task berubah menjadi deadline task dan sistem reminder dapat diaktifkan.

---

# 12. Deadline Behavior

Deadline terdiri dari:

**Date + Time**

Contoh:

> 18 September 2026, 20:00

Sistem harus membedakan:

### Upcoming

Deadline masih cukup jauh.

### Due Soon

Deadline sudah mendekat.

### Overdue

Waktu sekarang telah melewati deadline dan task belum selesai.

### Completed

Task telah selesai.

---

# 13. Smart Reminder

Smart Reminder merupakan default.

Tujuannya:

> Pengguna tidak perlu memikirkan sendiri kapan harus diingatkan.

Sistem menentukan waktu reminder berdasarkan jarak antara waktu sekarang dan deadline.

Contoh pendekatan awal:

### Deadline > 7 hari

Reminder:

* 3 hari sebelum
* 1 hari sebelum
* 3 jam sebelum

### Deadline 1–7 hari

Reminder:

* 1 hari sebelum
* 3 jam sebelum

### Deadline < 24 jam

Reminder:

* 3 jam sebelum
* 30 menit sebelum

### Deadline sangat dekat

Sistem hanya menggunakan reminder yang masih masuk akal.

Reminder yang waktunya sudah lewat tidak boleh dijadwalkan.

> Catatan: angka di atas merupakan Product Decision awal dan harus divalidasi melalui testing pengguna.

---

# 14. Custom Reminder

Pengguna dapat memilih reminder sendiri.

Contoh:

```text
Reminder me:

○ 1 week before
○ 3 days before
○ 1 day before
○ 3 hours before
○ 1 hour before
○ 30 minutes before
○ Custom
```

Pengguna dapat memilih lebih dari satu reminder.

---

# 15. Notification Requirements

Notification harus:

* menggunakan Android notification system
* tetap bekerja ketika aplikasi ditutup
* menggunakan task title
* menampilkan informasi deadline
* dapat membuka task terkait
* dapat dibatalkan jika task selesai
* dapat dijadwalkan ulang jika deadline berubah

Contoh:

> **Math Homework**
> Due tomorrow at 08:00.

Notification tidak boleh dikirim jika:

* task sudah selesai
* task dihapus
* reminder dinonaktifkan
* waktu reminder sudah tidak relevan

---

# 16. Notification Actions

Untuk MVP, notification dapat membuka task.

Future:

* Complete
* Snooze
* Reschedule

Future actions tidak menjadi requirement MVP kecuali implementasinya terbukti sederhana dan stabil.

---

# 17. Home Screen

Home adalah halaman utama.

Tujuan:

> Menjawab pertanyaan "Apa yang harus saya perhatikan sekarang?"

Struktur konseptual:

```text
Greeting

NEEDS ATTENTION

Task paling urgent

UPCOMING

Task berikutnya

RECENTLY COMPLETED
(optional)
```

Task harus diurutkan berdasarkan urgency.

Prioritas utama:

1. Overdue
2. Deadline paling dekat
3. Priority
4. Creation time

---

# 18. Tasks Screen

Menampilkan seluruh task.

Filter awal:

* All
* Upcoming
* Due Soon
* Overdue
* Completed

Task dapat dikelompokkan berdasarkan waktu:

```text
Today

Tomorrow

This Week

Later

Completed
```

---

# 19. Calendar Screen

Calendar digunakan untuk melihat deadline berdasarkan tanggal.

Fungsi MVP:

* melihat bulan
* memilih tanggal
* melihat task pada tanggal tersebut
* membuka detail task

Calendar bukan pengganti aplikasi calendar penuh.

---

# 20. Task Detail

Task Detail menampilkan:

```text
Task Title

Description

Deadline
18 Sep · 20:00

Priority
High

Category
School

Reminder
Smart

────────────

Edit
Complete
Delete
```

Deadline harus menjadi informasi visual paling dominan.

---

# 21. Settings

MVP settings:

### Notifications

* Enable notifications
* Smart reminder default
* Notification sound
* Vibration

### Appearance

* System
* Light
* Dark

### Defaults

* Default reminder behavior
* Default category

### About

* App version
* Privacy
* About DueSoon

---

# 22. Empty States

Setiap halaman harus mempunyai empty state.

Contoh Home:

> **You're all caught up.**
> No upcoming deadlines.

Tasks:

> **No tasks yet.**
> Create your first task to get started.

Calendar:

> **Nothing scheduled.**
> No deadlines on this day.

Empty state harus tetap memberikan action utama.

Contoh:

**+ Create Task**

---

# 23. Error States

Contoh:

### Empty Title

> Task name is required.

### Invalid Deadline

Jika deadline sudah lewat saat membuat task:

> This deadline has already passed.

Pengguna tetap dapat membuat task overdue jika produk memutuskan hal tersebut diperbolehkan, tetapi behavior harus konsisten.

### Notification Permission Denied

Aplikasi menjelaskan:

> Notifications are disabled. DueSoon won't be able to remind you about upcoming deadlines.

Berikan shortcut menuju pengaturan Android jika memungkinkan.

---

# 24. Important Edge Cases

MVP harus mempertimbangkan:

1. Deadline sudah lewat.
2. Task dibuat dengan deadline sangat dekat.
3. Task selesai sebelum semua reminder dikirim.
4. Deadline diubah.
5. Reminder diubah.
6. Task dihapus.
7. Notification permission ditolak.
8. Perangkat restart.
9. Timezone berubah.
10. Waktu perangkat berubah.
11. Daylight saving/timezone transition.
12. Aplikasi tidak dibuka dalam waktu lama.
13. Banyak task memiliki deadline sama.
14. Banyak notification muncul berdekatan.
15. User membuat task tanpa deadline.

---

# 25. Privacy

MVP bersifat local-first.

Data task disimpan secara lokal pada perangkat.

Tidak diperlukan:

* account
* email
* server
* cloud database

Aplikasi hanya meminta permission yang benar-benar diperlukan.

Notification permission harus diminta dengan konteks yang jelas.

---

# 26. Accessibility

Aplikasi harus:

* mendukung system font scaling
* memiliki touch target yang cukup besar
* tidak bergantung hanya pada warna
* memiliki content description untuk icon
* memiliki contrast yang memadai
* dapat digunakan dengan screen reader
* mendukung dark mode

---

# 27. Performance Requirements

Aplikasi harus terasa instant untuk operasi lokal:

* membuka Home
* membuka task
* membuat task
* mengedit task
* menyelesaikan task

Operasi database lokal tidak boleh menyebabkan UI freeze.

---

# 28. Offline Requirements

Core functionality harus tetap berjalan tanpa internet.

User dapat:

* membuat task
* mengedit task
* menghapus task
* melihat task
* menyelesaikan task
* membuat reminder

Internet tidak diperlukan untuk core functionality.

---

# 29. MVP Success Metrics

Beberapa metrik yang dapat digunakan:

### Activation

Persentase pengguna yang membuat task pertama.

### Reminder Reliability

Persentase reminder yang berhasil dijadwalkan dan dipicu sesuai aturan.

### Completion

Persentase task yang ditandai selesai.

### Retention

Berapa banyak pengguna yang kembali menggunakan aplikasi.

### Missed Deadline

Jumlah task yang menjadi overdue.

Target jangka panjang:

> Mengurangi jumlah deadline yang terlewat oleh pengguna.

---

# 30. MVP Acceptance Criteria

MVP dianggap siap apabila:

### Task

* User dapat membuat task.
* User dapat mengedit task.
* User dapat menghapus task.
* User dapat menyelesaikan task.
* Task tersimpan ketika aplikasi ditutup.

### Deadline

* User dapat menentukan tanggal.
* User dapat menentukan waktu.
* Deadline ditampilkan sesuai timezone perangkat.
* Task berubah menjadi overdue setelah deadline.

### Reminder

* Smart reminder dapat dihitung.
* Reminder dapat dijadwalkan.
* Reminder dapat dibatalkan.
* Reminder diperbarui ketika deadline berubah.
* Reminder berhenti ketika task selesai.

### Notification

* Notification muncul sesuai jadwal.
* Notification dapat membuka task.
* Notification permission ditangani dengan benar.

### Reliability

* Task tetap tersedia setelah aplikasi ditutup.
* Sistem menangani restart perangkat.
* Sistem tidak mengirim reminder untuk task yang sudah selesai.

---

# 31. Future Roadmap

## Version 1.1

* Snooze
* Recurring tasks
* Subtasks
* Better notification actions

## Version 1.2

* Android home-screen widget
* Natural language task creation
* Improved smart reminder

## Version 2.0

* AI deadline assistant
* Calendar integration
* Cloud backup
* Multi-device synchronization

## Version 3.0

Potentially:

* Account
* Cross-platform
* Collaboration
* Advanced intelligence

Roadmap tersebut tidak mengikat MVP.

---

# 32. Product Decisions Needed

Beberapa keputusan masih harus dikunci sebelum UX/UI.

### Decision 1 — Task tanpa deadline

**Keputusan:** Ya.

Task tanpa deadline diperbolehkan.

### Decision 2 — Deadline

**Keputusan:** Satu deadline utama per task untuk MVP.

### Decision 3 — Reminder

**Keputusan:** Hybrid.

Smart Reminder menjadi default, tetapi Custom Reminder tersedia.

### Decision 4 — Account

**Keputusan:** Tidak diperlukan untuk MVP.

### Decision 5 — Cloud

**Keputusan:** Tidak diperlukan untuk MVP.

### Decision 6 — AI

**Keputusan:** Tidak diperlukan untuk MVP.

### Decision 7 — Core positioning

**Keputusan:**

> Deadline-first reminder application.

---

# 33. Product North Star

Jika suatu saat kita bingung apakah sebuah fitur harus dimasukkan, gunakan pertanyaan:

> **"Apakah fitur ini membantu pengguna mengetahui dan menangani deadline dengan lebih baik?"**

Jika jawabannya tidak, fitur tersebut kemungkinan bukan prioritas.

---

# 34. One-Sentence Product Definition

> **DueSoon is a minimal, local-first Android deadline reminder app that helps anyone know what needs their attention before it's too late.**

---

# 35. MVP Core Loop

```text
          CREATE
             │
             ▼
        SET DEADLINE
             │
             ▼
      SMART REMINDER
             │
             ▼
        NOTIFICATION
             │
             ▼
          ACTION
        /         \
   COMPLETE      IGNORE
      │             │
      ▼             ▼
   FINISHED       DUE SOON
                    │
                    ▼
                 OVERDUE
```

**End of PRD v1.0**

