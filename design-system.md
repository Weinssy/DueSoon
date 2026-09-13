# STEP 5 — VISUAL DESIGN SYSTEM

## 1. Design Direction

### Brand

**Nama:** DueSoon

**Tagline:**

> Know what needs your attention next.

### Design personality

DueSoon harus terasa:

* Minimal
* Clean
* Calm
* Modern
* Reliable
* Fast
* Focused

Bukan seperti aplikasi project management yang kompleks.

Prinsip visual utama:

> **Deadline should be obvious. Everything else should stay quiet.**

---

# 2. Color System

Gunakan sistem warna yang sederhana.

Jangan menggunakan terlalu banyak warna.

## Primary

Warna utama:

**Indigo / Blue**

Tujuan:

* CTA
* selected navigation
* interactive elements
* links
* focus state

Recommended:

```text
Primary
#4F46E5
```

Dark variation:

```text
#6366F1
```

---

# 3. Semantic Colors

Warna semantic hanya digunakan ketika memiliki arti.

## Success

Untuk:

* Completed
* Success feedback

```text
#16A34A
```

## Warning

Untuk:

* Due Soon
* deadline semakin dekat

```text
#D97706
```

## Error

Untuk:

* Overdue
* destructive action

```text
#DC2626
```

## Neutral

Untuk:

* Normal priority
* secondary information

Gunakan grayscale daripada warna tambahan.

---

# 4. Priority System

Priority tidak boleh mendominasi UI.

### Low

```text
Neutral
```

### Normal

```text
Neutral / subtle
```

### High

```text
Error semantic
```

Contoh:

```text
HIGH
```

lebih baik menggunakan teks + indikator daripada hanya warna.

---

# 5. Deadline State Colors

Deadline merupakan informasi paling penting.

## Upcoming

Neutral.

```text
Friday · 20:00
```

## Due Soon

Warning.

```text
Due tomorrow · 09:00
```

## Due Today

Stronger warning.

```text
Due today · 14:00
```

## Overdue

Error.

```text
Overdue · Sep 12, 14:00
```

## Completed

Success / muted.

```text
Completed
```

---

# 6. Dark Mode

DueSoon harus mendukung Light dan Dark Mode sejak awal.

## Light Theme

```text
Background
#FAFAFA

Surface
#FFFFFF

Primary Text
#171717

Secondary Text
#737373

Divider
#E5E5E5
```

## Dark Theme

```text
Background
#0F0F10

Surface
#18181B

Primary Text
#F5F5F5

Secondary Text
#A1A1AA

Divider
#27272A
```

Jangan menggunakan pure black `#000000` sebagai background utama.

Tujuannya agar UI terasa lebih nyaman dan tidak terlalu keras.

---

# 7. Typography

Gunakan font system Android.

**Font family:**

```text
Roboto
```

Tidak perlu custom font untuk MVP.

## Type Scale

### Display

Digunakan untuk angka/status besar jika diperlukan.

```text
32sp
Bold
```

### Screen Title

```text
24sp
Bold
```

Contoh:

> Tasks

### Section Title

```text
16sp
SemiBold
```

Contoh:

> Needs Attention

### Task Title

```text
16sp
Medium
```

### Body

```text
14sp
Regular
```

### Secondary

```text
13sp
Regular
```

### Caption

```text
12sp
Regular
```

---

# 8. Typography Hierarchy

Prioritas visual:

```text
Screen Title
     ↓
Task Title
     ↓
Deadline
     ↓
Secondary information
```

Contoh:

```text
Mathematics Assignment
Due today · 14:00
School · High
```

Mata pengguna harus langsung menangkap:

**Mathematics Assignment → Due today · 14:00**

---

# 9. Spacing System

Gunakan sistem berbasis kelipatan 4dp.

```text
4dp
8dp
12dp
16dp
20dp
24dp
32dp
40dp
48dp
```

Default screen padding:

```text
16dp
```

Section spacing:

```text
24dp
```

Task card internal padding:

```text
16dp
```

---

# 10. Corner Radius

Gunakan radius yang konsisten.

### Small

```text
8dp
```

Untuk:

* chips
* small controls

### Medium

```text
12dp
```

Untuk:

* cards
* input fields

### Large

```text
16dp
```

Untuk:

* prominent containers
* dialogs

### Floating Action Button

```text
16dp / Material standard shape
```

Jangan membuat semua komponen terlalu bulat.

DueSoon harus terlihat **clean**, bukan playful.

---

# 11. Task Card

Task Card adalah komponen terpenting setelah deadline.

## Structure

```text
┌──────────────────────────────────┐
│                                  │
│ Mathematics Assignment           │
│ Due today · 14:00                │
│ School · High                    │
│                                  │
└──────────────────────────────────┘
```

## Spacing

```text
Card padding: 16dp
Title → deadline: 6dp
Deadline → metadata: 6dp
Card → card: 8dp
```

---

# 12. Task Card Hierarchy

Contoh:

```text
Mathematics Assignment
↑
primary

Due today · 14:00
↑
important

School · High
↑
secondary
```

Jangan membuat:

```text
SCHOOL
HIGH
TODAY
14:00
```

semuanya besar.

Itu menyebabkan cognitive overload.

---

# 13. Deadline Label

Deadline adalah komponen reusable.

States:

```text
Upcoming
Due Soon
Due Today
Overdue
Completed
No Deadline
```

Contoh:

```text
Due in 5 days
```

```text
Due tomorrow · 09:00
```

```text
Due today · 14:00
```

```text
Overdue · Sep 12, 14:00
```

Untuk task tanpa deadline:

```text
No deadline
```

---

# 14. Buttons

## Primary Button

Digunakan untuk action utama.

Contoh:

```text
┌─────────────────────────────┐
│         SAVE TASK           │
└─────────────────────────────┘
```

Properties:

```text
Height: 48dp
Radius: 12dp
Text: 14sp Medium
```

## Secondary Button

Untuk action sekunder.

Contoh:

```text
Edit Task
```

## Destructive Button

Digunakan untuk:

```text
Delete Task
```

Gunakan semantic error.

Jangan menggunakan warna error untuk action normal.

---

# 15. Floating Action Button

Home dan Tasks memiliki tombol:

```text
+
```

Tujuan:

> Create Task

Ukuran mengikuti standar Material/Android.

Icon harus sederhana.

Tidak perlu label:

```text
+ Create Task
```

untuk MVP.

Cukup:

```text
+
```

---

# 16. Bottom Navigation

Empat destination:

```text
⌂ Home
☑ Tasks
▣ Calendar
⚙ Settings
```

Dalam implementasi Android gunakan Material icons yang sesuai.

State:

### Selected

Primary color.

### Unselected

Neutral gray.

Jangan menggunakan warna berbeda untuk setiap menu.

---

# 17. Icons

Gunakan satu icon family.

Recommended:

**Material Symbols / Material Icons**

Contoh:

```text
Add
ArrowBack
CalendarToday
Check
Delete
Edit
Notifications
Search
Settings
MoreVert
```

Jangan mencampur:

* Material Icons
* Font Awesome
* custom icons
* emoji

sebagai sistem icon utama.

Emoji hanya boleh digunakan jika memang bagian dari content.

---

# 18. Input Fields

Create Task menggunakan input sederhana.

## Title

```text
┌──────────────────────────────┐
│ What needs to be done?       │
└──────────────────────────────┘
```

## Description

Multiline:

```text
┌──────────────────────────────┐
│ Add details (optional)       │
│                              │
│                              │
└──────────────────────────────┘
```

## Input rules

* Label jelas
* Placeholder singkat
* Focus state jelas
* Error state jelas
* Tidak menggunakan border berlebihan

---

# 19. Date & Time Picker

Deadline:

```text
📅 Select date
🕐 Select time
```

Setelah dipilih:

```text
📅 Sep 18, 2026
🕐 14:00
```

User harus dapat mengubah tanggal dan waktu secara terpisah.

---

# 20. Category Chip

Contoh:

```text
[ School ]
```

atau:

```text
[ Work ]
```

Chip tidak perlu memiliki warna berbeda untuk setiap category pada MVP.

Gunakan neutral surface.

---

# 21. Priority Selector

Gunakan segmented selection atau radio-style control.

```text
Priority

○ Low
● Normal
○ High
```

High dapat menggunakan semantic warning/error indication.

Namun label **High** tetap harus ditampilkan.

---

# 22. Reminder Selector

```text
Reminder

● Smart Reminder
○ Custom Reminder
○ No Reminder
```

Smart Reminder menjadi default.

UI harus menjelaskan bahwa Smart Reminder tidak membutuhkan konfigurasi manual.

---

# 23. Home Visual Hierarchy

Home harus memiliki struktur:

```text
Greeting
    ↓
Needs Attention
    ↓
Urgent Tasks
    ↓
Upcoming
    ↓
Navigation
```

Jangan membuat dashboard penuh dengan:

* statistics
* charts
* productivity score
* streak
* progress bars

MVP harus tetap deadline-focused.

---

# 24. Overdue Visual Treatment

Overdue harus mudah dikenali tetapi tidak agresif.

Gunakan:

* error-colored deadline
* subtle indicator
* text "Overdue"

Contoh:

```text
┌──────────────────────────────┐
│ Math Assignment              │
│ Overdue · Sep 12, 14:00      │
│ School · High                │
└──────────────────────────────┘
```

Tidak perlu membuat seluruh card berwarna merah.

---

# 25. Completed Visual Treatment

Completed task menjadi lebih tenang.

Contoh:

```text
┌──────────────────────────────┐
│ ✓ Math Assignment             │
│ Completed                     │
│ School                        │
└──────────────────────────────┘
```

Title dapat menggunakan muted appearance.

Task tetap dapat dibuka jika pengguna ingin melihat detailnya.

---

# 26. Motion & Animation

Animasi harus subtle.

Gunakan:

* fade
* small scale
* standard navigation transitions

Contoh setelah Complete:

```text
Complete
   ↓
Task moves to Completed
   ↓
Snackbar appears
```

Hindari:

* bouncing animation
* confetti
* excessive motion
* gamification

DueSoon bukan aplikasi game.

---

# 27. Notification Visual Language

Notification harus konsisten dengan UI.

Contoh:

```text
DueSoon

Math Assignment
Due today at 14:00

Complete     Snooze
```

Untuk MVP:

Primary notification action:

```text
Open
```

Future:

```text
Complete
Snooze
```

---

# 28. Design Tokens

Implementasi sebaiknya menggunakan centralized design tokens.

Contoh:

```text
DueSoonColors
DueSoonTypography
DueSoonSpacing
DueSoonShapes
```

Conceptual structure:

```text
DesignSystem
│
├── Colors
├── Typography
├── Spacing
├── Shapes
├── Elevation
└── Components
```

Tujuannya agar perubahan desain di masa depan mudah dilakukan.

---

# 29. Elevation

Gunakan elevation secara minimal.

Task card:

```text
Low elevation
```

Bottom navigation:

```text
Subtle elevation
```

Dialog:

```text
Higher elevation
```

Jangan membuat semua card memiliki shadow besar.

Flat surface + spacing lebih sesuai dengan karakter DueSoon.

---

# 30. Visual Rules

### Rule 1

**Deadline > decoration**

### Rule 2

**Content > chrome**

### Rule 3

**One primary CTA per screen**

### Rule 4

**Color has meaning**

### Rule 5

**Whitespace is part of the design**

### Rule 6

**Don't use color as the only information**

### Rule 7

**Keep interactions predictable**

---

# 31. Design System Summary

```text
BRAND
DueSoon

STYLE
Minimal · Clean · Calm

PRIMARY
Indigo

SEMANTIC
Green = Completed
Orange = Due Soon
Red = Overdue
Gray = Neutral

TYPOGRAPHY
Roboto

SPACING
4dp grid

SCREEN PADDING
16dp

CARD RADIUS
12dp

BUTTON HEIGHT
48dp

CARD PADDING
16dp

ICON SYSTEM
Material Symbols

THEMES
Light + Dark
```

---

# 32. Visual Component Inventory

Komponen yang harus dibuat sebelum screen implementation:

```text
Foundation
├── Colors
├── Typography
├── Spacing
├── Shapes
└── Icons

Components
├── AppTopBar
├── BottomNavigation
├── TaskCard
├── DeadlineLabel
├── PriorityIndicator
├── CategoryChip
├── PrimaryButton
├── SecondaryButton
├── TextInput
├── DateTimeSelector
├── ReminderSelector
├── EmptyState
├── Snackbar
└── ConfirmationDialog

Screens
├── HomeScreen
├── TasksScreen
├── CalendarScreen
├── SettingsScreen
├── CreateTaskScreen
├── TaskDetailScreen
└── EditTaskScreen
```

---

# 33. Final Design Principle

Semua keputusan visual DueSoon harus melewati pertanyaan:

> **Apakah ini membantu pengguna memahami deadline dengan lebih cepat?**

Jika jawabannya tidak:

**hapus, sederhanakan, atau pindahkan.**

---

# 34. Step 5 Definition of Done

Step 5 selesai apabila:

* [x] Color system ditentukan
* [x] Light theme ditentukan
* [x] Dark theme ditentukan
* [x] Typography ditentukan
* [x] Spacing ditentukan
* [x] Radius ditentukan
* [x] Button system ditentukan
* [x] Card system ditentukan
* [x] Deadline states ditentukan
* [x] Priority system ditentukan
* [x] Icon system ditentukan
* [x] Navigation style ditentukan
* [x] Accessibility direction ditentukan
* [x] Design tokens ditentukan
* [x] Component inventory ditentukan

## Product Definition Progress

```text
STEP 1  Product Concept
   ✓

STEP 2  PRD
   ✓

STEP 3  User Flow & IA
   ✓

STEP 4  Wireframe & UI/UX
   ✓

STEP 5  Visual Design System
   ✓

STEP 6  Android Project Setup & Coding
   ↓
   NEXT
```
