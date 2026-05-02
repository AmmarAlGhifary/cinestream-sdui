# CineStream Android Client

This is the native Android frontend for CineStream, built to explore Server-Driven UI (SDUI) for my thesis.

Instead of hardcoding screens and layouts, this app is essentially a flexible rendering engine. It fetches JSON blueprints from a custom backend and translates them into Jetpack Compose components on the fly. All layout decisions, navigation paths, and content logic are entirely driven by the server.

## Tech Stack
*   **Kotlin & Jetpack Compose** (Material 3)
*   **Dagger Hilt** (Dependency Injection)
*   **Coroutines & Flow**
*   **Retrofit & kotlinx.serialization** (Networking & parsing)
*   **Coil** (Image loading)

## Project Structure
The app uses a multi-module setup to strictly isolate the SDUI rendering engine from the rest of the infrastructure:
Refer to the [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)

## The Backend (BFF)
Because this is an SDUI app, it relies entirely on its Backend-for-Frontend (BFF) server to function. The backend is built with TypeScript and deployed on Vercel.
Repository: [cinestream-sdui-backend](https://github.com/AmmarAlGhifary/cinestream-sdui-backend)

**Local Development Tip:**
If you want to run the backend locally to test real-time UI changes, change the Retrofit Base URL in `gradle.properties` to point to the emulator's localhost alias:

```kotlin
.baseUrl("http://10.0.2.2:3000/")
```

Make sure to temporarily enable `android:usesCleartextTraffic="true"` in your `AndroidManifest.xml` so Android allows the local HTTP connection. Once connected, you can change the server code, hit save, and swipe to refresh the app to see instant UI changes.
