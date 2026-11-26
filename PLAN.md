# Plan: Home Screen Date Mode (AD / BS / Off)

## Goal
Change the **Show date** setting under **Settings → Homescreen** from a binary `On` / `Off` toggle to a three-option selector: **AD**, **BS**, and **Off**.
- **AD:** Show the Gregorian date on the home screen (matching existing format `EEE, dd MMM`, e.g. `Thu, 26 Nov`).
- **BS:** Show the Nepali Bikram Sambat date on the home screen (e.g. `Thu, 10 Mangsir` or `बिहीबार, १० मंसिर` when system language is Nepali).
- **Off:** Hide the date view (`View.GONE`).

Existing clock behavior, date text sizing, alignment, and click-on-date action (`prefs.clickDateAction`) stay intact.

---

## Background / Current Behavior
- `app/src/main/res/layout/fragment_home.xml` uses a `TextClock` (`@+id/date`) with `format12Hour="EEE, dd MMM"` / `format24Hour="EEE, dd MMM"`. `TextClock` only supports Gregorian formatting and cannot display BS dates.
- `app/src/main/java/app/olaunchercf/data/Prefs.kt` stores `showDate: Boolean` under `SHOW_DATE` (default `true`).
- `app/src/main/java/app/olaunchercf/MainViewModel.kt` exposes `val showDate = MutableLiveData(prefs.showDate)` and `setShowDate(visibility: Boolean)`.
- `app/src/main/java/app/olaunchercf/ui/SettingsFragment.kt` renders `R.string.show_date` in the `homescreen` `SettingsArea` using `SettingsToggle`.
- `app/src/main/java/app/olaunchercf/ui/HomeFragment.kt` observes `showDate` to toggle `binding.date.visibility`.
- `app/src/androidTest/java/app/olaunchercf/SettingsTest.kt` has `hideDate()` asserting `!prefs.showDate` after clicking `R.string.show_date`.

---

## Decisions
1. **Setting options & UI:** Use the existing `SettingsItem` / `SettingsSelector` pattern in `SettingsFragment` (same as Theme mode, Language, and Alignments) with enum values **`AD`**, **`BS`**, **`Off`**.
2. **Default value:** `BS` on fresh installs.
3. **Backwards-compatible preference storage:** Store the new mode under a new key `DATE_MODE` (as enum name string). If `DATE_MODE` is not set yet, check legacy `SHOW_DATE` boolean if present (`true` → `AD` to preserve existing behavior, `false` → `Off`), and otherwise default to `BS` on fresh installs. This avoids `ClassCastException` on upgrades and when restoring older backup JSONs via `Prefs.loadFromString`.
4. **BS script/format:** Follow system locale (`ne` → Devanagari weekday, digits, and month name; all other locales → English transliteration `EEE, d MMM`, omitting the year).

---

## Implementation Steps

### 1. Add `Constants.DateMode` and strings
- **`app/src/main/java/app/olaunchercf/data/Constants.kt`**
  - Add `enum class DateMode : EnumOption { AD, BS, Off }` implementing `@Composable override fun string(): String` using `R.string.date_ad`, `R.string.date_bs`, and existing `R.string.off`.
- **`app/src/main/res/values/strings.xml`**
  - Add `<string name="date_ad">AD</string>` and `<string name="date_bs">BS</string>` (reuse existing `off`).

### 2. Update `Prefs` and `MainViewModel`
- **`app/src/main/java/app/olaunchercf/data/Prefs.kt`**
  - Add `private const val DATE_MODE = "DATE_MODE"` and `var dateMode: Constants.DateMode`:
    - Getter: if `DATE_MODE` exists, parse `Constants.DateMode.valueOf(...)` (with `try/catch` fallback to `BS`). Otherwise if legacy `SHOW_DATE` exists, migrate (`if (prefs.getBoolean(SHOW_DATE, true)) Constants.DateMode.AD else Constants.DateMode.Off`); otherwise return `Constants.DateMode.BS` on fresh installs.
    - Setter: write `DATE_MODE` as `value.name`.
- **`app/src/main/java/app/olaunchercf/MainViewModel.kt`**
  - Replace `showDate: MutableLiveData<Boolean>` / `setShowDate(Boolean)` with `val dateMode = MutableLiveData(prefs.dateMode)` and `fun setDateMode(mode: Constants.DateMode)`.

### 3. Update Settings UI
- **`app/src/main/java/app/olaunchercf/ui/SettingsFragment.kt`**
  - In the `homescreen` `SettingsArea`, replace the `SettingsToggle` for `R.string.show_date` with:
    - `SettingsItem` bound to `prefs.dateMode`, `values = Constants.DateMode.values()`, `active = currentSelection.value != Constants.DateMode.Off`, and `onSelect = { mode -> setDateMode(mode) }`.
  - Replace `toggleShowDate()` with `setDateMode(mode: Constants.DateMode)` updating `prefs.dateMode` and `viewModel.setDateMode(mode)`.

### 4. Add `NepaliDate` helper
- **`app/src/main/java/app/olaunchercf/helper/NepaliDate.kt`**
  - Self-contained AD → BS converter using the standard Bikram Sambat month-length lookup table (2000–2090+ BS, zero new dependencies).
  - Convert local calendar date (`java.util.Date` / `Calendar`) to BS `(year, month, day, dayOfWeek)` by counting days from a verified anchor date (e.g. `1943-04-14 AD = 2000-01-01 BS`).
  - Format as `EEE, d MMM` without the year (Devanagari when `Locale.getDefault().language == "ne"`, English transliteration otherwise).
  - Fallback gracefully to Gregorian `EEE, dd MMM` if a date falls outside the table range.

### 5. Replace date `TextClock` and render AD / BS / Off on Home Screen
- **`app/src/main/res/layout/fragment_home.xml`**
  - Replace the date `TextClock` (`@+id/date`) with a `TextView` keeping the same `id`, `style="@style/TextDefault"`, `paddingHorizontal="2dp"`, and `textSize="@dimen/date_size"`.
  - Keep the clock `TextClock` (`@+id/clock`) unchanged.
- **`app/src/main/java/app/olaunchercf/ui/HomeFragment.kt`**
  - Observe `viewModel.dateMode`:
    - `DateMode.Off` → `binding.date.visibility = View.GONE` and stop refresh updates.
    - `DateMode.AD` → `binding.date.visibility = View.VISIBLE`, set `binding.date.text` using `SimpleDateFormat("EEE, dd MMM", Locale.getDefault())`, and ensure refresh updates are active.
    - `DateMode.BS` → `binding.date.visibility = View.VISIBLE`, set `binding.date.text` using `NepaliDate.format(Date())`, and ensure refresh updates are active.
  - Refresh the date text in `onStart` / `onResume` and every minute via a `Handler(Looper.getMainLooper())` runnable (stopped in `onStop` / `onDestroyView`) so midnight rollovers and system date/timezone changes update promptly.
  - Keep `binding.date.textSize = prefs.textSize.toFloat()` and `binding.date.setOnClickListener(this)` unchanged.

### 6. Update & add tests
- **Unit tests** (e.g. `app/src/test/java/app/olaunchercf/helper/NepaliDateTest.kt`):
  - Verify known AD ↔ BS conversion dates (anchor, month boundaries, leap years, `ne` vs `en` formatting, out-of-range fallback).
- **Instrumented test** (`app/src/androidTest/java/app/olaunchercf/SettingsTest.kt`):
  - Update `hideDate()` (or replace with `testDateMode()`) to select `AD`, `BS`, and `Off` in Settings and assert `prefs.dateMode`.
  - Verify `Prefs.saveToString` / `loadFromString` round-trip and legacy `SHOW_DATE` boolean migration.

### 7. Verify & Commit
- Run `./gradlew testDebugUnitTest assembleDebug assembleRelease bundleRelease`.
- Install release APK on emulator (`emulator-5554`) and verify:
  - Switching **Show date** between **AD**, **BS**, and **Off** updates the home screen immediately.
  - Clock, text size, alignment, and click-on-date action work in both `AD` and `BS` modes.
- Commit changes once verified (keeping `release.properties` and `app/release.jks` untouched).

---

## Open Questions (confirm before implementation)
1. **Default for fresh installs:** Should the default date mode be **`AD`** (matches current behavior) or **`BS`**?
2. **Legacy `SHOW_DATE = true` migration:** Migrate `true` → **`AD`** (preserves existing user setup) or **`BS`**?
3. **BS display format:** Is `EEE, d MMM yyyy` (e.g. `Thu, 10 Mangsir 2083`, with year) good, or do you prefer `EEE, d MMM` without year?
