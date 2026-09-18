# DueSoon v1.6.0 — Release Notes

**Version:** 1.6.0 (Build 7)  
**Release Date:** September 18, 2026

DueSoon v1.6.0 brings interactive Widgets and Quick Actions directly to your home screen!

## 🚀 What's New
* **Interactive Widgets:** You can now mark tasks as completed directly from the home screen widget without opening the app!
* **Quick Add Button:** We added a handy `(+)` button to the widget header. Tapping it instantly opens the app straight to the "Add Task" screen.
* **Safe Checkboxes:** The checkbox on the widget is strictly sized to prevent accidental taps when you actually meant to open the task details. 

## 🛠 Under the Hood (Technical Notes)
* **Domain Idempotency:** Implemented `CompleteTaskUseCase` that safely guards against rapid double-tapping on the widget, guaranteeing atomic completion, recurrence calculation, and alarm cancellation.
* **Action Parameters:** Upgraded Glance intent handling to securely pass explicit arguments (`quick_add`) via `ActionParameters` directly to the Navigation controller.
* **Database Stability:** Zero Room database schema changes. We are still running flawlessly on Schema 3.
* **Dependencies:** Clean bump with no new third-party libraries introduced. Everything remains native Jetpack Compose & Glance.
