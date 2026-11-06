# NOAA-Tides-Android

A native Android app for viewing a week of NOAA tide predictions in a clean, high-contrast dashboard. It is the Android companion to the [NOAA-Tides web app](https://github.com/cs669/NOAA-Tides) and shows the same data.

## Features

- Searches the full NOAA tide station catalog by city, station name, state, or station id
- Remembers recently selected stations for quick reuse
- Fetches seven days of NOAA CO-OPS tide predictions (MLLW datum) for the chosen station
- Interactive weekly tide curve chart with touch inspection
- Summary cards for current tide height, station info, forecast span, and last update time
- Day-by-day forecast cards with the daily high, low, and each tide event
- Clear error banner with a Retry button when the network or NOAA API is unavailable

## Tech stack

- Kotlin 2.0 and Jetpack Compose (Material 3)
- AndroidX ViewModel and Kotlin coroutines
- Android Gradle Plugin 8.7, Gradle 8.13 (wrapper included)
- Min SDK 26 (Android 8.0), target and compile SDK 35
- No API key needed: data comes from the public NOAA endpoints
  - Predictions: `https://api.tidesandcurrents.noaa.gov/api/prod/datagetter`
  - Station catalog: `https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations.json`

## Getting started

### Android Studio

1. Clone the repo:
   ```bash
   git clone https://github.com/cs669/NOAA-Tides-Android.git
   ```
2. In Android Studio, choose **Open** and select the cloned folder.
3. Let Gradle sync. Android Studio creates `local.properties` with your SDK path automatically.
4. Pick a device or emulator running API 26 or newer and click **Run**.

### Command line

Requires JDK 17 and the Android SDK (`ANDROID_HOME` set or `local.properties` present).

```bash
./gradlew assembleDebug        # build the debug APK (app/build/outputs/apk/debug/)
./gradlew installDebug         # install on a connected device
./gradlew test                 # run unit tests
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Project structure

```
app/src/main/java/gov/noaa/tides/
├── MainActivity.kt                 # entry point, hosts the Compose UI
├── data/
│   ├── NoaaApiService.kt           # NOAA station catalog and prediction requests
│   ├── TideRepository.kt           # caching and data access for the UI
│   └── RecentStationsStore.kt      # persists recently selected stations
├── model/
│   └── TideModels.kt               # stations, tide points, daily summaries, formatters
└── ui/
    ├── TideDashboardScreen.kt      # main dashboard, station search sheet, cards
    ├── TideViewModel.kt            # screen state, search, and refresh logic
    ├── components/TideChart.kt     # interactive weekly tide curve
    └── theme/Theme.kt              # colors and Material theme
app/src/test/java/gov/noaa/tides/
└── TideLogicTest.kt                # unit tests for tide calculations
```

## Data source

Tide predictions and station metadata come from the [NOAA CO-OPS Tides & Currents API](https://api.tidesandcurrents.noaa.gov/api/prod/). Predictions are forecasts, not observed water levels, and should not be used for navigation.
