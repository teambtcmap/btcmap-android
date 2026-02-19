# AGENTS.md - BTC Map Android Development Guide

## Project Overview
- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 36
- **Architecture**: Android Views with ViewBinding, SQLite database, Coroutines for async

## Build Commands

### Full Build & Verification
```bash
./gradlew check              # Run all verification (lint + tests)
./gradlew assembleDebug      # Build debug APK
./gradlew assembleRelease    # Build release APK
./gradlew bundleData         # Download latest places data snapshot
```

### Running Tests
```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run all instrumented tests (requires emulator/device)
./gradlew connectedDebugAndroidTest

# Run a single instrumented test class
./gradlew connectedDebugAndroidTest --tests "ExampleInstrumentedTest"

# Run a single test method
./gradlew connectedDebugAndroidTest --tests "ExampleInstrumentedTest.useAppContext"
```

### Linting
```bash
./gradlew lint               # Run lint analysis
./gradlew lintDebug          # Run lint on debug build
./gradlew lintRelease        # Run lint on release build
```

## Code Style Guidelines

### Naming Conventions
- **Classes**: PascalCase (e.g., `MapFragment`, `PlaceQueries`, `SearchAdapter`)
- **Functions**: camelCase (e.g., `initSearchBar`, `selectPlace`, `setFilter`)
- **Variables/Properties**: camelCase (e.g., `binding`, `httpClient`, `bottomSheetBehavior`)
- **Constants**: UPPER_SNAKE_CASE inside `companion object` (e.g., `MIN_QUERY_LENGTH`)
- **Package names**: lowercase (e.g., `org.btcmap`, `db.table.place`)

### File Organization
- Source files in `app/src/main/kotlin/`
- Package structure mirrors directory structure
- One class per file (filename matches class name)
- Test files mirror source structure in `app/src/androidTest/kotlin/`

### Imports
- Grouped by:
  1. Kotlin standard library (`kotlinx.*`)
  2. Android framework (`android.*`)
  3. AndroidX (`androidx.*`)
  4. Third-party libraries (`com.google.*`, `okhttp3.*`, etc.)
  5. Project imports (relative paths like `db.db`, `bundle.BundledPlaces`)
- No wildcard imports
- Sorted alphabetically within groups

### Formatting
- Use 4 spaces for indentation (Kotlin default)
- Opening brace on same line for classes/functions
- Space after `//` in comments
- Line length: typically under 120 chars (not strictly enforced)
- Chained calls: one call per line with leading dot
- Multi-parameter functions: one parameter per line with comma on previous line

### Kotlin Patterns

#### Null Safety
- Use `?.` and `?:` extensively
- Prefer late initialization (`lateinit var`) over nullable types for view bindings
- Use `by lazy` for expensive lazy initialization
- Use `!!` sparingly (only when you're certain value is non-null)

#### Coroutines
- Use `viewLifecycleOwner.lifecycleScope.launch` for Fragment coroutines
- Use `withContext(Dispatchers.IO)` for blocking operations
- Use `Dispatchers.Main.immediate` for UI updates requiring immediate execution

#### Extensions
- Extensions are preferred over utility classes
- Extensions live in files named after the extended type (e.g., `FragmentExt.kt` extends `Fragment`)
- Use receiver type aliases for cleaner code

### ViewBinding Usage
```kotlin
private var _binding: MapFragmentBinding? = null
private val binding get() = _binding!!

override fun onCreateView(...): View {
    _binding = MapFragmentBinding.inflate(inflater, container, false)
    return binding.root
}

override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
}
```

### Database (SQLite)
- Use `SQLiteDatabase` with custom `DbHelper`
- Use `lateinit var db: SQLiteDatabase` pattern (initialized in `App.onCreate`)
- Query classes in `db.table.*` packages (e.g., `PlaceQueries`, `CommentQueries`)

### Error Handling
- Use try-catch for network operations and file I/O
- Silent failures acceptable for non-critical operations (e.g., version check failures)
- Always close resources in try-finally or use `.use {}`
- Handle 429 (rate limit) with exponential backoff (see `http/Client.kt`)

### UI/Views
- Use Material Design components
- Use `BottomSheetBehavior` for bottom sheets
- Handle window insets for edge-to-edge display
- Use `isVisible` from AndroidX for visibility control

### Constants
- Group constants in `companion object` at bottom of class
- Use descriptive names (e.g., `MIN_QUERY_LENGTH` not `MIN_QL`)

## Project Structure
```
app/src/main/kotlin/
├── activity/        # Activity classes
├── api/             # API interfaces (PlaceApi, CommentApi, etc.)
├── app/             # Application class
├── boost/           # Boost/sponsor functionality
├── bundle/         # Bundled data
├── comment/        # Comments feature
├── db/             # Database helpers and queries
├── fragment/       # Fragment extensions
├── http/           # HTTP client setup
├── icon/           # Marker icons
├── json/           # JSON utilities
├── map/            # Map functionality
├── place/          # Place details
├── search/         # Search functionality
├── settings/       # User preferences
├── sync/           # Data synchronization
├── time/           # Date/time utilities
├── typeface/       # Custom fonts
└── view/           # Custom views
```

## Dependencies
- **Networking**: OkHttp with coroutines extension
- **Maps**: MapLibre (Open-source Mapbox alternative)
- **UI**: Material Design Components
- **Async**: Kotlin Coroutines
- **QR Codes**: QRGenerator
- **Color Picker**: Colorpicker library

## Testing
- Instrumented tests only (require device/emulator)
- Tests located in `app/src/androidTest/kotlin/`
- Use `AndroidJUnit4` runner
- Access app context via `InstrumentationRegistry.getInstrumentation().targetContext`
