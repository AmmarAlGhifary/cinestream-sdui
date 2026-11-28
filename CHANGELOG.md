# Changelog

All notable changes to the CineStream Android client are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [2.0.0] - 2026-11-28

### Added

- Failed loads now open a dialog with a plain-language title and message, and offer Retry when trying again can help.
- Error dialogs show the server's own explanation and requested waiting time when it sends them.
- Every pull request is now built and unit-tested automatically, including the release variant.
- Tests for screen decoding, rendering, error handling and navigation.
- Favorites: the heart on a movie's detail page saves it to, or removes it from, your TMDB account's favorites.
- Sign in with TMDB on its own page in a browser tab, so the app never sees your password.
- When you are signed in, an account icon on the home screen shows who you are and offers Sign out.
- **For maintainers:** release builds can be signed using a properties file kept outside the repository (see the README).

### Changed

- The server-driven UI engine is rebuilt on [Blueprint](https://github.com/AmmarAlGhifary/blueprint) 0.4.0, using the same backend field names as before.
- A component the app cannot recognise or read is now left out on its own instead of failing the whole screen.
- Search queries are limited to 200 characters.
- **Breaking for developers:** building the app now requires Kotlin 2.3 or newer (Kotlin 2.3.21, KSP 2.3.9, Hilt 2.58).
- **For developers:** debug builds now default to a backend on your own machine and release builds to production; `-PBASE_URL` overrides either.
- The README and project structure guide are expanded and brought up to date.

### Removed

- The classic comparison app and its feature modules.

### Fixed

- A screen that failed to load can now be retried, and on home, detail and movie lists pulling to refresh works too.
- A failed refresh keeps the content already on screen.
- Opening a movie list that has no movie ID no longer sends the text "null" to the server.
- Server layouts that nest columns or lists inside a scrolling area no longer crash the app.

### Security

- Release builds use the production server over HTTPS only, and cannot be built with an unencrypted server address.
- The sign-in session is stored encrypted with an Android Keystore key, left out of backups and device transfer, sent only to the CineStream server and never written to logs.
- A sign-in link is accepted only for a sign-in the app itself started, and completing it requires a one-time secret.
- Error messages in release builds no longer show exception text, server addresses or URLs.
- Release builds no longer log network requests or responses.
- Taps and actions no longer write their contents or destinations to the device log.
- The client no longer contains or needs a TMDB API key.

[Unreleased]: https://github.com/AmmarAlGhifary/cinestream-sdui/compare/v2.0.0...development
[2.0.0]: https://github.com/AmmarAlGhifary/cinestream-sdui/releases/tag/v2.0.0
