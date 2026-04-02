# EPUB Reader with Personal Knowledge Graph

## Database Layer Technical Documentation (Phase 1)

## Purpose and Scope

This document explains the **database layer architecture** for a local-first EPUB reader that treats notes as a **personal knowledge graph**, not as passive annotations. The schema is designed to support:

* EPUB library management
* Deep navigation and reading progress
* Highlights as contextual anchors
* Notes as first-class, standalone ideas
* Explicit bidirectional note linking
* Full-text search across books and notes
* Analytics-ready reading telemetry

This README is intended for developers working on the data, domain, and search layers.


## Architectural Philosophy

### Local-First and Durable Thinking

* All data lives locally using Room (SQLite).
* The system is usable offline by design.
* Notes do **not** depend on books to exist.

### Knowledge Graph, Not a Notebook

* Notes are **nodes**.
* Explicit links are **edges**.
* Highlights are **contextual grounding**, not the core unit.
* Backlinks are mandatory, not optional UI sugar.

### Separation of Concerns

* Books own **content and highlights**.
* Notes own **ideas and relationships**.
* Search is orthogonal and powered by FTS.


## High-Level Data Model Overview

### Core Conceptual Layers

* **Library Layer**: Books, Chapters
* **Reading Layer**: Highlights, Progress, Sessions
* **Thinking Layer**: Notes, Note Links
* **Organization Layer**: Tags
* **Discovery Layer**: Full-Text Search (FTS)

Each layer is loosely coupled and queryable independently.


## Core Entities

### Book

Represents an EPUB source file.

**Key characteristics**

* Books are sources, not silos.
* They can be deleted without destroying ideas.
* DRM status is tracked to enable feature gating.

**Key fields**

* `filePath`: EPUB location in app storage
* `drmStatus`: NONE, DRM_PROTECTED, UNKNOWN
* `rating`: User-defined quality signal
* `importTimestamp`: Enables recency queries


### Chapter

Represents structural navigation inside a book.

**Why it exists**

* Enables chapter-level navigation.
* Supports accurate progress tracking.
* Acts as a reference point for highlights.

**Key fields**

* `chapterIndex`: Ordering within the book
* `cfiOrHref`: Deep link locator


### Highlight

Represents quoted text from a book.

**Conceptual role**

* Highlights are **context nodes**.
* They ground notes in source material.
* They are not the primary thinking unit.

**Key fields**

* `cfiOrRange`: Exact EPUB text location
* `text`: Captured quoted passage
* `color`: Visual categorization

Highlights are always book-owned and cascade on book deletion.


### Note

The **primary node** in the knowledge graph.

**Critical properties**

* Notes can exist without books.
* Notes can exist without highlights.
* Notes evolve over time.

**Key fields**

* `title` and `body`: Freeform idea space
* `bookId`: Optional contextual reference
* `highlightId`: Optional grounding reference
* `createdAt` and `modifiedAt`: Temporal evolution

This design enables thinking-first workflows.


### NoteLink

Represents **explicit, directional relationships** between notes.

**Why this matters**

* Direction encodes meaning.
* Backlinks are derived, not stored.
* Enables Zettelkasten-style navigation.

**Key fields**

* `fromNoteId`: Source idea
* `toNoteId`: Referenced idea
* `linkType`: Future semantic extension

Deleting a note deletes all its edges.


### Tag and BookTag

Supports categorization and filtering.

**Design decisions**

* Tags are global and unique by name.
* Books can have many tags.
* Notes are intentionally not tagged in Phase 1.

This avoids premature ontology design.


### ReadingProgress

Tracks the last known reading state for a book.

**Usage**

* Resume reading
* Completion detection
* Progress dashboards

**Key fields**

* `lastCfi`: Exact EPUB location
* `progressPercent`: Normalized 0–100

One row per book, enforced by a unique index.


### ReadingSession

Captures reading analytics.

**Purpose**

* Daily reading time
* Habit tracking
* Insight generation

**Key fields**

* `startTime`, `endTime`
* `durationMinutes`

Sessions are append-only and analytics-friendly.


## Full-Text Search (FTS)

### BookTextFts

Virtual table for searching EPUB content.

**Key design points**

* Populated during EPUB import.
* Stores extracted chapter text.
* Enables cross-book idea discovery.

**Why this matters**

Search is not limited to metadata or highlights.
Ideas can be discovered across the entire library.


### NoteFts

Virtual table for searching notes.

**Searchable content**

* Note titles
* Note bodies

This enables global thinking recall independent of source books.


## Data Access Objects (DAOs)

### Reactive Design

* Read operations use `Flow<T>` where appropriate.
* Enables live UI updates.
* Supports incremental recomposition.


### BookDao

Supports:

* Library browsing
* Continue-reading surfaces
* Rating-based discovery
* Library analytics


### ChapterDao

Supports:

* Chapter navigation
* Re-import cleanup
* Structural consistency


### HighlightDao

Supports:

* Highlight review per book
* Contextual note creation
* Highlight metrics


### NoteDao

The most critical DAO.

**Key capabilities**

* Global note streams
* Notes by book or highlight
* Outgoing links
* Backlinks (memory mechanism)

Backlinks are resolved dynamically using reverse link queries.


### NoteLinkDao

Manages graph edges.

**Responsibilities**

* Prevent duplicate edges
* Clean up on note deletion
* Link existence checks

No implicit links are ever created.


### TagDao

Supports:

* Tag creation
* Book-tag assignment
* Tag-based filtering


### ReadingProgressDao

Handles:

* Resume logic
* Completion marking
* Progress persistence


### ReadingSessionDao

Supports time-based analytics:

* Daily reading totals
* Arbitrary date range summaries


### SearchDao

Unified search interface for:

* EPUB content
* Notes

**Returns**

* Structured search result DTOs
* Jump-to-location metadata


## Search Result Models

### BookSearchResult

Contains:

* Matched text
* Book title
* Chapter reference
* CFI locator

Used to jump directly to the passage.


### NoteSearchResult

Contains:

* Note metadata
* Creation timestamp
* Optional book context

Used for global idea recall.


## Database Configuration

### EPubReaderDatabase

* Central Room database definition.
* Schema version: 1
* Schema export enabled.

**Entities included**

* Core entities
* FTS virtual tables


### Database Builder

**Current behavior**

* Uses destructive migration.
* Intended for MVP and rapid iteration.

**Production requirement**

* Replace with versioned migrations before release.


## Lifecycle and Data Integrity Rules

### Deletion Behavior Summary

* Deleting a book deletes chapters, highlights, progress, sessions, and book tags.
* Deleting a highlight does not delete notes.
* Deleting a note deletes all its links.
* Notes remain durable unless explicitly deleted.


## Phase 1 Completion Guarantees

This database layer guarantees:

* Standalone note creation
* Explicit idea linking
* Bidirectional traversal
* Full-text search across sources and ideas
* Offline-first reliability


## Future Extensions (Out of Scope)

* Semantic link types
* Note versioning
* Note tagging
* Graph algorithms (centrality, clustering)
* Sync and conflict resolution

These are intentionally deferred.


## Summary

This schema is not a typical EPUB annotation database.

It is a **thinking system** with books as inputs, notes as ideas, and links as meaning.

Everything else exists to support that core truth.

# EPUB Reader with Personal Knowledge Graph

## Repository Layer Technical Documentation

## Purpose and Scope

This document explains the **Repository Layer**, which sits above the database and below the UI. This layer is responsible for **business logic, graph integrity, and conceptual enforcement**.

While the database defines what *can* exist, the repository layer defines what *should* exist.

This layer ensures that:

* Books remain sources, not containers
* Notes remain first-class ideas
* Links remain explicit and meaningful
* Backlinks emerge automatically
* Full-text search suggests, but never decides


## Architectural Role of the Repository Layer

### Position in the Stack

* UI Layer consumes **domain models**, not raw entities
* Repository Layer orchestrates:

  * Multiple DAOs
  * Graph rules
  * Indexing side effects
* Database Layer remains unaware of intent

The repository layer is where **meaning** is enforced.


## Domain Models (UI-Facing Contracts)

### BookWithMetadata

An enriched projection of a book for UI consumption.

**Why it exists**

The UI should not assemble counts and relationships itself. This model provides a cohesive snapshot.

**Included context**

* Book entity
* Highlight count
* Note count
* Tags
* Reading progress

This supports library views, dashboards, and book detail screens.


### NoteWithLinks

The most important domain model in the system.

**Conceptual importance**

* Represents a **node plus its graph neighborhood**
* Makes backlinks a first-class concept
* Prevents UI from ignoring graph memory

**Included context**

* The note itself
* Outgoing links
* Incoming links (backlinks)
* Optional grounding highlight
* Optional source book

Every serious note view should be powered by this model.


### SearchResult (Sealed Class)

A unified abstraction for global search.

**Variants**

* BookPassage
* NoteResult

This allows the UI to render heterogeneous results without leaking storage details.


## BookRepository

### Responsibilities

* Library orchestration
* Book lifecycle management
* Metadata aggregation
* Book-level FTS indexing control


### Enriched Book Retrieval

`getAllBooksWithMetadata`

* Combines multiple DAOs
* Produces UI-ready models
* Keeps UI stateless and reactive

This is a deliberate anti-pattern to “query everything in the UI”.


### Book Import Flow

`importBook`

This is the **entry point for EPUB ingestion**.

Responsibilities include:

* Persisting book metadata
* Inserting chapters
* Initializing reading progress

EPUB parsing and text extraction happen outside, but persistence happens here.


### Deletion Semantics

`deleteBook`

Key design rule:

* Relational cleanup is handled by CASCADE
* FTS cleanup must be manual

This separation avoids accidental orphaned search data.


### DRM-Aware Indexing

`indexBookText`

* DRM-protected books are explicitly excluded from FTS
* This preserves legal and functional boundaries

The repository layer enforces this policy, not the database.


## HighlightRepository

### Responsibilities

* Highlight lifecycle management
* No graph logic
* No note creation logic

Highlights are deliberately kept simple.

**Key principle**

Highlights are context, not cognition.


## NoteRepository (Knowledge Graph Core)

### Central Responsibility

This repository enforces **knowledge graph integrity**.

If this repository is correct, the system thinks correctly.


### Retrieving Notes with Context

`getNoteWithLinks`

This method is non-negotiable.

It guarantees that:

* Outgoing links are visible
* Backlinks are visible
* Context is preserved

Backlinks are not optional UI decorations. They are memory.


### Note Creation Modes

#### Standalone Notes (Thinking-First)

`createStandaloneNote`

* No book required
* No highlight required
* Indexed immediately

This supports ideation outside reading.


#### Highlight-Based Notes (Reading-First)

`createNoteFromHighlight`

* Anchors idea to source text
* Preserves book and highlight references
* Indexed immediately

Both flows are equal citizens.


### Note Updates and Reindexing

Every note update:

* Updates `modifiedAt`
* Triggers FTS reindexing

Search is always consistent with thought.


### Linking Notes (Graph Edges)

`linkNotes`

This method enforces multiple invariants:

* Both notes must exist
* Self-links are forbidden
* Duplicate links are prevented
* Directionality is preserved

Only explicit, human-curated links are allowed.


### Unlinking Notes

`unlinkNotes`

* Removes a single directional edge
* Backlinks disappear automatically

No additional cleanup logic is required.


### Note Deletion

`deleteNote`

Deletion guarantees:

* FTS index cleanup
* Cascade removal of all links

The graph never contains dangling edges.


### Related Notes via Search (Suggestions)

`getRelatedNotes`

Purpose:

* Suggest possible connections
* Never auto-create links

FTS is advisory. The graph remains intentional.


### Key Term Extraction

Current implementation:

* Simple tokenization
* Length filtering
* OR-based query

This is intentionally naive and replaceable.


## SearchRepository

### Responsibilities

* Unified global search
* Result normalization
* Context-aware mapping


### Global Search

`globalSearch`

Searches:

* Full EPUB content
* All notes

Returns:

* Book passages with jump targets
* Notes with content snippets

This supports discovery without collapsing context.


## ReadingSessionRepository

### Responsibilities

* Session lifecycle
* Reading analytics
* Progress updates


### Session Start and End

* Sessions are append-only
* Progress updates are explicit
* Duration logic is replaceable

Analytics correctness is separated from reading correctness.


### Aggregated Metrics

Supports:

* Weekly reading time
* Completion tracking

This enables habit and insight features without polluting core models.


## Graph Integrity Rules (Enforced by Repositories)

These rules are conceptual, but enforced in code.

### Rule 1: Notes Are Independent

* Notes do not require books
* Notes do not require highlights

This is enforced by optional foreign keys and repository APIs.


### Rule 2: Links Are Explicit

* No automatic graph mutation
* Suggestions never become structure

This preserves trust in the graph.


### Rule 3: Backlinks Are Guaranteed

* Backlinks are derived, not stored
* Every link has a visible reverse

Memory is a property of structure, not UI.


### Rule 4: Highlights Ground, Not Own

* Highlights anchor notes
* Notes are not subordinate to highlights

This avoids annotation traps.


### Rule 5: Books Are Sources

* Notes can span books
* Cross-book thinking is fundamental

Books are inputs, not silos.


### Rule 6: Search Suggests, Graph Remembers

* FTS helps discovery
* Links preserve meaning

Ephemeral relevance versus durable understanding.


### Rule 7: Everything Is Local

* No network dependency
* Graph is fully exportable
* Structure is preserved

This system is user-owned by design.


## Summary

The repository layer is the **guardian of meaning**.

* The database stores facts
* The repository enforces philosophy
* The UI merely reflects both

If the database layer defines *what exists*,
the repository layer defines *why it exists and how it relates*.

This is where the system stops being an EPUB reader and becomes a thinking tool.