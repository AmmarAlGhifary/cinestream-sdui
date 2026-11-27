# CineStream Android Client

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green?logo=android)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-purple?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.11.2-blue?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Dagger Hilt](https://img.shields.io/badge/Dagger%20Hilt-2.58-orange)](https://dagger.dev/hilt/)
[![Backend](https://img.shields.io/badge/Backend-BFF%20Vercel-black?logo=vercel)](https://sdui-backend.vercel.app/)

The native Android frontend for **CineStream**, engineered to explore **Server-Driven UI (SDUI)** architectures.

Instead of hardcoding UI layouts, hierarchy, and navigation on the client, CineStream operates as a dynamic, declarative rendering engine. It fetches polymorphic JSON blueprints from a dedicated Backend-for-Frontend (BFF) and renders them natively using Jetpack Compose in real-time.

---

## 🔗 Project Linktree & Ecosystem

| Resource | Link / Reference | Description |
| :--- | :--- | :--- |
| **Android Client (This Repo)** | [AmmarAlGhifary/cinestream-sdui](https://github.com/AmmarAlGhifary/cinestream-sdui) | Native Android client with Jetpack Compose SDUI engine |
| **Backend BFF Repository** | [AmmarAlGhifary/cinestream-sdui-backend](https://github.com/AmmarAlGhifary/cinestream-sdui-backend) | Serverless BFF (TypeScript/Node.js) producing SDUI blueprints |
| **Live Production API** | [https://sdui-backend.vercel.app/](https://sdui-backend.vercel.app/) | Deployed production BFF on Vercel |
| **Architecture Specification** | [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) | In-depth breakdown of modules and package organization |
| **TMDB API** | [The Movie Database API v3](https://developer.themoviedb.org/) | External movie metadata, backdrops, and trending feeds |

---

## 🏛️ System Architecture

```mermaid
flowchart LR
    A["🎬 TMDB API<br/>(Movie Data Source)"] -->|REST / JSON| B["⚡ CineStream BFF<br/>(cinestream-sdui-backend)"]
    B -->|Polymorphic SDUI Blueprints| C["📱 CineStream Android Client<br/>(Jetpack Compose Engine)"]
    
    subgraph SDUI Android Client
        C1[":core:network<br/>Retrofit + Serialization"] --> C2[":core:sdui<br/>Parser & Component Registry"]
        C2 --> C3[":feature:*<br/>Declarative Compose Screens"]
    end
    
    C --- SDUI Android Client
```

---

## 📂 Android Modules Directory

### 🛠️ Core Infrastructure
| Module | GitHub Path | Purpose |
| :--- | :--- | :--- |
| **`:core:sdui`** | [`/core/sdui`](https://github.com/AmmarAlGhifary/cinestream-sdui/tree/development/core/sdui) | Dynamic SDUI Rendering Engine (JSON parser, component registries, domain models, Jetpack Compose renderers) |
| **`:core:network`** | [`/core/network`](https://github.com/AmmarAlGhifary/cinestream-sdui/tree/development/core/network) | Retrofit clients, serialization converters, OkHttp interceptors, and data sources |
| **`:core:designsystems`** | [`/core/designsystems`](https://github.com/AmmarAlGhifary/cinestream-sdui/tree/development/core/designsystems) | Design tokens, Compose Material 3 theme definitions, typography, and colors |

### 🚀 SDUI Feature Modules
| Module | GitHub Path | Screen & Responsibilities |
| :--- | :--- | :--- |
| **`:app`** | [`/app`](https://github.com/AmmarAlGhifary/cinestream-sdui/tree/development/app) | Main SDUI application entrypoint, Hilt DI setup, and navigation wiring |
| **`:feature:home`** | [`/feature/home`](https://github.com/AmmarAlGhifary/cinestream-sdui/tree/development/feature/home) | Dynamic Home dashboard: Hero banner and horizontal movie carousels |
| **`:feature:detail`** | [`/feature/detail`](https://github.com/AmmarAlGhifary/cinestream-sdui/tree/development/feature/detail) | Movie detail screen with dynamic metadata and similar film sections |
| **`:feature:listMovie`** | [`/feature/listMovie`](https://github.com/AmmarAlGhifary/cinestream-sdui/tree/development/feature/listMovie) | Categorized vertical movie listing (Trending, Upcoming, Similar) |
| **`:feature:search`** | [`/feature/search`](https://github.com/AmmarAlGhifary/cinestream-sdui/tree/development/feature/search) | Real-time movie search exploration screen |

---

## 🌐 SDUI Contract & Backend Endpoints

The Android client dynamically consumes endpoints defined in [cinestream-sdui-backend](https://github.com/AmmarAlGhifary/cinestream-sdui-backend):

| Endpoint | Method | Parameters | Android Consumer | Description |
| :--- | :---: | :--- | :--- | :--- |
| `/api` / `/` | `GET` | None | Network Health | Health check and available endpoint directory |
| `/api/home` | `GET` | None | `:feature:home` | Blueprint containing Hero Banner + Carousel sections |
| `/api/movie_detail_screen` | `GET` | `movie_id` | `:feature:detail` | Blueprint for movie backdrop, summary, and similar carousels |
| `/api/movie_list_screen` | `GET` | `list_type`, `movie_id` | `:feature:listMovie` | Vertical list blueprint for categories (`trending`, `upcoming`, `similar`) |
| `/api/search` | `GET` | `query` | `:feature:search` | Blueprint for search query results |

---

## 🌿 Git Branch Structure & Coordination

| Repository | Active Branch | Purpose |
| :--- | :--- | :--- |
| **`cinestream-sdui`** | `development` | Main integration branch for SDUI Android client features |
| **`cinestream-sdui`** | `main` | Production-ready stable release branch |
| **`cinestream-sdui-backend`** | `main` | Production deployment branch automatically hosted on Vercel |

---

## 🚀 Local Development Setup

### 1. Prerequisites
- **Android Studio** Ladybug or newer
- **JDK 17** (Temurin 17 LTS recommended)
- **Android SDK** API 26 (Min SDK) to API 36 (Compile/Target SDK)

### 2. Configure Backend Endpoint
In `gradle.properties`:
```properties
# Debug builds: the local Vercel dev server via the Android emulator alias
DEBUG_BASE_URL=http://10.0.2.2:3000/

# Release builds: the production deployment on Vercel (https only)
RELEASE_BASE_URL=https://sdui-backend.vercel.app/
```

To override the address for one build, whatever its type:
```bash
./gradlew :app:installDebug -PBASE_URL=https://sdui-backend.vercel.app/
```
A release build fails if its address does not start with `https://`.

> **Note:** Plain HTTP works only in debug builds, and only to `10.0.2.2` and `localhost` ([`network_security_config.xml`](app/src/debug/res/xml/network_security_config.xml)). A physical device can reach a local server through `adb reverse tcp:3000 tcp:3000` with `-PBASE_URL=http://localhost:3000/`.

### 3. Build & Install via Gradle CLI
```bash
# Make gradlew executable
chmod +x gradlew

# Build and install SDUI application on running AVD/device
./gradlew :app:installDebug

# Run unit tests across all modules
./gradlew testDebugUnitTest
```
