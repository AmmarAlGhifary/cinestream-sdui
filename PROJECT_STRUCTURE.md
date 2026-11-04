# Project Structure

This project is a multi-module Android application engineered around a Server-Driven UI (SDUI) architecture, along with a counterpart classic implementation for architectural comparison.

The repository is modularized into `app`, `core`, `feature`, `app-classic`, and `feature-classic` components.

---

## Modules Directory

### 🚀 SDUI Architecture Modules

#### 1. `app`
The main application entry point and dependency injection root for the SDUI engine.
- **Package**: `com.ammar.cinestream`
- **Responsibilities**: Application-level initialization, Dagger Hilt DI container setup, and global navigation wiring (`MainActivity`).

#### 2. `core`
Reusable foundational libraries and shared data layer logic.

* **`core:sdui`** (Server-Driven UI Engine, built on [Blueprint](https://github.com/AmmarAlGhifary/blueprint))
  Decodes server responses and renders them as Jetpack Compose. Decoding and the render loop come from
  Blueprint (`io.github.ammaralghifary:blueprint-core`); this module supplies CineStream's vocabulary.
  - `data/repository`: Fetches SDUI JSON and decodes it with `Blueprint.decodeScreen`. Folds `toolbar` + `body` into a `screen_scaffold` node.
  - `domain/model`: CineStream component and action models, subclasses of Blueprint's open `SduiComponent` / `SduiAction`.
  - `domain/usecase`: SDUI business logic.
  - `presentation/components`: Compose renderers, each receiving Blueprint's `SduiScope` to render children and dispatch actions.
  - `presentation/registry`: `CineStreamRegistry` binds every JSON `type` to a model and renderer; `CineStreamBlueprint` is the shared instance.
  - Tolerant decoding: an unknown or malformed node degrades alone (placeholder in debug, nothing in release) and is logged under the `Blueprint` tag.

* **`core:network`**
  Handles external API communications and networking infrastructure.
  - `di`: Network module dependency injection provisioning.
  - `source`: Retrofit API services, interceptors, and network data sources.

* **`core:designsystems`**
  Shared UI design system, tokens, typography, and themes.
  - `theme`: Compose Material 3 theme definitions, color palettes, and typography used globally.

#### 3. `feature`
Feature modules implementing specific screens driven by the SDUI engine under an MVVM architecture.

* **`feature:home`**
  - `state`: UI states for the home dashboard.
  - `ui`: Dynamic home feed containing hero sections and carousels.
  - `viewmodel`: Coordinates SDUI blueprint fetching and refresh states.

* **`feature:detail`**
  - `state`: UI states for the movie detail screen.
  - `ui`: Detail view rendering metadata and similar movie carousels.
  - `viewmodel`: Handles movie-specific blueprint retrieval.

* **`feature:listMovie`**
  - `state`: UI states for movie category lists.
  - `ui`: Vertical list layouts for categories (Trending, Upcoming, Similar).
  - `viewmodel`: Handles pagination and query filters.

* **`feature:search`**
  - `state`: UI states for the search flow.
  - `ui`: Real-time search query bar and vertical results.
  - `viewmodel`: Query processing and debounce handling.

---

### 🏛️ Classic Architecture Modules (Comparative Baseline)

Used for empirical comparison in thesis research against traditional native Android development patterns.

#### 1. `app-classic`
- **Package**: `com.ammar.cinestream.classic`
- **Responsibilities**: Host application for the hardcoded benchmark application.

#### 2. `feature-classic`
Feature modules directly consuming REST endpoints (TMDB) with hardcoded UI structures.
* **`feature-classic:home`**: Hardcoded home screen with static carousel sections.
* **`feature-classic:detail`**: Hardcoded movie detail screen.
* **`feature-classic:listMovie`**: Hardcoded category movie list screen.

---

## Key Architectural Patterns

- **Modularization**: Strict separation of concerns by layers (`core`, `feature`) to improve build speeds, promote reusability, and prevent unintended coupling.
- **Clean Architecture & MVVM**: Unidirectional data flow (UDF) with immutable UI state and Kotlin Coroutines/Flow.
- **Server-Driven UI (SDUI)**: Client acts as a declarative rendering runtime, allowing dynamic updates to UI hierarchy, components, and user actions directly from the backend without client application releases.
