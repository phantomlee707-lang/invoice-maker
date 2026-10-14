# Invoice Maker

**Offline-first invoicing for Android.** Create, manage, and share professional invoice PDFs entirely on your device — no account, no backend, no internet required. Built for freelancers, contractors, and small businesses who just need clean invoices fast.

A [Rainweaver Studio](https://rainweaver.qd.je) app.

---

## Features

- **Business profile** — name, contact, address, currency, and payment instructions
- **Customers** — reusable records, or enter one-off details per invoice
- **Catalog** — save products/services with default price and tax
- **Invoice builder** — line items with quantity, unit price, discount, and tax; **live totals**; quick-add from your catalog
- **Three PDF templates** — Minimal, Classic, and Modern, rendered on-device
- **Invoice numbering** — automatic `YYYYMMDDHHmmss` timestamps
- **History** — search by number/customer, status tracking (draft / issued / paid), duplicate, mark paid
- **Share PDFs** via Android's share sheet (no broad filesystem access)
- **Backup & restore** — local export/import of all your data
- **Immutable snapshots** — editing a customer or catalog item never rewrites an issued invoice
- **Exact money math** — integer minor units, no floating-point

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Room** for local persistence with immutable invoice snapshots
- **Android `PdfDocument`** for on-device, multi-page PDF generation
- **FileProvider + SAF** for sharing and backup
- **min SDK 24** (Android 7.0)

## Download

Grab the latest signed release from [**Releases**](https://github.com/phantomlee707-lang/invoice-maker/releases/latest):

- `app-release.apk` — release build (optimized, signed)

Install it directly on your device (enable "install from unknown sources"). The release APK is signed with a different key than debug builds, so uninstall any debug version first.

## Build from source

```bash
git clone https://github.com/phantomlee707-lang/invoice-maker.git
cd invoice-maker
./gradlew assembleDebug        # debug APK (works out of the box)
```

A **release** build additionally requires a `keystore.properties` file (with your signing keystore) in the project root — it is intentionally **not** committed:

```properties
storeFile=release.keystore
storePassword=…
keyAlias=…
keyPassword=…
```

Then: `./gradlew assembleRelease`

> Requires JDK 17 and the Android SDK (compileSdk 34).

## Tests

```bash
./gradlew testDebugUnitTest
```

Covers money math, invoice calculation/totals, validation, and invoice-number formatting.

## License

© Rainweaver Studio. All rights reserved.
