# EPUB Reader with Personal Knowledge Graph

## Phase 2 Reader Screen UI Technical Documentation

## Purpose and Scope

This document explains the **Reader Screen UI layer**, implemented using **Jetpack Compose**, and its integration with the reading, highlighting, and note-creation workflows.

This layer is where **reading becomes thinking**.

It translates the conceptual model into lived interaction by combining:

* EPUB rendering
* Text selection and highlighting
* Immediate note creation
* Reading preferences
* Progress tracking
* Session lifecycle management

This documentation is intended for UI engineers, platform engineers, and anyone extending the reader experience.

---

## Architectural Role of the Reader UI Layer

### Position in the Stack

* Sits above the Repository and ReadingController layers
* Owns **interaction state**, not business state
* Delegates all persistence and graph logic downward

The reader UI is **thin by design**.
It reacts, it orchestrates, it never decides meaning.

---

## High-Level Responsibilities

The Reader UI layer is responsible for:

* Rendering EPUB content
* Responding to user gestures
* Surfacing contextual actions
* Managing transient UI state
* Applying reading preferences
* Initiating highlight and note creation flows

It is explicitly not responsible for:

* Graph integrity
* Persistence logic
* Search indexing
* Progress storage rules

---

## ReaderViewModel

### Purpose

The `ReaderViewModel` acts as the **interaction conductor** between:

* Readium-based navigation
* Repositories
* User preferences
* UI state

It is lifecycle-aware and session-aware.

---

### State Streams

The ViewModel exposes three primary reactive streams:

#### Reading State

* Provided by `ReadingController`
* Represents navigator loading, progress, selection, and errors

#### Preferences

* Provided by `PreferencesManager`
* Includes font size and theme

#### UI State

* Local Compose-only state
* Controls overlays, dialogs, and transient UI behavior

This separation prevents UI noise from polluting domain state.

---

### Book Lifecycle

#### Opening a Book

* Initiates a reading session
* Loads EPUB via `ReadingController`
* Emits loading and error states

Failures are surfaced immediately to the UI.

---

#### Closing a Book

* Ends reading session
* Flushes final progress
* Triggered on navigation or lifecycle cleanup

This guarantees session closure even on abrupt exits.

---

## ReaderUiState

### Purpose

Encapsulates **ephemeral UI concerns** that should not persist.

### Included Flags

* Control visibility
* Selection menu visibility
* Note creation dialog state
* Highlight creation handoff
* Error display state

This keeps Compose recompositions predictable and explicit.

---

## ReaderScreen Composable

### Role

The top-level screen that:

* Binds ViewModel state to UI
* Manages lifecycle hooks
* Composes all reader subcomponents

---

### Lifecycle Integration

#### Initial Load

* Automatically opens the book on first composition
* Book identity is stable via `LaunchedEffect`

#### Cleanup

* Ensures session termination via `DisposableEffect`

This prevents dangling reading sessions.

---

## Visual Structure

### Main Content Area

Displays one of three states:

* Loading indicator
* Error screen
* Reader content

This avoids partially rendered or ambiguous states.

---

### Overlay Controls

Controls are intentionally **ephemeral and hidden by default**.

#### Top Controls

* Back navigation
* Reading progress percentage
* Progress bar

Used for orientation, not distraction.

---

#### Bottom Controls

* Font size adjustment
* Theme cycling

Preferences apply immediately and persist across sessions.

---

### Selection Menu

Appears only when:

* Text is actively selected
* User intent is clear

Provides:

* Highlight color options
* Cancel action

This prevents accidental highlights.

---

### Note Creation Dialog

Triggered immediately after highlight creation.

Design intent:

* Capture thought while context is fresh
* Reduce friction between reading and thinking
* Allow skipping without penalty

The dialog always shows the highlight context.

---

## ReaderContent Composable

### Purpose

Hosts the actual EPUB rendering surface.

---

### Theme Application

Background color adapts based on reading theme:

* Day
* Night
* Sepia

This improves readability and reduces cognitive fatigue.

---

### Readium Integration Placeholder

Current implementation uses a placeholder view.

In production:

* This will be replaced with `EpubNavigatorFragment`
* Integrated via `AndroidView` and `FragmentContainerView`

The UI is designed to remain stable when the navigator is swapped.

---

## UI Components

### LoadingIndicator

* Communicates progress clearly
* Avoids empty screens
* Reinforces system responsiveness

---

### ErrorDisplay

* Shows failure reasons
* Provides retry action
* Prevents silent failures

---

### TopControls

* Always visible together
* Progress is numeric and visual
* Navigation is explicit

---

### BottomControls

* Font size changes are incremental
* Theme toggle cycles predictably
* No hidden settings screens required

---

### SelectionMenu

* Displays truncated selection context
* Offers visual color cues
* Cancel is always visible

This reduces accidental actions and ambiguity.

---

### HighlightColorButton

* Minimalist visual affordance
* Color communicates meaning instantly
* Labels reduce guesswork

---

### NoteCreationDialog

### Design Principles

* Context first
* Thought second
* Commitment optional

The dialog enforces:

* Non-empty title
* Non-empty body

This preserves note quality without being heavy-handed.

---

## Interaction Flow Summary

### Reading-First Flow

1. User reads EPUB
2. Selects text
3. Chooses highlight color
4. Creates note immediately
5. Returns to reading

This flow minimizes context switching.

---

### Control Flow

* Single tap toggles controls
* Controls auto-hide unless explicitly shown
* Reading remains the default state

The UI never fights the reader.

---

## Integration Notes for Readium (Production)

### Required Integration Steps

#### Navigator Hosting

* Use `FragmentContainerView`
* Manage fragment lifecycle explicitly

---

#### Location Tracking

* Listen to locator changes
* Calculate progress consistently
* Forward updates to ViewModel

---

#### Text Selection Handling

* Implement selection listeners
* Capture selected text and locator
* Trigger selection menu

---

#### Highlight Rendering

* Use Readium Decorations API
* Map stored highlights to visual overlays
* Keep rendering idempotent

---

#### Locator Persistence

* Store full Readium locator JSON
* Avoid lossy position formats
* Enable precise navigation and recovery

---

## Design Invariants Enforced by the UI

### Invariant 1: Reading Comes First

* UI defaults to content
* Controls are hidden unless requested

---

### Invariant 2: Thought Is Immediate

* Notes can be created at the moment of insight
* No navigation required to capture ideas

---

### Invariant 3: Context Is Preserved

* Highlights are always visible during note creation
* Notes never lose their grounding

---

### Invariant 4: Preferences Are Lightweight

* No settings screen required
* Adjustments are reversible and instant

---

## Summary

The Reader UI layer is where **books turn into ideas**.

* It is quiet when you read
* It is present when you think
* It is invisible when you flow

This layer does not store meaning,
but it makes meaning easy to create.

When implemented correctly, the reader screen disappears,
and the knowledge graph quietly begins to grow.

# EPUB Reader with Personal Knowledge Graph

## Phase 2 Reader Core and Readium Integration Technical Documentation

## Purpose and Scope

This document explains the **Reader Core layer**, which integrates the **Readium Kotlin Toolkit** to provide EPUB rendering, navigation, selection, highlighting, progress tracking, and DRM awareness.

This layer is the **execution engine of reading**.

It sits below the UI and above persistence, translating EPUB mechanics into stable, observable state that the rest of the system can trust.

This documentation is intended for platform engineers and developers responsible for reader correctness, performance, and integration.

---

## Architectural Role of the Reader Core

### Position in the Stack

* Below the Compose UI
* Above repositories and database
* Owns EPUB lifecycle and reading state
* Acts as the single authority on “what is currently being read”

The Reader Core does not store ideas and does not render UI.
It governs **truth about reading**.

---

## High-Level Responsibilities

The Reader Core layer is responsible for:

* Opening and closing EPUB publications
* Managing Readium `Publication` lifecycles
* Tracking reading position via locators
* Handling text selection and highlights
* Coordinating reading sessions
* Detecting and responding to DRM constraints
* Supplying stable reading state to the UI

It explicitly avoids:

* UI decisions
* Knowledge graph logic
* Long-term persistence rules

---

## PublicationService

### Purpose

`PublicationService` is the **single gateway** for interacting with Readium publications.

It ensures:

* Publications are opened once per book
* Resources are cleaned up correctly
* DRM status is detected early

---

### Publication Lifecycle Management

#### Opening a Publication

When opening a publication:

* File existence is validated
* Readium `Streamer` opens the asset
* DRM restrictions are detected
* The publication is cached per book ID

This avoids duplicate publication instances and resource leaks.

---

#### Closing a Publication

Closing a publication:

* Removes it from the in-memory cache
* Releases all underlying resources

This must always be called when reading ends.

---

### PublicationResult

Results are explicitly modeled as:

* `Success` with DRM status
* `Error` with a user-facing message

This avoids leaking Readium-specific failures upward.

---

## ReadingState

### Purpose

`ReadingState` represents the **current truth of a reading session**.

It is immutable and emitted reactively.

---

### Included State

* Book identity
* Current locator
* Progress percentage
* Loading and error flags
* Active text selection

This state is consumed directly by the UI.

---

## TextSelection

### Purpose

Represents an active user text selection.

### Included Data

* Selected text
* Readium locator
* Optional screen rectangle

This enables:

* Accurate highlight creation
* Contextual UI placement

---

## ReadingController

### Purpose

The `ReadingController` coordinates:

* Publication access
* Reading sessions
* Highlight creation
* Reading progress persistence

It behaves like a ViewModel without owning UI concerns.

---

### Reading Session Start

When a session starts:

1. The publication is opened
2. A reading session is created
3. Last known progress is retrieved
4. Initial reading state is emitted

Failure at any stage is surfaced immediately.

---

### Location Updates

Whenever navigation occurs:

* Locator is updated
* Progress percentage is updated
* State is re-emitted

No persistence happens here. This remains a pure state update.

---

### Text Selection Handling

Selection events:

* Update reading state
* Trigger downstream UI affordances
* Remain transient until acted upon

Selections are never persisted directly.

---

### Highlight Creation Flow

Creating a highlight:

1. Reads current selection
2. Extracts CFI fragment
3. Persists highlight via repository
4. Clears selection state

This ensures a clean interaction loop.

---

### Ending a Reading Session

Ending reading guarantees:

* Session duration persistence
* Progress persistence
* Publication closure
* State reset

This is the most critical cleanup path in the system.

---

## Locator Utilities

### Purpose

`LocatorUtils` provides safe, minimal utilities for working with Readium locators.

---

### Locator to CFI

* Extracts the first fragment
* Used for database storage
* Simple and deterministic

---

### CFI to Locator

* Reconstructs a locator from stored data
* Requires publication context
* Simplified for MVP usage

Production implementations must be more robust.

---

### Progress Calculation

Progress is calculated using:

* `totalProgression` when available
* Fallback position-based estimation

This ensures progress is always available, even when incomplete.

---

## Highlight Rendering Support

### HighlightDecoration

A UI-agnostic structure that maps:

* Database highlights
* Readium locators
* Visual attributes

This decouples persistence from rendering.

---

### HighlightDecorationService

Responsibilities include:

* Loading highlights for a book
* Converting CFIs into locators
* Preparing decorations for Readium

This service bridges the database and the navigator.

---

## Text Extraction for Indexing

### TextExtractionService

### Purpose

Extracts full readable text from EPUB files during import.

This supports:

* Full-text search
* Cross-book discovery
* Offline indexing

---

### Extraction Flow

* Open EPUB with Streamer
* Iterate reading order
* Read and clean chapter content
* Emit structured chapter text objects

Extraction errors are isolated per chapter.

---

### HTML Cleaning Strategy

* Strip tags
* Remove entities
* Normalize whitespace

This produces search-friendly text without layout noise.

---

## DRM Detection and Handling

### DrmHandler

### Purpose

Centralizes DRM interpretation and messaging.

---

### DRM Status Evaluation

* Uses Readium restriction flags
* Differentiates between:

  * Fully restricted content
  * Content with limited rights
  * DRM-free content

This enables feature gating without breaking reading.

---

### User Messaging

DRM messages are:

* Informative, not punitive
* Feature-specific
* Optional

Reading is never blocked unless technically required.

---

## Reading Preferences

### ReadingPreferences

Encapsulates all user-controlled reading parameters:

* Font size
* Font family
* Line height
* Theme
* Scroll mode

Preferences are intentionally orthogonal to reading state.

---

### PreferencesManager

### Responsibilities

* Maintains reactive preference state
* Enforces sane bounds
* Applies updates immediately

Persistence is deferred to production storage solutions.

---

## Integration Requirements

### Readium Toolkit Dependencies

The reader core depends on:

* Readium Shared
* Readium Streamer
* Readium Navigator
* Optional LCP support

Correct version alignment is mandatory.

---

### Critical Setup Steps

Implementers must ensure:

* Readium initialization at app startup
* Correct storage paths
* Scoped storage compliance
* Publication lifecycle management

Failure here results in subtle, hard-to-debug errors.

---

## Known Limitations

The current implementation intentionally simplifies:

* CFI parsing and serialization
* DRM handling depth
* Text extraction threading
* Locator reconstruction

These are acceptable for Phase 2 and must be addressed before production.

---

## Design Invariants Enforced by the Reader Core

### Invariant 1: One Publication per Book

* Prevents resource leaks
* Ensures consistent state

---

### Invariant 2: State Is Observable

* All reading state flows outward
* No hidden internal mutations

---

### Invariant 3: Persistence Is Explicit

* No silent database writes
* Progress and sessions are deliberate

---

### Invariant 4: DRM Is Respected

* Features degrade gracefully
* Reading remains primary

---

## Summary

The Reader Core is the **mechanical heart** of the system.

* It opens books
* It tracks position
* It enforces legality
* It guarantees cleanup

When this layer is correct:

* The UI remains simple
* The database remains honest
* The knowledge graph remains trustworthy

This is where reading becomes a reliable substrate
upon which thinking can safely grow.
