# TrackIt Side Hustle Earnings Module

## Purpose

Add a dedicated, offline-first workspace for tracking income-generating side hustles without mixing hustle records into TrackIt's personal transactions. Users can create and rename their own hustle types at any time.

## Navigation and screen structure

Add a **Hustles** destination to the main app navigation. It opens a separate workspace with four sections:

1. **Overview** — selected date range, total earnings, hustle expenses, net profit, outstanding customer balances, and recent activity.
2. **My hustles** — dynamic list of user-created hustles. Each hustle has its own detail view and filtered records.
3. **Records** — create, review, edit, and delete earnings, expenses, and debt records.
4. **Reports** — date-filtered performance, profit by hustle, earnings/expense trends, and outstanding balances.

Keep the existing personal Home, Activity, Budgets, Reports, Money, Profile, and Backup & Restore flows intact. Hustle reports must use only hustle-module records.

## Data model

Use the existing Room database and money convention (Long minor units; KES display formatting). Add independent entities rather than reusing MoneyTransaction, because that entity feeds personal finance totals and budgets.

### Hustle

- id: generated local primary key
- name: required user-defined name
- description: optional
- active: allows archiving without deleting historical records
- createdAtEpochMillis, updatedAtEpochMillis

Names should be trimmed and case-insensitively unique. Deleting a hustle with history should be disallowed or converted to archive; never silently orphan its records.

### Hustle transaction

- id, hustleId
- type: INCOME or EXPENSE
- amountMinor: positive integer minor units
- category: user-editable label (for example, website development, typing, transport, data bundles, materials)
- description, paymentMethod
- dateEpochMillis, createdAtEpochMillis, updatedAtEpochMillis

An income is recognized as received money. Expenses are costs attributable to a hustle. Net profit for a period is income minus expenses for that period. Transfers to personal finances are not income a second time; a future explicit transfer workflow can link both sides.

### Hustle debt

- id, hustleId
- direction: CUSTOMER_OWES_ME or I_OWE
- counterparty: customer/supplier name
- description
- originalAmountMinor, settledAmountMinor
- dueDateEpochMillis (optional), createdAtEpochMillis, updatedAtEpochMillis
- status derived from outstanding balance (original - settled), with validation to prevent over-settlement

Payments/collections should be recorded against the debt with dated settlement entries in a follow-up implementation stage, rather than merely overwriting a balance. Outstanding balance is original amount minus recorded settlements.

## Main user flows

### Create a hustle
User taps Add hustle, enters a name and optional description, and saves. The new hustle appears immediately and can be used for new records. Support any name; do not hard-code a list of hustle types.

### Record earnings or expenses
User selects a hustle, chooses Earning or Expense, enters amount, date, category/service, payment method, and optional note, then saves. Require a valid positive amount and a selected active hustle. Records remain local and available offline.

### Track debts
User selects a hustle and records who owes them or whom they owe, the original amount, optional due date, and notes. Show outstanding balance and status. Settlement history should be represented explicitly so partial payments remain auditable.

### Reports
Allow Today, This week, This month, and All time filters. Show:
- earnings, expenses, and net profit;
- breakdown by hustle and category;
- record history for the selected period;
- total customer balances outstanding and amounts the user owes;
- clear empty states when no records exist.

All summaries must be derived from hustle-specific entities, never from personal transactions.

## Persistence, migrations, and backup

- Add Room entities and DAOs; increment the Room schema from version 9 to 10 with a non-destructive MIGRATION_9_10 that creates new tables and indexes. Do not use destructive migration.
- Keep repository/view-model state flow consistent with existing Room patterns.
- Extend TrackItBackupManager to export the new hustle tables.
- Preserve compatibility with existing format-version-1 backups: newly introduced tables must be optional when restoring older backups, while malformed known data must still fail validation before replacing current rows.
- Restore in dependency-safe order and include the new tables in the same SQLite transaction. Existing records must not be lost because a legacy backup predates hustles.
- No network permission, account integration, new database, or Gradle dependency is needed for this offline-first first version.

## Acceptance criteria

1. A user can create multiple custom-named hustles and archive one without losing history.
2. Earnings, expenses, and debts are stored independently from personal records and persist across app restarts.
3. A hustle detail view shows only that hustle's records.
4. Totals and reports calculate from selected dates and selected hustle scope; net profit equals earnings minus expenses.
5. Partial debt settlements preserve their history and correctly update outstanding balances.
6. Existing personal dashboards, budgets, reports, savings/debts, and backup/restore continue working.
7. A backup made after the change includes hustle data; an older backup remains restorable without creating fake hustle data.
8. Verify with a build and focused tests for migration, calculations, debt settlement, and legacy backup restore. If Gradle/device execution is unavailable, report that clearly rather than claiming verification.

## Recommended implementation sequence

1. Room entities, DAOs, repositories, migration, and backup compatibility.
2. ViewModel state and validated operations.
3. Hustle overview/list and create/archive flow.
4. Earnings/expense entry and history.
5. Debt tracking with settlement history.
6. Reports and tests, then build verification.

This design deliberately keeps the module separate from personal budgeting while using the same local database, money representation, theme, and user-controlled backup mechanism.
