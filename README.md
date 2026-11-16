# RD Diary

A simple Android app for a Postal RD agent: customer RD accounts, monthly collections (full, partial or advance),
pending and advance lists, today's collection, maturity alerts with 60 → 120 month extension, backup and restore.
Telugu and English. Customer data stays on the phone — it is never part of this repository.

Every push to `main` builds a signed APK (GitHub Actions) and publishes it under **Releases** (needs the `KEYSTORE_BASE64` secret).
