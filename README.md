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
*   `app/` - Main entry point and application wiring.
*   `core/sdui/` - The brain of the client. Holds the JSON parsers, models, and the `UiComponentRenderer` that translates server instructions into actual UI.
*   `core/network/` - API clients and data layer.
*   `core/designsystems/` - Reusable Compose components and theming.
*   `feature/*/` - Isolated, super-thin feature modules (home, search, details). They mostly just pass network responses straight to the SDUI renderer.

## The Backend (BFF)
Because this is an SDUI app, it relies entirely on its Backend-for-Frontend (BFF) server to function. The backend is built with TypeScript and deployed on Vercel.
Repository: [cinestream-sdui-backend](https://github.com/AmmarAlGhifary/cinestream-sdui-backend)

**Local Development Tip:**
If you want to run the backend locally to test real-time UI changes, update the Retrofit Base URL in `NetworkModule.kt` to point to the emulator's localhost alias:
```kotlin
.baseUrl("http://10.0.2.2:3000/")
```

## Running the App
1. Clone the repo and open it in Android Studio.
2. Let Gradle sync.
3. Select the `app` run configuration and hit Run.

*(Note: Minimum SDK is 26, Target SDK is 36)*