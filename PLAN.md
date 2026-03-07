# Utang Tracker — Project Plan

> **Huwag kalimutan. Huwag ipagkait. May katibayan ka na.**
> *(Don't forget. Don't deny. You have proof.)*

---

## App Overview

A debt tracking app for Filipinos that handles informal lending between friends, family, and neighbors. Key differentiator: **digital contract with signature** that can be used as evidence in barangay mediation or Small Claims Court.

---

## Package & Identity

- **App name:** Utang Tracker
- **Package:** `com.cedd.utangtracker`
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35 (Android 15)
- **Language:** Kotlin + Jetpack Compose (Material 3)
- **Architecture:** MVVM + Repository + Hilt DI
- **Currency:** PHP (₱)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository |
| DI | Hilt 2.52 |
| Database | Room (v1 to start) |
| Preferences | DataStore |
| Navigation | Navigation Compose |
| Images | Coil |
| PDF Generation | iTextPDF / PdfDocument (Android) |
| Signature Canvas | Custom Compose Canvas |
| Background | WorkManager (reminders) |
| Min SDK | 26 |

---

## Database Schema (v1)

### `persons`
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK | auto-increment |
| name | TEXT | borrower/lender name |
| photoPath | TEXT? | optional contact photo |
| phone | TEXT? | for SMS reminder |
| createdAt | INTEGER | timestamp |

### `debts`
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK | auto-increment |
| personId | INTEGER FK | → persons |
| type | TEXT | `OWED_TO_ME` or `I_OWE` |
| amount | REAL | original amount |
| paidAmount | REAL | total paid so far |
| purpose | TEXT | reason for debt |
| dateCreated | INTEGER | timestamp |
| dateDue | INTEGER? | due date (nullable) |
| interestRate | REAL | 0.0 = no interest |
| status | TEXT | `ACTIVE`, `SETTLED`, `OVERDUE` |
| hasContract | INTEGER | 0 or 1 (boolean) |
| notes | TEXT | extra notes |

### `payments`
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK | auto-increment |
| debtId | INTEGER FK | → debts (CASCADE) |
| amount | REAL | amount paid this time |
| datePaid | INTEGER | timestamp |
| notes | TEXT? | e.g. "via GCash" |

### `contracts`
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK | auto-increment |
| debtId | INTEGER FK | → debts (CASCADE) |
| contractNumber | TEXT | e.g. UTC-2026-00001 |
| lenderName | TEXT | |
| borrowerName | TEXT | |
| amount | REAL | |
| purpose | TEXT | |
| dateCreated | INTEGER | |
| dateDue | INTEGER? | |
| interestRate | REAL | |
| lenderSignaturePath | TEXT? | saved PNG of signature |
| borrowerSignaturePath | TEXT? | saved PNG of signature |
| witnessName | TEXT? | optional |
| witnessSignaturePath | TEXT? | optional |
| pdfPath | TEXT? | path to generated PDF |
| gpsLocation | TEXT? | city/barangay at signing |
| isSigned | INTEGER | 0 or 1 |
| createdAt | INTEGER | |

---

## Screens & Navigation

```
BottomNav:
├── Dashboard (Home)
├── Debts
│   ├── Owed to Me
│   └── I Owe
├── Persons
└── Settings

Modals / Sub-screens:
├── Add/Edit Debt
├── Debt Detail
│   ├── Payment History
│   └── Add Payment
├── Contract Creator
│   ├── Contract Preview
│   ├── Signature Pad (Lender)
│   ├── Signature Pad (Borrower)
│   └── PDF Viewer
└── Person Detail
```

---

## Features by Tier

### Tier 1 — Core (v1.0)
- [ ] Add/edit/delete persons (name, photo, phone)
- [ ] Add debt (owed to me / I owe, amount, purpose, due date)
- [ ] Dashboard: total owed to me vs. total I owe
- [ ] Debt list with status badges (Active, Overdue, Settled)
- [ ] Mark partial or full payment
- [ ] Payment history per debt
- [ ] Swipe to delete debt
- [ ] Dark mode

### Tier 2 — Contracts (v1.1)
- [ ] Generate digital contract from debt details
- [ ] Signature pad (finger drawing on Canvas)
- [ ] Lender + Borrower signatures
- [ ] Optional witness signature
- [ ] Lock contract after both parties sign (no edits)
- [ ] Export contract as PDF
- [ ] Share PDF via Messenger/Viber/email
- [ ] "PAID" watermark on settled contract PDF
- [ ] Contract number (UTC-YYYY-XXXXX)

### Tier 3 — Reminders (v1.2)
- [ ] Set due date reminder per debt
- [ ] WorkManager daily check for overdue debts
- [ ] Pre-written polite reminder message templates (Filipino/English)
- [ ] Copy reminder to clipboard (paste in Messenger/Viber)
- [ ] Optional direct SMS send

### Tier 4 — Polish (v1.3)
- [ ] Search debts by person name
- [ ] Filter: All / Overdue / Settled / Active
- [ ] Sort: by amount / by due date / by person
- [ ] Interest calculator (simple/compound)
- [ ] CSV export of all debts
- [ ] Home screen widget (total owed to me)
- [ ] Spending insights (most borrowed from/to)
- [ ] Backup to Google Drive

---

## Contract PDF Layout

```
┌─────────────────────────────────────┐
│         KASUNDUAN SA UTANG          │
│          DEBT AGREEMENT             │
│                                     │
│  Contract No: UTC-2026-00001        │
│  Date: March 2, 2026                │
│                                     │
│  I, [Borrower], acknowledge that    │
│  I have borrowed ₱[Amount] from     │
│  [Lender] on [Date] for the         │
│  purpose of [Purpose].              │
│                                     │
│  I agree to repay this amount       │
│  in full by [Due Date].             │
│                                     │
│  Interest: [None / X% per month]    │
│                                     │
│  ─────────────  ─────────────       │
│  Lender Sig     Borrower Sig        │
│  [signature]    [signature]         │
│                                     │
│  Witness (optional):                │
│  ─────────────                      │
│  [signature]                        │
│                                     │
│  Signed at: [City], Philippines     │
│  Timestamp: 2026-03-02 10:42 AM     │
│                                     │
│  * For personal record only.        │
│    Consult a lawyer for legal use.  │
└─────────────────────────────────────┘
```

---

## Folder Structure

```
app/src/main/java/com/cedd/utangtracker/
├── BudgetTrackerApp.kt
├── MainActivity.kt
├── data/
│   ├── local/
│   │   ├── entity/         # PersonEntity, DebtEntity, PaymentEntity, ContractEntity
│   │   ├── dao/            # PersonDao, DebtDao, PaymentDao, ContractDao
│   │   ├── relation/       # DebtWithPayments, PersonWithDebts
│   │   └── UtangDatabase.kt
│   ├── preferences/        # DataStore (dark mode, user name)
│   └── repository/         # UtangRepository
├── di/                     # AppModule (Hilt)
├── domain/model/           # UI state models
├── notification/           # WorkManager reminder
├── pdf/                    # Contract PDF generator
├── presentation/
│   ├── dashboard/          # DashboardScreen + ViewModel
│   ├── debt/               # DebtListScreen, DebtDetailScreen, AddDebtScreen
│   ├── contract/           # ContractScreen, SignaturePad, PDFViewer
│   ├── person/             # PersonListScreen, PersonDetailScreen
│   ├── settings/           # SettingsScreen
│   └── components/         # Reusable composables
├── navigation/             # AppNavigation
└── widget/                 # Home screen widget
```

---

## Milestones

| Phase | Features | Est. Sessions |
|---|---|---|
| v1.0 | Core debt tracking + dark mode | 3–4 |
| v1.1 | Digital contracts + PDF + signatures | 3–4 |
| v1.2 | Reminders + message templates | 2 |
| v1.3 | Polish + export + widget | 2–3 |

---

## Notes

- Reuse patterns from BudgetTracker (MVVM, Hilt, Room, Compose)
- Signature pad = custom `Canvas` composable, save drawing as PNG to filesDir
- PDF generation = Android's built-in `PdfDocument` API (no external lib needed for simple layout) or iText for richer formatting
- Contract is immutable once both parties sign — enforce at DB level (no update after `isSigned = 1`)
- GPS location is optional and requires `ACCESS_COARSE_LOCATION` permission
