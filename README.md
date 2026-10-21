# TrackIt — Personal Income & Budget Tracker

**TrackIt** is an Android app in development to help you understand where your money comes from, where it goes, and how much you can realistically save each month. It is being designed especially for people managing a salary alongside side hustles and other irregular income.

The goal is to make everyday money tracking quick, useful, and reliable—even when you're offline.

> **Project status:** Early development. The repository currently contains the Android starter project and initial database dependency setup. Features described below are the product plan; they should not be assumed to be implemented yet.

## Why TrackIt?

Money can come in from several sources throughout the month, while expenses happen every day. TrackIt aims to bring those records together in one simple place so you can:

- Record income from a job, side hustle, business, or another source.
- Record everyday spending without needing an internet connection.
- Set a monthly budget for your whole spending plan, individual categories, or both.
- See whether your current spending pace is likely to take you over budget.
- Compare your planned savings with what you actually kept or set aside.
- Review a clear summary at the end of each month.

## Planned features

### Income and expense tracking
- Add income and expense entries with an amount, date, category, source, and optional notes.
- Use a quick-entry flow for everyday transactions, with extra details available when needed.
- Review transaction history and monthly totals.

### Flexible budgets
- Set an overall monthly spending budget.
- Set individual budgets for categories such as rent, food, transport, bills, and entertainment.
- Use either budgeting method, or both together.
- Get an advisory daily spending guide based on the monthly plan and remaining days.
- See warnings when spending is moving too quickly or a budget has been exceeded.

The daily guide is a planning aid, not a restriction on spending. Unused daily allowance does **not** roll over as a new allowance for the next day. Actual spending and the remaining monthly budget remain the basis for the month-to-date view.

### Savings and debts
Planned follow-on features include:
- Set a monthly savings target.
- Track money actually transferred or set aside as savings.
- Distinguish money left unspent from money deliberately saved.
- Record debts, repayments, and outstanding balances.

### Monthly insights
- Compare income and expenses for the selected month.
- Review budgeted spending against actual spending.
- See the difference between planned savings, money left over, and savings actually set aside.
- Identify categories that take up the largest share of spending.

### Offline-first and data portability
- Keep core records available locally on the device.
- Work without an internet connection.
- Plan for export and backup so records can be preserved outside the app.
- Consider optional multi-user support in a later phase; the initial focus is personal use.

## Technology

The project is an Android application built with:

- **Kotlin**
- **Jetpack Compose** and **Material 3** for the user interface
- **Room** for planned local persistence
- **KSP** for Room compiler/code generation
- **MVVM and repository patterns** as the intended structure for separating UI, business logic, and data access

The current project is an early scaffold. Architecture and dependencies may evolve as features are implemented and tested.

## Getting started

### Requirements
- Android Studio with a compatible Android SDK
- JDK compatible with the project's Gradle/Android Gradle Plugin configuration
- An Android emulator or Android device

### Open the project
1. Clone or download this repository.
2. Open the project root in Android Studio.
3. Allow Gradle sync to finish and install any SDK components Android Studio requests.
4. Run the `app` configuration on an emulator or connected device.

The project uses the Gradle wrapper, so prefer the included wrapper scripts rather than installing a separate Gradle version.

On Windows, the wrapper can be run from Command Prompt with:

```bat
gradlew.bat assembleDebug
```

On macOS or Linux:

```bash
./gradlew assembleDebug
```

These commands are intended as a build check; a successful build has not yet been confirmed for every environment.

## Development roadmap

The planned implementation order is:

1. **Foundation:** confirm the build, establish app structure, and configure local database components.
2. **Transactions:** create, edit, delete, and list income and expense records.
3. **Dashboard:** show income, expenses, and the current month's position.
4. **Budgets:** support overall and category budgets with daily guidance.
5. **Monthly reports:** compare plans with actual results and calculate leftover money.
6. **Savings and debts:** track savings targets, contributions, debts, and repayments.
7. **Backup and export:** let users preserve and export their data.
8. **Quality:** add tests, improve accessibility, and verify behaviour across screen sizes and offline scenarios.

Roadmap items may be adjusted as the app develops.

## Design principles

- **Fast to use:** everyday entries should take as few steps as practical.
- **Offline first:** recording and reviewing core financial data should not depend on a live connection.
- **Clear, not judgmental:** provide useful warnings and explanations rather than shame or pressure.
- **Honest summaries:** distinguish income, spending, unspent money, and savings actually set aside.
- **Privacy minded:** personal financial records deserve careful handling.
- **Built incrementally:** changes should be reviewed and tested before being treated as complete.

## Contributing and feedback

This project is currently being built for personal use, with the possibility of supporting more users later. Suggestions and issue reports are welcome. Please describe the problem, the expected behaviour, and the steps needed to reproduce it where applicable.

## Disclaimer

TrackIt is a personal money-tracking and budgeting tool in development. Its summaries are intended to support personal planning and are not professional financial, tax, or investment advice.

---

**TrackIt — know what comes in, understand what goes out, and plan what stays.**
