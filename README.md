# Utang Tracker

A Filipino debt tracking Android app with digital contracts for barangay mediation evidence.

## Features

- **Debt Management** — Track debts owed to you and debts you owe, with payment history
- **Person Management** — Store borrower/lender profiles with phone and notes
- **Digital Contracts** — Generate signed PDF loan agreements (*Kasunduan sa Pagpapautang*) usable for barangay mediation
- **Remote Signing** — Borrower signs the contract online via a secure link (Firebase Hosting)
- **Disbursement Receipts** — Attach screenshots proving money was sent
- **SMS Reminders** — Send payment reminders directly from the app
- **Share Payment Summary** — Share a formatted payment history via WhatsApp, SMS, email, etc.
- **Interest Tracking** — Set monthly interest rates with optional auto-apply on overdue debts
- **Backup & Restore** — Export/import all data as JSON
- **CSV Export** — Export debts to spreadsheet for analysis
- **Biometric Lock** — Fingerprint/face unlock on app open
- **Dark Mode** — Full light/dark theme support
- **Dashboard** — Net balance overview, debt status chart, quick actions

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM + Hilt + Room
- **Database:** Room (SQLite) — DB v13
- **Preferences:** DataStore
- **Remote:** Firebase Firestore + Firebase Hosting
- **PDF:** Android Canvas-based PDF generation
- **Min SDK:** 26 (Android 8.0)

## Project Structure

```
app/src/main/java/com/cedd/utangtracker/
├── data/
│   ├── local/          # Room DB, entities, DAOs
│   ├── preferences/    # DataStore
│   ├── remote/         # Firebase/Firestore
│   └── repository/     # UtangRepository
├── di/                 # Hilt AppModule
├── domain/model/       # DebtType, DebtStatus
├── navigation/         # AppNavigation, Screen routes
├── pdf/                # ContractPdfGenerator
├── presentation/
│   ├── components/     # Shared UI components
│   ├── contract/       # Contract screen & VM
│   ├── dashboard/      # Dashboard screen & VM
│   ├── debt/           # Debt list, detail, add/edit
│   ├── person/         # Person list, detail, add/edit
│   └── settings/       # Settings screen & VM
└── worker/             # WorkManager (overdue reminders)
```

## Setup

1. Clone the repo
2. Add your `google-services.json` to `app/` (Firebase project required for contracts)
3. Open in Android Studio and run

> **Note:** `google-services.json` is excluded from version control. The app works without Firebase — contract remote signing features will simply be unavailable.

## Version

**v1.0.0** — Initial release

---

*A project by Cedd*
