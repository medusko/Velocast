# Changelog

All notable changes to **Velocast** are documented in this file.

## [android-v1.0.1-Beta] - 2026-11-08

### Added & Improved
- **Live Camera Snapshots & Direct Unique Stream Links:** Verified working Caltrans D4 CCTV snapshot feeds (`cwwp2.dot.ca.gov`) and Wikimedia Geosearch coordinate-matched camera feeds; added inline Live Stream Player toggle and ensured "Open Direct Stream" opens the unique camera stream URL directly.
- **Top 50 US Suburbs Cycling Database (150 Places) & Startup GPS Resolution:** Requests location permission on launch to automatically populate the 3 most popular cycling places for the user's nearest US suburb, or defaults to **Skyline Blvd • Old La Honda** and **Marin Headlands • Hawk Hill** when outside those 50 metro suburbs.
- **Country Labels on All Places:** All cycling places and geocoded results now include the country (e.g., `Sausalito, CA, USA`).
- **Native OpenFreeMap & Map Style Selector (`Regular`, `Street`, `Topograph`):** Replaced WebView MapLibre with native high-FPS tile rendering for both `OpenFreeMap` and `OSMDroid`, expanded the map vertically by 25%, added `Regular` / `Street` / `Topograph` style switching, replaced legacy pins with custom vector **Bike** and **Camera** icons, and upgraded the map tap popup to a modern solid-color card.
- **20-Point Route Cast + Reordering + Swipe-to-Delete:** Expanded Plan a Ride from 5 to 20 points, added Up/Down reordering buttons, added swipe-to-delete on route waypoints, and added an optional confirmation toggle in Options (default: off).
- **Custom Date Picker & Two-Row Controls:** Added a `Custom Date` picker button next to `Tomorrow`, moved the speed selector to the next row with an arrival-time weather calculation note, and placed the `Save Ride` button inline beside a 1-line ride name input with a default name suggestion.
- **Independent Granular Units & Dynamic Info Resolution:** Independent Imperial/Metric selectors for Temperature (`°F`/`°C`), Speed (`mph`/`km/h`), Distance (`mi`/`km`), and Climb/Elevation (`ft`/`m`), with dynamic `mi`/`km` model resolution formatting in Info and Options.
- **24-Hour Weather Timeline & Custom Sunrise/Sunset Vectors:** Expanded the hourly cycling forecast timeline from 12 to 24 hours, removed all emojis across the UI, and rendered custom adaptive `Canvas` vector graphics for Sunrise and Sunset.
- **Saved Rides Enhancements:** Kept the 2 Bay Area default rides (removed PCH/Latigo Canyon), added a closest-place subtext below each saved ride title, added a delete confirmation dialog, and trimmed top header spacing.
- **BETA Launcher Icon Overlay:** Added an orange `BETA` badge overlay to the launcher icon and removed the icon picker option.

## [android-v1.0.0-Beta] - 2026-11-08

### Added
- **Saved Cycling Places Tab:** View temperature, wind speed/gusts/direction, chance of rain, UV index, sunrise/sunset, closest weather station, and 12-hour timeline for favorite cycling locations.
- **Plan a Ride Tab (5-Point Route Cast):** Interactive map with tap-to-add waypoints (up to 5 points) and geocoded address search, route geometry, and headwind/tailwind/crosswind vector math.
- **Saved Rides Tab:** Persistent catalog of multi-point rides displaying live forecasted temperature range per ride, wind summary, rain probability, and UV exposure.
- **Public Camera Feeds:** Integrated live roadside and mountain pass camera viewer supporting **Windy Webcams API** (default) and **State & Regional DOT 511 Feeds**.
- **Configurable Providers (Options Tab):**
  - Mapping: **OpenFreeMap (MapLibre Vector)** (default) & **OSMDroid (OpenStreetMap)**.
  - Weather & Microclimates: **Open-Meteo** (default), **NOAA / NWS Station API**, **WeatherAPI.com**, and **OpenWeatherMap**.
  - Selectable Launcher Icons: **Medusko Gold Contour** (default) & **Classic Neon Cyan** matching Keyboardy and Lagatron.
