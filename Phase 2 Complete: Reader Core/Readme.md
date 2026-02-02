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
