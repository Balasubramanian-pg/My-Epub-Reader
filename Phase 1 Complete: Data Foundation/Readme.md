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

---

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

---

## High-Level Data Model Overview

### Core Conceptual Layers

* **Library Layer**: Books, Chapters
* **Reading Layer**: Highlights, Progress, Sessions
* **Thinking Layer**: Notes, Note Links
* **Organization Layer**: Tags
* **Discovery Layer**: Full-Text Search (FTS)

Each layer is loosely coupled and queryable independently.

---

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

---

### Chapter

Represents structural navigation inside a book.

**Why it exists**

* Enables chapter-level navigation.
* Supports accurate progress tracking.
* Acts as a reference point for highlights.

**Key fields**

* `chapterIndex`: Ordering within the book
* `cfiOrHref`: Deep link locator

---

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

---

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

---

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

---

### Tag and BookTag

Supports categorization and filtering.

**Design decisions**

* Tags are global and unique by name.
* Books can have many tags.
* Notes are intentionally not tagged in Phase 1.

This avoids premature ontology design.

---

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

---

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

---

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

---

### NoteFts

Virtual table for searching notes.

**Searchable content**

* Note titles
* Note bodies

This enables global thinking recall independent of source books.

---

## Data Access Objects (DAOs)

### Reactive Design

* Read operations use `Flow<T>` where appropriate.
* Enables live UI updates.
* Supports incremental recomposition.

---

### BookDao

Supports:

* Library browsing
* Continue-reading surfaces
* Rating-based discovery
* Library analytics

---

### ChapterDao

Supports:

* Chapter navigation
* Re-import cleanup
* Structural consistency

---

### HighlightDao

Supports:

* Highlight review per book
* Contextual note creation
* Highlight metrics

---

### NoteDao

The most critical DAO.

**Key capabilities**

* Global note streams
* Notes by book or highlight
* Outgoing links
* Backlinks (memory mechanism)

Backlinks are resolved dynamically using reverse link queries.

---

### NoteLinkDao

Manages graph edges.

**Responsibilities**

* Prevent duplicate edges
* Clean up on note deletion
* Link existence checks

No implicit links are ever created.

---

### TagDao

Supports:

* Tag creation
* Book-tag assignment
* Tag-based filtering

---

### ReadingProgressDao

Handles:

* Resume logic
* Completion marking
* Progress persistence

---

### ReadingSessionDao

Supports time-based analytics:

* Daily reading totals
* Arbitrary date range summaries

---

### SearchDao

Unified search interface for:

* EPUB content
* Notes

**Returns**

* Structured search result DTOs
* Jump-to-location metadata

---

## Search Result Models

### BookSearchResult

Contains:

* Matched text
* Book title
* Chapter reference
* CFI locator

Used to jump directly to the passage.

---

### NoteSearchResult

Contains:

* Note metadata
* Creation timestamp
* Optional book context

Used for global idea recall.

---

## Database Configuration

### EPubReaderDatabase

* Central Room database definition.
* Schema version: 1
* Schema export enabled.

**Entities included**

* Core entities
* FTS virtual tables

---

### Database Builder

**Current behavior**

* Uses destructive migration.
* Intended for MVP and rapid iteration.

**Production requirement**

* Replace with versioned migrations before release.

---

## Lifecycle and Data Integrity Rules

### Deletion Behavior Summary

* Deleting a book deletes chapters, highlights, progress, sessions, and book tags.
* Deleting a highlight does not delete notes.
* Deleting a note deletes all its links.
* Notes remain durable unless explicitly deleted.

---

## Phase 1 Completion Guarantees

This database layer guarantees:

* Standalone note creation
* Explicit idea linking
* Bidirectional traversal
* Full-text search across sources and ideas
* Offline-first reliability

---

## Future Extensions (Out of Scope)

* Semantic link types
* Note versioning
* Note tagging
* Graph algorithms (centrality, clustering)
* Sync and conflict resolution

These are intentionally deferred.

---

## Summary

This schema is not a typical EPUB annotation database.

It is a **thinking system** with books as inputs, notes as ideas, and links as meaning.

Everything else exists to support that core truth.

