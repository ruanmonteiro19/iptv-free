# TekasTV TV 0.3.6 — Master Refactor

This refactor is isolated to the `tvapp` module. The mobile `app` module is intentionally untouched.

## Inventory

- Android TV UI: Compose + TV focus helpers
- Playback: AndroidX Media3 / ExoPlayer
- Local data: Room database (`beiratv_database`)
- Playlist parser: `M3uParser`
- EPG parser: `XmltvParser`
- Network: OkHttp
- Images: Coil
- Preferences: DataStore for theme/playback preferences
- Existing favorites/history preserved through a Room 1→2 migration

## Implemented phases

1. Built-in hidden source model + 12-hour cache policy
2. M3U parsing off the main thread
3. Canonical channel normalization and deduplication
4. Multiple stream backups per canonical channel
5. Remote aliases/logos catalog support + visual placeholder
6. Standard categories and regional metadata/state filters
7. Accent-insensitive global search independent of category filters
8. More compact adaptive TV home/grid for 720p, 1080p and 4K classes
9. Light / Dark / AMOLED themes persisted with DataStore
10. Android TV DPAD/Enter behavior, including explicit search keyboard activation
11. Android 10+ (`minSdk 29`) with target/compile API 36
12. Unit tests for core normalization/deduplication rules

## Safety / source policy

- Built-in sources are not exposed in Settings.
- Unverified built-in sources are filtered for known subscription/premium channel names.
- No credentials, tokens, scraped IPTV accounts or invented stream URLs are added.
- Source failures preserve the last usable local grade when cache exists.
- Logs contain source identifiers and counts, not full stream URLs.

## Data model

The existing `ChannelEntity` remains as the UI-facing canonical channel row for migration compatibility. New tables support:

- `channel_sources`
- `channel_streams`
- `channel_aliases`
- `channel_logos`
- `channel_metadata`
- `favorites`
- `source_sync`

A single canonical channel can therefore have multiple prioritized streams while appearing only once in the UI.

## Build validation

The source-level normalizer/parser smoke tests were executed in the authoring environment. A full Android build still needs the repository GitHub Actions/Android SDK environment (`:tvapp:assembleDebug`).
