# CineStream Android Client

This is the native Android frontend for CineStream, built to explore Server-Driven UI (SDUI) for my thesis.

Instead of hardcoding screens and layouts directly in the Android code, this app acts as a rendering engine. It fetches JSON blueprints from a custom backend and dynamically maps them to Jetpack Compose components. All layout decisions, navigation paths, and content logic are handled by the server.

## Tech Stack

### Tech Stack
- Kotlin
- Jetpack Compose (Material 3)
- Dagger Hilt
- Jetpack Compose
- Dagger Hilt for Dependency Injection
- Kotlin Coroutines & Flow
- Coil
- Retrofit & kotlinx.serialization

## Running the App

1. Clone the repository and open it in Android Studio.
2. Sync the project with Gradle.
3. Select the `app` run configuration and hit Run on an emulator or physical device.

*(Minimum SDK: 26, Target SDK: 36)*

## The Backend (BFF)
- Coil for image loading

This client requires the Backend-for-Frontend (BFF) server to function. The backend dictates exactly what the app should display.
### Project Structure
- `app/` - Main entry point and application wiring
- `core/sdui/` - SDUI parsing and rendering engine
- `core/network/` - API clients and data layer
- `core/designsystems/` - Reusable Compose components and theme
- `feature/*/` - Isolated feature modules (home, search, movie details, list)

The backend is built with TypeScript and deployed on Vercel. 
Repository: [cinestream-sdui-backend](https://github.com/AmmarAlGhifary/cinestream-sdui-backend)

**Local Development:**
To run the backend locally and test real-time UI changes, update the Retrofit Base URL in `NetworkModule.kt` to point to the emulator's localhost alias:
```kotlin
.baseUrl("http://10.0.2.2:3000/")
```

Make sure to temporarily enable `android:usesCleartextTraffic="true"` in your `AndroidManifest.xml`. Once connected, you can change the server code, save, and swipe-to-refresh the app to see instant UI changes.
