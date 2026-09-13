# STEP 4 — WIREFRAME & UI/UX SPECIFICATION

## 1. Tujuan Step 4

Mendefinisikan struktur dan perilaku setiap layar utama DueSoon sebelum masuk ke tahap visual design dan coding.

Prinsip utama:

> **Less UI, more clarity.**

Setiap layar harus membantu pengguna memahami:

1. Apa yang sedang terjadi?
2. Apa yang harus saya lakukan?
3. Apa tindakan berikutnya?

---

# 2. Struktur Navigasi Utama

Aplikasi menggunakan Bottom Navigation dengan 4 menu utama:

```text
┌─────────────────────────────────┐
│                                 │
│         CONTENT AREA            │
│                                 │
│                                 │
├─────────────────────────────────┤
│  Home   Tasks   Calendar   ⚙    │
└─────────────────────────────────┘
```

Menu:

* Home
* Tasks
* Calendar
* Settings

Tombol `+` digunakan untuk membuat task baru.

Screen yang tidak berada di Bottom Navigation:

* Create Task
* Task Detail
* Edit Task
* Notification Permission
* Confirmation Dialog

---

# 3. Screen 01 — HOME

## Tujuan

Menjawab pertanyaan:

> **"Apa yang perlu saya perhatikan sekarang?"**

Home adalah layar paling penting dalam aplikasi.

## Struktur

```text
┌─────────────────────────────────┐
│ Good morning                    │
│ Here's what needs attention.    │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 3 tasks need attention      │ │
│ └─────────────────────────────┘ │
│                                 │
│ NEEDS ATTENTION                 │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 🔴 Math Assignment          │ │
│ │    Due today · 14:00        │ │
│ │    High                     │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 🟠 Project Presentation     │ │
│ │    Tomorrow · 09:00        │ │
│ └─────────────────────────────┘ │
│                                 │
│ UPCOMING                        │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ English Essay               │ │
│ │ Friday · 20:00              │ │
│ └─────────────────────────────┘ │
│                                 │
│                         ┌─────┐ │
│                         │  +  │ │
│                         └─────┘ │
├─────────────────────────────────┤
│ Home   Tasks   Calendar   ⚙     │
└─────────────────────────────────┘
```

## Sections

### Header

Menampilkan:

* Greeting
* Ringkasan singkat

Contoh:

> Good morning
> Here's what needs attention.

Greeting dapat menyesuaikan waktu.

### Needs Attention

Menampilkan task yang paling membutuhkan perhatian.

Urutan:

1. Overdue
2. Deadline hari ini
3. Deadline terdekat
4. Priority

### Upcoming

Menampilkan task yang belum mendesak tetapi akan datang.

### Floating Action Button

`+`

Aksi:

```text
Tap +
↓
Create Task
```

## Empty State

Jika belum ada task:

```text
Nothing here yet.

Create your first task
and never miss a deadline.

        [+ Add Task]
```

---

# 4. Screen 02 — CREATE TASK

## Tujuan

Membuat task baru secepat mungkin.

```text
┌─────────────────────────────────┐
│ ←  New Task                     │
│                                 │
│ TITLE                           │
│ ┌─────────────────────────────┐ │
│ │ What needs to be done?      │ │
│ └─────────────────────────────┘ │
│                                 │
│ DESCRIPTION                     │
│ ┌─────────────────────────────┐ │
│ │ Add details (optional)      │ │
│ │                             │ │
│ └─────────────────────────────┘ │
│                                 │
│ DEADLINE                        │
│ ┌─────────────────────────────┐ │
│ │ 📅  Select date             │ │
│ │ 🕐  Select time             │ │
│ └─────────────────────────────┘ │
│                                 │
│ CATEGORY                        │
│ [ School ▼ ]                    │
│                                 │
│ PRIORITY                        │
│ ○ Low   ● Normal   ○ High       │
│                                 │
│ REMINDER                        │
│ ┌─────────────────────────────┐ │
│ │ Smart Reminder          >   │ │
│ └─────────────────────────────┘ │
│                                 │
│                                 │
│ ┌─────────────────────────────┐ │
│ │          SAVE TASK           │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

## Required Fields

Title:

* Required

Deadline:

* Optional

Description:

* Optional

Category:

* Optional / default category

Priority:

* Default: Normal

Reminder:

* Default: Smart Reminder

## Behavior

Jika deadline tidak diisi:

Task tetap dapat disimpan.

Namun:

* Tidak ada countdown
* Tidak ada status Due Soon
* Tidak ada status Overdue
* Smart Reminder tidak dijadwalkan

Jika deadline diisi:

```text
Save
↓
Create Task
↓
Schedule Reminder
↓
Return to Home
```

---

# 5. Screen 03 — TASK DETAIL

## Tujuan

Memberikan seluruh informasi task dan tindakan utama.

```text
┌─────────────────────────────────┐
│ ←                         ⋮     │
│                                 │
│ Mathematics Assignment          │
│                                 │
│ 🔴 DUE TODAY                    │
│ Today · 14:00                   │
│                                 │
│ ─────────────────────────────── │
│                                 │
│ Complete the exercises from     │
│ chapter 5.                      │
│                                 │
│ CATEGORY                        │
│ School                          │
│                                 │
│ PRIORITY                        │
│ High                            │
│                                 │
│ REMINDER                        │
│ Smart Reminder                  │
│                                 │
│ ─────────────────────────────── │
│                                 │
│ ┌─────────────────────────────┐ │
│ │       ✓ COMPLETE TASK       │ │
│ └─────────────────────────────┘ │
│                                 │
│ Edit Task                       │
└─────────────────────────────────┘
```

## Actions

Primary:

* Complete Task

Secondary:

* Edit
* Delete

Menu `⋮`:

```text
Edit Task
Delete Task
```

---

# 6. Screen 04 — EDIT TASK

Struktur hampir sama dengan Create Task.

Perbedaannya:

```text
New Task
```

berubah menjadi:

```text
Edit Task
```

Tombol:

```text
SAVE CHANGES
```

## Behavior penting

Jika deadline berubah:

```text
Old Reminder
      ↓
Cancel
      ↓
New Deadline
      ↓
Calculate Reminder
      ↓
Schedule New Reminder
```

Jika reminder dimatikan:

```text
Cancel scheduled notifications
```

Jika task selesai:

```text
Cancel remaining reminders
```

---

# 7. Screen 05 — TASKS

## Tujuan

Memberikan akses ke seluruh task.

```text
┌─────────────────────────────────┐
│ Tasks                           │
│                                 │
│ 🔍 Search tasks                 │
│                                 │
│ [All] [Upcoming] [Due Soon]     │
│ [Overdue] [Completed]           │
│                                 │
│ TODAY                           │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Math Assignment             │ │
│ │ Today · 14:00       🔴 High │ │
│ └─────────────────────────────┘ │
│                                 │
│ TOMORROW                        │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Presentation                │ │
│ │ Tomorrow · 09:00            │ │
│ └─────────────────────────────┘ │
│                                 │
├─────────────────────────────────┤
│ Home   Tasks   Calendar   ⚙     │
└─────────────────────────────────┘
```

## Filter

Tab:

* All
* Upcoming
* Due Soon
* Overdue
* Completed

Task dapat dicari berdasarkan:

* Title
* Description
* Category

---

# 8. Screen 06 — CALENDAR

## Tujuan

Memberikan perspektif berdasarkan tanggal.

```text
┌─────────────────────────────────┐
│ Calendar                        │
│                                 │
│        September 2026           │
│                                 │
│ Mo Tu We Th Fr Sa Su            │
│  1  2  3  4  5  6  7           │
│  8  9 10 11 12 13 14           │
│ 15 16 17 18 19 20 21           │
│ ...                             │
│                                 │
│ SEPTEMBER 13                    │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ Math Assignment             │ │
│ │ 14:00 · High               │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ English Essay               │ │
│ │ 20:00                       │ │
│ └─────────────────────────────┘ │
│                                 │
├─────────────────────────────────┤
│ Home   Tasks   Calendar   ⚙     │
└─────────────────────────────────┘
```

## Behavior

Tap tanggal:

```text
Date
↓
Show tasks on that date
```

Tap task:

```text
Task Detail
```

Tanggal dengan task dapat diberi indikator visual sederhana.

---

# 9. Screen 07 — SETTINGS

```text
┌─────────────────────────────────┐
│ Settings                        │
│                                 │
│ NOTIFICATIONS                   │
│                                 │
│ Notifications              >    │
│ Default Reminder            >   │
│                                 │
│ APPEARANCE                      │
│                                 │
│ Theme                       >   │
│                                 │
│ GENERAL                         │
│                                 │
│ Default Category            >   │
│ Default Priority             >  │
│                                 │
│ ABOUT                           │
│                                 │
│ About DueSoon               >   │
│ Privacy Policy              >   │
│                                 │
│ Version 1.0.0                   │
│                                 │
├─────────────────────────────────┤
│ Home   Tasks   Calendar   ⚙     │
└─────────────────────────────────┘
```

---

# 10. Screen 08 — NOTIFICATION PERMISSION

Pada Android versi modern, aplikasi perlu menangani izin notifikasi.

Saat pengguna pertama kali membutuhkan reminder:

```text
┌─────────────────────────────────┐
│                                 │
│            🔔                   │
│                                 │
│ Never miss a deadline.           │
│                                 │
│ DueSoon needs notification       │
│ permission to remind you         │
│ about upcoming deadlines.        │
│                                 │
│ ┌─────────────────────────────┐ │
│ │      ENABLE NOTIFICATIONS   │ │
│ └─────────────────────────────┘ │
│                                 │
│ Not now                         │
└─────────────────────────────────┘
```

Jika permission ditolak:

* Task tetap dapat dibuat.
* Reminder tidak dikirim.
* Aplikasi memberikan informasi bahwa notification permission diperlukan untuk reminder.

---

# 11. Screen 09 — EMPTY STATES

Empty state harus informatif dan tidak membuat aplikasi terasa rusak.

## No Tasks

```text
No tasks yet.

Add something you need
to remember.
```

CTA:

```text
+ Add Task
```

## No Upcoming Tasks

```text
You're all caught up.

No upcoming deadlines.
```

## No Overdue Tasks

```text
You're on track.

No overdue tasks.
```

## No Completed Tasks

```text
No completed tasks yet.
```

---

# 12. Screen 10 — OVERDUE STATE

Task yang melewati deadline berubah menjadi:

```text
OVERDUE
```

Contoh:

```text
┌─────────────────────────────┐
│ 🔴 Math Assignment          │
│    Overdue · Sep 12, 14:00  │
│    High                     │
└─────────────────────────────┘
```

Task tetap berada di:

```text
Overdue
```

sampai pengguna:

* Complete
* Delete
* Edit deadline

---

# 13. Smart Reminder UX

Pada Create/Edit Task:

```text
REMINDER

● Smart Reminder
○ Custom Reminder
○ No Reminder
```

Jika memilih Smart Reminder:

```text
Smart Reminder

We'll remind you before
your deadline based on
how much time is left.

✓ Automatic
✓ Multiple reminders
✓ No setup required
```

Jika Custom:

```text
Custom Reminder

[ Add Reminder ]

Examples:
• 1 day before
• 3 hours before
• 30 minutes before
```

---

# 14. Confirmation Dialog

## Delete Task

```text
Delete task?

This task and its reminders
will be permanently removed.

Cancel       Delete
```

## Complete Task

Tidak perlu confirmation dialog untuk menjaga UX tetap cepat.

```text
Tap Complete
↓
Immediately Completed
```

Undo dapat ditampilkan sementara sebagai Snackbar:

```text
Task completed       UNDO
```

---

# 15. Snackbar

Digunakan untuk feedback singkat.

Contoh:

```text
Task created
```

```text
Task updated
```

```text
Task deleted       UNDO
```

```text
Task completed     UNDO
```

Tidak menggunakan dialog untuk feedback sederhana.

---

# 16. UX Rules

## Rule 1 — One Primary Action

Setiap layar harus memiliki satu tindakan utama.

Contoh:

Create Task:

> Save Task

Task Detail:

> Complete Task

---

## Rule 2 — Minimal Input

Jangan meminta informasi yang tidak diperlukan.

Minimal task:

```text
Title
+
Deadline
```

Semua lainnya optional.

---

## Rule 3 — Deadline First

Informasi deadline harus mudah ditemukan.

Prioritas visual:

```text
Task Title
↓
Deadline
↓
Priority
↓
Category
↓
Description
```

---

## Rule 4 — No Cognitive Overload

Hindari:

* terlalu banyak tombol
* terlalu banyak warna
* terlalu banyak statistik
* dashboard kompleks
* konfigurasi reminder yang rumit

---

# 17. Responsive Layout

Target utama:

Android phone.

Namun UI harus tetap mendukung:

* small phones
* standard phones
* large phones
* tablet secara basic

Gunakan:

* adaptive Compose layouts
* scalable spacing
* responsive containers
* system font scaling

---

# 18. Accessibility

Minimum requirement:

* touch target cukup besar
* teks dapat diperbesar
* content description untuk icon
* jangan bergantung hanya pada warna
* kontras teks yang baik
* screen reader support
* keyboard/focus support bila relevan

Contoh:

Jangan hanya:

```text
🔴
```

Tetapi semantic meaning juga tersedia:

```text
High priority
```

---

# 19. Design Direction

Visual design akan dibuat dengan prinsip:

### Minimal

Tidak ada elemen dekoratif yang tidak memiliki fungsi.

### Clean

Whitespace cukup.

### Calm

Tidak menggunakan warna mencolok secara berlebihan.

### Clear

Deadline selalu mudah dibaca.

### Consistent

Spacing, typography, button, card, dan icon memiliki aturan konsisten.

---

# 20. Komponen UI Utama

Komponen yang perlu dibuat sebagai reusable components:

```text
DueSoon
│
├── AppTopBar
├── BottomNavigation
├── TaskCard
├── TaskList
├── DeadlineLabel
├── PriorityIndicator
├── CategoryChip
├── ReminderSelector
├── DateTimePicker
├── PrimaryButton
├── SecondaryButton
├── EmptyState
├── ConfirmationDialog
└── Snackbar
```

Dengan demikian saat coding kita tidak membuat UI setiap layar dari nol.

---

# 21. Final Screen Map

```text
                    DueSoon
                       │
       ┌───────────────┼───────────────┐
       │               │               │
      Home           Tasks          Calendar
       │               │               │
       │               │               │
       └───────┬───────┴───────┬───────┘
               │               │
          Task Detail       Settings
               │
       ┌───────┼───────┐
       │       │       │
      Edit   Complete Delete
       │
       │
   Save Changes
```

Create Task dapat dibuka dari:

```text
Home
Tasks
Calendar
```

melalui tombol `+`.

---

# 22. Definition of Done — Step 4

Step 4 dianggap selesai apabila kita sudah memiliki:

* [x] daftar seluruh screen
* [x] struktur setiap screen
* [x] primary CTA
* [x] secondary actions
* [x] navigation
* [x] empty states
* [x] error/confirmation states
* [x] task lifecycle behavior
* [x] reminder behavior
* [x] accessibility direction
* [x] reusable UI components

## Output Step 4

Kita sekarang memiliki blueprint:

```text
PRD
 ↓
User Flow
 ↓
Information Architecture
 ↓
Wireframe & UI/UX Specification
 ↓
NEXT: Visual Design
```
