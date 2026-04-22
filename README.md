# CineStream

A full-stack project demonstrating a Server-Driven UI (SDUI) architecture. The project consists of a native Android application and a dedicated Backend-for-Frontend (BFF) that dictates the UI layout, navigation, and content dynamically.

## Architecture Overview

CineStream shifts the responsibility of UI structure from the client to the server. The Android app acts as a "dumb" rendering engine that parses JSON blueprints and maps them to Jetpack Compose components. The Vercel-hosted BFF handles all business logic, calls the TMDB API for movie data, and constructs the exact UI hierarchy the phone should display.

---

## 📱 Android Client (Frontend)

The Android application is structured as a multi-module project to strictly separate the SDUI rendering engine from feature implementation.

### Tech Stack
* **Kotlin**
* **Jetpack Compose** (Material 3)
* **Dagger Hilt** (Dependency Injection)
* **Kotlin Coroutines & Flow** (Asynchronous state management)
* **Coil** (Image loading)
* **Retrofit & kotlinx.serialization** (Network and JSON parsing)

### Project Structure
* `app/` - Main entry point, NavHost, and application wiring.
* `core/sdui/` - The core SDUI parsing and rendering engine (`UiComponentRenderer`).
* `core/network/` - API clients and data layer.
* `core/designsystems/` - Reusable Compose components and theme configuration.
* `feature/*/` - Isolated feature modules (`home`, `search`, `detail`, `listmovie`).

### Setup & Installation
1. Open the project in Android Studio.
2. Sync the project with Gradle files.
3. Select the `app` configuration and run on an emulator or physical device.

*(Note: Minimum SDK is 26, Target SDK is 36).*

---

## ⚙️ Backend-for-Frontend (BFF)

**Repository:** [cinestream-sdui-backend](https://github.com/AmmarAlGhifary/cinestream-sdui-backend)

The BFF acts as the middleware between the Android app and the external TMDB API. It translates raw movie data into structured SDUI JSON blueprints.

### Tech Stack
* **TypeScript / Node.js**
* **Vercel** (Serverless Functions)
* **Axios** (Data fetching from TMDB)

### Core Endpoints
* `/api/home` - Generates the main dashboard (Hero banner, Trending carousel, Upcoming carousel).
* `/api/movie_detail_screen?movie_id={id}` - Generates the layout for a specific movie's details.
* `/api/movie_list_screen?list_type={type}` - Returns a `vertical_list` blueprint for categories like "trending" or "upcoming".
* `/api/search?query={text}` - Queries TMDB and returns a `vertical_list` of matching results.

### Setup & Local Development
To enable rapid, real-time SDUI editing without waiting for cloud deployments, you can run the BFF locally and connect your Android emulator directly to it.

1. Clone the backend repository.
2. Install dependencies: `npm install`
3. Start the local Vercel development server:
   ```bash
   vercel dev
   ```
4. **Android Emulator Connection:** In the Android project's `NetworkModule`, point your Retrofit Base URL to the emulator's localhost alias:
   ```kotlin
   .baseUrl("http://10.0.2.2:3000/")
   ```
5. Ensure `android:usesCleartextTraffic="true"` is temporarily enabled in your `AndroidManifest.xml` to allow local HTTP traffic during development.
6. Make changes to your TypeScript files, hit save, and simply "swipe to refresh" on the Android emulator to see instant UI updates.

### Deployment
To push layout updates to production instantly:
```bash
vercel --prod
```
