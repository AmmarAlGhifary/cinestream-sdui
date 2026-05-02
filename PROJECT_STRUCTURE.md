# Project Structure

This project is a multi-module Android application using a Server-Driven UI (SDUI) approach. The structure is logically separated into `app`, `core`, and `feature` modules.

## Modules Overview

### 📱 `app`
The main entry point of the application. It acts as the wiring module that brings all features and core components together.
- **`com.ammar.cinestream`**: Contains application-level configurations, Dependency Injection (DI) setup, and main activity.

### 🧩 `core`
Contains reusable components, foundational libraries, and data layer logic shared across multiple features.

* **`core:sdui`** (Server-Driven UI Engine)
  The engine responsible for parsing server responses and rendering Jetpack Compose UI dynamically.
  - **`data/repository`**: Handles fetching SDUI layout responses.
  - **`domain/model`**: Defines UI component data models.
  - **`domain/usecase`**: SDUI specific business logic.
  - **`presentation/components`**: Maps domain models to native Jetpack Compose components.
  - **`presentation/registry`**: Component registry for dynamic rendering.

* **`core:network`**
  Handles all external API communications and networking setup.
  - **`di`**: Network module DI provisioning.
  - **`source`**: API clients, interceptors, and remote data sources.

* **`core:designsystems`**
  Houses the common UI elements, typography, colors, and theming.
  - **`theme`**: Contains Compose theme definitions, color palettes, and typography used globally.

### 🌟 `feature`
Isolated feature modules that implement specific screens or flows. Each feature strictly follows an MVVM architecture pattern.

* **`feature:home`**
  - **`state`**: UI states for the home screen.
  - **`ui`**: Compose screens and layouts for the home feed.
  - **`viewmodel`**: Handles the business logic for the home feature.

* **`feature:detail`**
  - **`state`**: UI states for the movie details screen.
  - **`ui`**: Compose screens for viewing movie specifics.
  - **`viewmodel`**: Handles the business logic for fetching and displaying movie details.

* **`feature:listMovie`**
  - **`state`**: UI states for movie lists (e.g., categories, see all).
  - **`ui`**: Compose screens for listing movies.
  - **`viewmodel`**: Handles pagination and listing logic.

* **`feature:search`**
  - **`state`**: UI states for search operations.
  - **`ui`**: Compose screens for the search bar and results.
  - **`viewmodel`**: Handles query processing and search results.

## Key Architectural Patterns
- **Modularization**: Separation of concerns by layers (`core`, `feature`) to improve build times, reusability, and maintainability.
- **Clean Architecture / MVVM**: Each feature module contains `ui`, `state`, and `viewmodel` packages for predictable state management.
- **Server-Driven UI (SDUI)**: The UI logic and structure can be dynamically controlled by the backend through the `core:sdui` engine.
