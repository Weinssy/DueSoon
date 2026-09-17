# DueSoon v1.4.0 - Product Requirements Document (PRD)

**Version:** 1.4.0
**Theme:** Smart Attention Ranking
**Status:** APPROVED

## 1. Vision & Objective
DueSoon v1.4.0 helps users identify which tasks need attention first through explainable deadline and priority ranking, without introducing AI or project-management complexity. The goal is to blend strict chronological sorting with user-defined priority to surface the most critical tasks accurately.

## 2. Scope Boundary

| Feature | Decision |
|---|---|
| Explainable attention ranking | **In scope** |
| Deadline-first ordering | **In scope** |
| Priority interaction (Blending deadline + priority) | **In scope** |
| Widget sorting consistency | **In scope** |
| Attention label/reason | **In scope** (jika UI mendukung) |
| Database migration (Room v4) | **Out of scope** |
| Backup schema changes (PortableBackup v2) | **Out of scope** |
| Notification engine rewrite | **Out of scope** |
| AI-generated prioritization | **Out of scope** |
| User-configurable scoring formulas | **Out of scope** |

## 3. Product Rules & Logic
To achieve **Smart Attention Ranking** without modifying the database, the presentation layer (`HomeFilterLogic`) will use a weighted ranking algorithm:

1. **Base Value (Deadline):** The proximity of the deadline remains the primary factor (e.g., Overdue vs Today vs Upcoming).
2. **Modifier (Priority):** A task's priority (High, Normal, Low) acts as a modifier to push a task slightly higher or lower *within* its logical time boundary.
3. **Overdue Sanctity:** A task that is Overdue is fundamentally failing a temporal constraint and must always rank higher than any non-overdue task, regardless of priority.

## 4. Acceptance Criteria (AC)

- **AC-01 (Overdue Integrity):** Task `OVERDUE` tidak dapat diturunkan ranking-nya ke bawah batas _Today_ hanya karena memiliki _priority_ rendah.
- **AC-02 (No Deadline Integrity):** Task tanpa _deadline_ tetap muncul, didorong ke prioritas bawah (hanya diurutkan berdasar _priority_ atau waktu pembuatan), dan tidak memperoleh urgensi berbasis waktu palsu.
- **AC-03 (Deterministic Sorting):** Sorting bersifat deterministik (menghasilkan urutan yang sama secara mutlak pada _set input_ yang sama).
- **AC-04 (Filter Compatibility):** Filter status, filter kategori, dan fitur pencarian teks (_search_) tetap berfungsi tanpa terganggu oleh algoritma _ranking_ baru.
- **AC-05 (Widget Parity):** Widget Android dan tampilan *Home* menggunakan aturan _ranking_ dan fungsi *sort* yang 100% konsisten/berbagi logika yang sama.
- **AC-06 (Data Portability Integrity):** Fungsionalitas *backup, import*, dan *restore* dari v1.3.0 tidak berubah, tidak rusak, dan tidak mengalami modifikasi skema.
- **AC-07 (Test Coverage):** Terdapat _Unit Test_ yang menutupi skenario batas waktu (_boundaries_) dan persilangan kombinasi seluruh _priority_.
- **AC-08 (Zero Mutation):** Perubahan murni terjadi pada layer *domain/UI/presentation*. Perubahan tidak secara otomatis (ataupun diam-diam) memodifikasi data task di Room database.
