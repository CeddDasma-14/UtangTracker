# LoanTrack (Utang Tracker)

A Filipino loan & debt tracking Android app with digital contracts, loan ledger, and payment scheduling — built for personal and barangay-level use.

## Features

### Core
- **Debt Management** — Track debts owed to you and debts you owe, with full payment history
- **Person Management** — Store borrower/lender profiles with photo and phone number
- **Dashboard** — Net balance overview, debt status chart, overdue alerts, quick actions
- **Search & Sort** — Filter debts by name/purpose, sort by date, amount, due date, or status

### Loan Management
- **Interest Tracking** — Set monthly interest rates with optional auto-apply on overdue debts
- **Extend Loan Term** — Extend due dates with optional penalty interest (waivable), negotiated rate support, and undo via Snackbar
- **Payment Schedule** — Collapsible monthly breakdown table with extension rows, cumulative paid, and remaining balance
- **Loan Ledger** — Monthly ledger tracking opening balance, interest added, carry-over, payments, and closing balance
- **EXT Badge** — Visual indicator on debt cards and detail screen showing total extension months

### Contracts & Signing
- **Digital Contracts** — Generate signed PDF loan agreements (*Kasunduan sa Pagpapautang*) usable for barangay mediation
- **Remote Signing** — Borrower signs the contract online via a secure Firebase-hosted link
- **Co-makers** — Add co-signers to a loan contract
- **Collateral** — Record collateral in the contract PDF
- **Disbursement Receipts** — Attach screenshots proving money was sent

### Sharing & Export
- **SMS Reminders** — Send payment reminders directly from the app
- **Share Payment Summary** — Share formatted payment history via WhatsApp, SMS, email, etc.
- **CSV Export** — Export debts to spreadsheet for analysis
- **Backup & Restore** — Export/import all data as JSON

### Security & UX
- **Debt Lock** — Lock individual debts to prevent accidental edits or deletion
- **Biometric Lock** — Fingerprint/face unlock on app open
- **Dark Mode** — Full light/dark theme support
- **Onboarding & Coach Marks** — First-run tour highlighting key features
- **Animated Splash Screen** — Branded launch experience

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Hilt + Room |
| Database | Room (SQLite) — DB v6+ with migrations |
| Preferences | DataStore |
| Remote | Firebase Firestore + Firebase Hosting |
| PDF | Android Canvas-based generation |
| Background | WorkManager (overdue checks) |
| Min SDK | 26 (Android 8.0) |

## Project Structure

```
app/src/main/java/com/cedd/utangtracker/
├── data/
│   ├── local/          # Room DB, entities, DAOs, migrations
│   ├── preferences/    # DataStore
│   ├── remote/         # Firebase/Firestore
│   └── repository/     # UtangRepository
├── di/                 # Hilt AppModule
├── domain/model/       # DebtType, DebtStatus
├── navigation/         # AppNavigation, Screen routes
├── pdf/                # ContractPdfGenerator
├── presentation/
│   ├── components/     # DebtCard, PremiumDialog, shared UI
│   ├── contract/       # Contract screen & VM
│   ├── dashboard/      # Dashboard screen & VM
│   ├── debt/           # Debt list, detail, add/edit, extend dialog, payment schedule
│   ├── ledger/         # Loan Ledger screen & VM
│   ├── person/         # Person list, detail, add/edit
│   └── settings/       # Settings screen & VM
└── worker/             # WorkManager (overdue reminders)
```

## Setup

1. Clone the repo
2. Add your `google-services.json` to `app/` (Firebase project required for contracts)
3. Open in Android Studio and run

> **Note:** `google-services.json` is excluded from version control. The app works without Firebase — contract remote signing features will simply be unavailable.

## Download

See [Releases](https://github.com/CeddDasma-14/UtangTracker/releases) for the latest APK.

## Version History

| Version | Highlights |
|---|---|
| **v1.2.x** | Extend Loan Term, Payment Schedule table, EXT badge, Loan Ledger, Backup/Restore, CSV Export |
| **v1.1.0** | Reservations, onboarding, coach mark tour, gradient UI, lender name setup |
| **v1.0.0** | Initial release — core debt tracking, contracts, payments |

---

*A project by Cedd*
