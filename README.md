# EPUB Reader with Personal Knowledge Graph

## Overview

This project is a full-fledged **EPUB reading, annotation, and knowledge management system** built for long-term thinking, learning, and ownership of ideas.

At its core, the application combines:

* A modern EPUB reader
* First-class note-taking
* A bidirectional personal knowledge graph
* Deterministic Markdown export for long-term preservation

The system is intentionally designed so that **knowledge never becomes trapped inside the application**. Every idea, relationship, and annotation can be exported in open formats and reconstructed elsewhere.

---

## Core Philosophy

This project is guided by a small set of uncompromising principles:

* Knowledge must outlive software
* Notes are first-class, not attachments
* Links are explicit and user-controlled
* Backlinks are mandatory for memory
* Export is a moral obligation, not a feature

The app is not just a reader. It is a **knowledge instrument**.

---

## High-Level Architecture

The application is organized into clear, layered responsibilities:

* **Data Layer**: Room database, repositories, and domain models
* **Reader Layer**: EPUB rendering, navigation, highlights, and progress
* **Knowledge Layer**: Notes, links, backlinks, and graph traversal
* **UI Layer**: Jetpack Compose screens and dialogs
* **Export Layer**: Markdown serialization and filesystem output

Each layer is isolated, testable, and replaceable.

---

## Technology Stack

* **Language**: Kotlin
* **UI**: Jetpack Compose (Material 3)
* **Architecture**: MVVM + Repository pattern
* **Dependency Injection**: Hilt
* **EPUB Engine**: Readium Kotlin Toolkit
* **Persistence**: Room + FTS
* **Async**: Kotlin Coroutines + Flow
* **Export Format**: Markdown + YAML Frontmatter

---

## Feature Breakdown by Phase

### Phase 1 – Library & Ingestion

* EPUB import and storage
* Metadata extraction
* Cover handling
* Book-level organization
* Reading progress tracking

Result: A durable and queryable personal library.

---

### Phase 2 – Reader Core

* EPUB rendering via Readium
* CFI-based location tracking
* Persistent reading position
* Text selection and highlighting
* DRM detection and graceful degradation

Result: A serious reading experience, not a file viewer.

---

### Phase 3 – Notes & Knowledge Graph

* Notes as first-class entities
* Standalone notes (no book required)
* Notes from highlights
* Bidirectional linking between notes
* Automatic backlinks
* Related note discovery via FTS

Result: A growing personal knowledge graph.

---

### Phase 4 – UI, Navigation & Discovery

* Netflix-style library browsing
* Continue Reading, Recently Added, Top Rated
* Book detail screens
* Notes browser with filters and search
* Two-pane note navigation
* Bottom navigation with state preservation

Result: A calm, scalable, and discoverable interface.

---

### Phase 5 – Export & Polish

* Deterministic Markdown export engine
* YAML frontmatter for metadata
* Wikilink-style internal links
* Explicit backlinks
* Book-level and library-level indexes
* Timestamped export directories

Result: Knowledge that survives the app.

---

## Knowledge Model

### Notes

Notes are the primary unit of knowledge.

They can:

* Exist independently
* Reference highlights
* Link to other notes
* Receive backlinks automatically

Notes are never deleted implicitly.

---

### Links

* Links are explicit
* User-initiated only
* Bidirectional at the database level
* Rendered in both directions in UI and export

This avoids accidental or opaque graph construction.

---

### Backlinks

Backlinks are not a feature. They are an invariant.

Every note knows:

* What it references
* What references it

This creates memory and rediscovery.

---

## Export System

### Why Export Exists

If the app disappears, the knowledge must not.

Export is treated as a **first-class system**, not an afterthought.

---

### Export Scopes

* Single note
* Single book
* Entire library

Each scope is implemented independently.

---

### Export Format

Each note is exported as a standalone Markdown file containing:

* YAML frontmatter (metadata)
* Highlight quote (if applicable)
* Full note body
* Wikilink-style outgoing links
* Explicit backlinks section

The format is compatible with Obsidian, Logseq, Roam Research, and plain Markdown readers.

---

### Folder Structure

```
EPubReaderExport/
├── README.md
├── Book Title 1/
│   ├── INDEX.md
│   ├── 2026-02-02_14_note-title.md
│   └── ...
├── Book Title 2/
│   └── ...
└── Standalone Notes/
    └── ...
```

---

## UI Philosophy

* No visual noise
* Calm defaults
* Explicit actions
* No dark patterns
* No automatic linking

The UI is designed to support **thinking**, not consumption.

---

## What This Project Is Not

* Not a social platform
* Not a recommendation engine
* Not a proprietary notes silo
* Not a closed ecosystem

This is a personal system.

---

## Build & Run

1. Clone the repository
2. Open in Android Studio
3. Ensure required permissions for storage access
4. Build and run on Android 10+

---

## Key Dependencies

* Readium Kotlin Toolkit
* AndroidX Navigation Compose
* Hilt Navigation Compose
* Coil Compose
* Kotlin Coroutines

---

## Long-Term Vision

This project is intentionally conservative.

It favors:

* Durability over novelty
* Explicit structure over automation
* Ownership over convenience

It is designed to still make sense years from now.

---

## Final Note

This codebase represents a complete lifecycle:

* Read
* Extract
* Reflect
* Connect
* Preserve

At its core, this project is about **respecting thought**.
