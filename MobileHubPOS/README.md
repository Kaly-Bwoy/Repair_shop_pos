# MobileHub POS — Complete Offline Android Application

MobileHub POS is a professional, high-performance, and lightweight native Android application designed specifically to replace router-based POS setups for phone repair shops and accessory stores. It runs fully offline with absolute zero internet dependencies, ensuring that all business data remains safely on the device.

---

## 📱 Performance & Optimization (Samsung SM-T561 & Android 8+)
- **RAM Footprint:** Optimized for low-end devices (1.5GB – 2GB RAM).
- **Offline-First:** No heavy transitions, zero-dependency data structures, and optimized Room SQLite queries.
- **Image Compression:** Custom path buffers to preserve system memory when saving device state photographs.
- **Support:** Full backward compatibility to API Level 24 (Android 7.0/8.0).

---

## 🏛️ Project Architecture (Clean MVVM)
The project structure cleanly segregates the data models, transactional layers, repositories, business logic (ViewModels), and responsive UI views (Jetpack Compose with Material Design 3):

```text
MobileHubPOS/
├── gradle/
│   └── libs.versions.toml (Version catalog for easy dependency tracking)
├── build.gradle.kts
├── settings.gradle.kts
└── app/
    ├── build.gradle.kts (Android configurations & Room declarations)
    ├── proguard-rules.pro (Size optimization & keeping local SQLite models)
    └── src/
        └── main/
            ├── AndroidManifest.xml (Security, App class, Launcher details)
            ├── res/
            │   ├── drawable/ic_launcher.xml (Vector asset for storefront logo)
            │   ├── xml/ (Offline cloud-backup rules preventing leaks)
            │   └── values/strings.xml
            └── java/com/mobilehub/pos/
                ├── MobileHubApplication.kt (Supervisor scope and lazy repositories)
                ├── data/
                │   ├── local/
                │   │   ├── AppDatabase.kt (Core SQLite Database)
                │   │   ├── dao/
                │   │   │   ├── ProductDao.kt (Stock, Categories, movements)
                │   │   │   ├── CustomerDao.kt (Client registry, searching)
                │   │   │   ├── SaleDao.kt (Invoices, cart transactions, checkouts)
                │   │   │   ├── RepairDao.kt (Tickets, parts list adjustments)
                │   │   │   ├── ExpenseDao.kt (Outgoing expenses & Settings keys)
                │   │   │   └── AdminDao.kt (StoreProfile setup, PIN logs)
                │   │   └── entity/ (13 custom entities mapped to SQLite tables)
                │   │       ├── Category.kt, Product.kt, Customer.kt, Sale.kt, SaleItem.kt,
                │   │       ├── Repair.kt, RepairPart.kt, Payment.kt, Expense.kt, StockHistory.kt,
                │   │       └── Settings.kt, StoreProfile.kt, User.kt
                │   └── repository/ (Thread-safe background transaction management)
                │       ├── InventoryRepository.kt, SalesRepository.kt, CustomerRepository.kt,
                │       ├── RepairRepository.kt, and BackupRepository.kt
                └── ui/
                    ├── MainActivity.kt (Dual state launcher: intercepts Setup Wizard vs Main app)
                    ├── theme/ (Color.kt, Type.kt, Theme.kt - custom tech-blue scheme)
                    └── screens/ (Responsive, large touch target layouts)
                        ├── setup/ (SetupWizardScreen.kt - first launch brand installer)
                        ├── dashboard/ (DashboardScreen.kt, ViewModel)
                        ├── pos/ (POSScreen.kt, ViewModel - Barcode typing & responsive invoice)
                        ├── inventory/ (InventoryScreen.kt, ViewModel - Stock history logs)
                        ├── repairs/ (RepairsScreen.kt, ViewModel - Lock codes, status badges, parts markup)
                        ├── customers/ (CustomersScreen.kt, ViewModel - Dual profile sheets)
                        ├── reports/ (ReportsScreen.kt, ViewModel - Net profit math, bestseller logs)
                        └── settings/ (SettingsScreen.kt, ViewModel - Business configs & database backup)
```

---

## 💾 Core Modules & Database Entities

### 1. Store Setup Wizard & Customization
- **First Launch Intercept:** Automatically detects if a `StoreProfile` exists. If not, blocks layout navigation and displays the Setup Wizard.
- **Store Configuration:** Saves shop name, address, contact phone, default currency symbol, and optional sales tax percentage.
- **Security:** Requires a 4-digit Administrator PIN to lock settings, financial reports, and database restores.
- **Store Branding:** The custom logo path and shop name are automatically injected on the main dashboard, receipts, repair tickets, and report layouts.

### 2. POS / Sales Transactions
- **Cart Management:** Support search by name or typing of SKU/barcodes. Includes custom quantity adjustments with high-contrast buttons and a custom unit discount modifier.
- **Client Linkage:** Lets clerks assign walk-in status or choose registered accounts from a scrollable client selector dialog.
- **Auto-Calculations:** Calculates running totals (Subtotal, total applied discounts, and final balance) in real-time.
- **Invoice Receipts:** Displays a virtual receipt summary dialog containing payment modes, transaction ID, custom shop header, and quick-action links to share or copy invoice details.

### 3. Inventory Management
- **Add, Edit, Delete:** Full support to track cost prices, retail price, and stock quantities.
- **Repair Parts:** Flags items with a "Part" badge if they are inventory elements (e.g., iPhone screens, batteries) to keep them clean.
- **Stock Log history:** Logs every restock, retail sale checkout, and repair part allocation automatically inside `StockHistory`.
- **Low Stock warning:** Highlights products in warning-red with a warning icon if they fall below the configured threshold.

### 4. Phone Repair Tickets
- **Fields:** Customer profiles, brand, model, IMEI/Serial, Lock pattern/password, condition notes, fault description, estimated costs, and technician logs.
- **Device Status Workflow:** `Received` ➡️ `Diagnosing` ➡️ `Waiting for parts` ➡️ `Repairing` ➡️ `Completed` ➡️ `Collected`.
- **Parts Deductions:** Linked directly to the inventory database. When parts are assigned to a ticket, they auto-calculate their cost prices, adjust final estimated balances, and deduct product stock while logging to `StockHistory`. Removing a part refunds stock automatically.
- **Balance Settle:** Supports partial repair deposits and triggers the status to `Collected` when the client settles the balance.

### 5. Financial Reports & Outgoing Expenses
- **Profits Estimate Math:** Calculated dynamically to protect margins:
  $$\text{Net Shop Profit} = (\text{Retail POS Sales} - \text{POS Products Cost Price}) + (\text{Repair Labor Fees}) + (\text{Repair Parts Retail Markup}) - (\text{Operational Expenses})$$
- **Expense Logger:** Categorized operational outgoings (Rent, supplies, electricity) to ensure balance sheets remain accurate.
- **Best Sellers:** Renders a list of the top 10 products sorted by sales volumes.

### 6. Local Backup & Restore System
- **Location:** Saves offline to `Android/data/com.mobilehub.pos/files/MobileHubPOS_Backup.json`.
- **Method:** Serializes and deserializes all 13 database tables into a single formatted JSON configuration structure.
- **Portability:** Users can copy this JSON backup file to a new device's data path, click "Restore Backup", enter their PIN, and instantly import their entire database of clients, sales history, and repairs offline!

---

## 🛠️ How to Import and Build the Project
1. Copy the `MobileHubPOS` folder to your workspace.
2. Launch **Android Studio** and choose **Open an Existing Project**, pointing it to the root of the `MobileHubPOS` folder.
3. Android Studio will automatically resolve Gradle dependencies defined in the modern `gradle/libs.versions.toml` catalog file.
4. Connect your Android phone or Galaxy Tab via USB debugging, select your target device, and click **Run**.
