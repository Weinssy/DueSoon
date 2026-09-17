# DueSoon v1.4.0 Release Notes

**Version:** 1.4.0  
**Release Date:** September 2026  

## What's New

### 🎯 Smart Attention Ranking
Say goodbye to manual task sorting! DueSoon v1.4.0 introduces an intelligent attention-based ranking system that automatically surfaces the tasks that need your immediate focus. 

Instead of just sorting blindly by deadline or priority, the new **Hybrid Urgency Matrix** intelligently blends both:
- Overdue tasks always stay at the very top.
- A low-priority task due today is recognized as more urgent than a high-priority task due in three days.
- Tasks with the exact same urgency tie-break deterministically, keeping your list stable and jitter-free.

### 🎨 Explainable Urgency Badges
Task deadline badges (and Widget indicators) now use smarter colors to reflect true urgency. A low priority task due today will gracefully display a "Warning" (Orange) color instead of a false "Critical" (Red) alarm, matching your actual attention needs.

### 📱 100% Widget Parity
The Android Glance Home Screen Widget has been completely overhauled to use the exact same Smart Attention Ranking algorithm as the main app. Your widget and your home screen will always agree on what task is #1.

## Under the Hood
- **Frozen Rendering Clock:** Sorting operations now lock the system clock during rendering to eliminate millisecond-level sorting jitter.
- **Zero Schema Migrations:** All ranking intelligence happens entirely in-memory, requiring absolutely zero changes to your existing local database or portable JSON backup files. 
- **Deterministic Engine:** Absolute mathematical stability for sorting edge cases using creation timestamps and internal IDs as final tie-breakers.
