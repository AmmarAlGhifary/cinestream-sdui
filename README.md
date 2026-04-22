# CineStream Android Client

This is the native Android frontend for CineStream, an app I built to explore Server-Driven UI (SDUI) for my thesis. 

Instead of hardcoding screens and layouts directly in the Android code, this app acts as a dumb rendering engine. It fetches JSON blueprints from a custom backend and dynamically maps them to Jetpack Compose components. All the layout decisions, navigation paths, and content-heavy lifting are handled by the server.

## Tech Stack

- Kotlin
- Jetpack Compose (Material 3)
- Dagger Hilt (Dependency Injection)
- Kotlin Coroutines & Flow
- Coil (Image loading)
- Retrofit & kotlinx.serialization

## Project Structure

I set this up as a multi-module project to strictly isolate the SDUI rendering engine from the rest of the app infrastructure.

- `app/` - The main entry point, navigation host, and application wiring.
- `core/sdui/` - The brain of the client. This contains the JSON parsers, models, and the `UiComponentRenderer` that translates server instructions into actual Compose UI.
- `core/network/` - API clients and data layer.
- `core/designsystems/` - Reusable Compose components and theme configuration.
- `feature/*/` - Isolated feature modules for the screens (home, search, detail, movie list). These are very thin, as they mostly just pass the network response to the SDUI renderer.

## Running the App

1. Clone the repository and open it in Android Studio.
2. Allow the project to sync with Gradle.
3. Select the `app` run configuration and hit Run on an emulator or physical device.

Note: Minimum SDK is 26, Target SDK is 36.

## The Backend (BFF)

This client requires the Backend-for-Frontend (BFF) server to function, as the server dictates exactly what the app should display. 

The backend is built with TypeScript and deployed on Vercel. You can find the repository here: [cinestream-sdui-backend](https://github.com/AmmarAlGhifary/cinestream-sdui-backend)

**Local Development Tip:**
If you want to run the backend locally to test real-time UI changes (skipping the cloud deployment wait time), change the Retrofit Base URL in `NetworkModule.kt` to point to the emulator's localhost alias:

```kotlin
.baseUrl("http://10.0.2.2:3000/")
```

Make sure to temporarily enable `android:usesCleartextTraffic="true"` in your `AndroidManifest.xml` so Android allows the local HTTP connection. Once connected, you can change the server code, hit save, and just swipe-to-refresh the app to see instant UI changes.
