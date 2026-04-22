# CineStream Android

An Android application that uses a Server-Driven UI (SDUI) approach to render its interface dynamically. 

## Project Overview

CineStream is structured as a multi-module project to separate core infrastructure from feature implementation. The main focus is on the `core:sdui` module, which handles parsing server responses into native Jetpack Compose UI components.

### Tech Stack
- Kotlin
- Jetpack Compose
- Dagger Hilt for Dependency Injection
- Kotlin Coroutines & Flow
- Coil for image loading

### Project Structure
- `app/` - Main entry point and application wiring
- `core/sdui/` - SDUI parsing and rendering engine
- `core/network/` - API clients and data layer
- `core/designsystems/` - Reusable Compose components and theme
- `feature/*/` - Isolated feature modules (home, search, movie details, list)

## Setup

1. Open the project in Android Studio.
2. Sync the project with Gradle files.
3. Select the `app` configuration and run.

Minimum SDK: 26
Target SDK: 36
