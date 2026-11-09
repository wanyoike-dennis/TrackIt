# TrackIt — Personal Finance Tracker

TrackIt is an offline-first Android application for recording income and expenses, managing budgets, keeping track of savings and debts, and understanding day-to-day cash flow.

Built with Kotlin and Jetpack Compose, TrackIt keeps financial records on the device and provides practical tools to review and manage personal finances.

## What TrackIt includes

### Dashboard
- View an overview of income, spending, savings, debts, and available balance.
- Review recent transactions and weekly cash flow.
- See current-month totals, spending projections, and budget alerts.
- View upcoming financial commitments.

### Income and expense tracking
- Record income and expenses with amounts, dates, categories, and descriptions.
- Associate transactions with a financial account.
- Review transaction activity and history.
- Track records locally without requiring an internet connection.

### Budgets
- Create category-based monthly budgets.
- Choose one-time or recurring budget behaviour.
- Monitor spending against budget limits.
- Receive alerts when monthly or recurring budget limits are exceeded.
- Use budget guidance to help plan spending throughout the month.

### Accounts and money management
- Keep multiple financial accounts and opening balances.
- Record transfers between accounts.
- Review account transaction activity.
- Reconcile account balances against external statements.
- Archive accounts when they are no longer in use.

### Savings and debts
- Create savings goals and record contributions.
- Track flexible savings balances.
- Record money owed and money owed to you.
- Track repayments, collections, and outstanding balances.
- Keep due dates and notes for financial records where applicable.

### Recurring transactions and commitments
- Set up recurring transaction records.
- Keep track of upcoming financial commitments and due dates.

### Reports and financial review
- Review income and expenses over time.
- Compare spending by category.
- Use dashboard cash-flow summaries and monthly figures to understand financial activity.

### Daily reminders and notifications
- Receive a daily reminder at **9:00 PM** to record the day's income and expenses.
- Receive budget notifications when configured spending limits are exceeded.
- Daily reminders use the device's local time and are rescheduled after device restarts or time-zone changes.
- Notification delivery depends on Android notification permissions and device battery/background restrictions.

### Backup and restore
- Export financial records to a portable TrackIt backup file.
- Restore records from a previously exported backup.
- Backups include financial data, personal profile information, and supported app settings.
- Backup files contain sensitive personal and financial information. Keep them in a secure location; exported backups are not encrypted.

### Personalisation
- Set up personal profile information.
- Choose light, dark, or system appearance.
- Use a consistent interface with Lucide-style icons.

## Screenshots

Screenshots will be added as the app's presentation assets are prepared.

## Technology

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose |
| Design system | Material 3 |
| Local database | Room |
| Database code generation | KSP |
| Architecture | ViewModel and repository-based structure |
| Android notifications | NotificationCompat and AlarmManager |

TrackIt currently uses a local Room database. Core financial records are stored on the device; an internet connection is not required for everyday recording and review.

## Requirements

- Android Studio with an Android SDK compatible with the project configuration.
- JDK compatible with the configured Gradle and Android Gradle Plugin versions.
- An Android device or emulator.

The project currently targets Android SDK 37 and supports Android 7.0 (API 24) and later.

## Getting started

1. Clone this repository.
2. Open the project root in Android Studio.
3. Allow Gradle sync to complete.
4. Select the `app` run configuration.
5. Run the app on an emulator or connected Android device.

### Build a debug APK

On Windows Command Prompt:

```bat
gradlew.bat assembleDebug
```

On macOS or Linux:

```bash
./gradlew assembleDebug
```

The debug APK is normally generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Notifications

Android 13 and later require notification permission. TrackIt requests this permission so it can show budget alerts and the daily recording reminder.

The daily reminder is scheduled for 9:00 PM using the device's local time. Android may delay inexact alarms while applying battery and background-use restrictions, so notification delivery is not guaranteed to occur at the exact minute on every device.

## Project structure

```
app/
└── src/
    └── main/
        ├── java/com/inventsolutions/trackit/
        │   ├── data/
        │   │   ├── local/          # Room entities, DAOs, database and backup
        │   │   └── repository/     # Data repositories
        │   ├── notifications/      # Budget alerts and daily reminder
        │   ├── ui/
        │   │   ├── components/    # Shared UI components and icons
        │   │   ├── screens/       # App screens
        │   │   ├── theme/         # App theme and colour definitions
        │   │   └── viewmodel/     # UI state and application logic
        │   └── MainActivity.kt
        └── res/                    # Android resources and launcher assets
```

## Data and privacy

- Financial records are stored locally in the app's Room database.
- TrackIt does not require an account or cloud connection for its core tracking features.
- Exported backup files include sensitive information and should be protected.
- Uninstalling the app or clearing its data may remove locally stored records. Export a backup before changing devices or reinstalling.

## Current development status

TrackIt is actively being developed. The repository contains implemented transaction, budgeting, account, savings, debt, reporting, profile, backup/restore, and notification functionality. Some areas may continue to evolve as the app is tested and refined.

Please report bugs with the device/Android version, steps to reproduce, and any relevant screenshots or error messages.

## Disclaimer

TrackIt is a personal finance tracking and budgeting tool. Its summaries and projections are intended to support personal money management and are not professional financial, tax, legal, or investment advice.

---

**TrackIt — know what comes in, understand what goes out, and plan what stays.**
