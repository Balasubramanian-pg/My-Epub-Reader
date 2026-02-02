# EPUB Reader with Personal Knowledge Graph

## Phase 5 Export and Polish Technical Documentation

### Markdown Export Engine

## Purpose and Scope

Phase 5 completes the system by answering a critical long-term question:

What happens to the knowledge if the app disappears?

The **Markdown Export Engine** ensures that all user-created knowledge is:

* Portable
* Human-readable
* Tool-agnostic
* Structurally faithful to the original knowledge graph

This phase transforms the app from a closed container into a **knowledge authoring system**.

---

## Design Goals

The export system is designed around five non-negotiable goals:

* Zero lock-in
* Graph structure preservation
* Long-term readability
* Compatibility with popular knowledge tools
* Deterministic, repeatable output

The output must be useful 10 years from now without the app.

---

## Conceptual Model

### What Is Being Exported

The export is not just notes.
It is a **knowledge graph serialized into Markdown**.

Each exported note preserves:

* Its content
* Its origin
* Its context
* Its relationships

---

### Knowledge Representation Strategy

The system uses a layered representation:

1. YAML frontmatter for metadata
2. Markdown body for human reading
3. Wikilinks for graph navigation
4. Folder structure for semantic grouping

Each layer serves a different consumer.

---

## Export Boundaries

The export system supports three scopes:

* Single note
* Single book
* Entire library

Each scope is implemented explicitly and independently to avoid accidental data loss or partial exports.

---

## MarkdownExportService

### Responsibility

This service owns the entire export pipeline.

It is responsible for:

* Fetching data from repositories
* Creating directory structures
* Generating filenames
* Rendering Markdown content
* Writing files atomically

It does not interact with UI state.

---

### Threading Model

All export operations run on the IO dispatcher.

This ensures:

* No UI blocking
* Safe file system access
* Predictable performance on large exports

---

## Export Directory Structure

### Root Export Directory

Each export creates a timestamped root directory.

This guarantees:

* No overwrites
* Clear export history
* Safe repeated exports

Example:

```
EPubReaderExport/2026-02-02_143012/
```

---

### Book-Level Organization

Notes are grouped by book title.

Each book folder contains:

* INDEX.md
* Individual note files

This mirrors the mental model of reading and annotation.

---

### Standalone Notes

Notes not tied to a book are placed in:

```
Standalone Notes/
```

This ensures no orphaned ideas are lost or hidden.

---

## Filename Strategy

### Deterministic Naming

Each note filename includes:

* Creation date
* Internal note ID
* Sanitized title

This ensures:

* Stable backlinks
* Collision resistance
* Chronological sorting

Example:

```
2026-02-02_14_power-of-habit.md
```

---

### Filename Sanitization

The system removes:

* Unsafe filesystem characters
* Excess whitespace
* Excessive length

This guarantees cross-platform compatibility.

---

## Markdown Content Structure

Each exported note follows the same structure.

Consistency is intentional and enforced.

---

### YAML Frontmatter

The frontmatter captures metadata that tools can parse.

Included fields:

* title
* created_at
* modified_at
* source_book
* author
* location_cfi
* links
* backlinks

This metadata enables:

* Graph reconstruction
* Timeline views
* Search and filtering
* External automation

---

### Highlight Preservation

If a note originated from a highlight:

* The quote is preserved verbatim
* Rendered as a Markdown blockquote
* Attributed to the source book

This maintains academic and contextual integrity.

---

### Note Body

The body is exported without transformation.

No formatting assumptions are imposed.

The user’s voice is preserved exactly.

---

### Internal Links

Relationships are rendered using wiki-style links:

```
[[Note Title]]
```

This format is intentionally chosen because it is supported by:

* Obsidian
* Logseq
* Roam Research
* Foam
* Many Markdown parsers

---

### Backlinks Section

Backlinks are rendered explicitly.

This ensures that:

* Graph traversal works both ways
* Tools without backlink indexing still show relationships
* Human readers can trace idea lineage

---

## Index Files

### Book Index

Each book folder contains an INDEX.md file.

This file provides:

* Book metadata
* Export date
* Ordered list of notes

It acts as a table of contents.

---

### Master Index

A README.md is created at the root level.

It explains:

* What the export contains
* How to use it
* How to import it into other tools
* The folder structure

This file is written for humans, not software.

---

## ExportAllNotes Strategy

When exporting the entire library:

* Notes are grouped by book
* Standalone notes are separated
* A single root index is generated

This allows partial imports and selective reuse.

---

## Export Result Model

### ExportResult

The export service returns structured results:

* Success with directory and files
* NoContent when nothing exists
* Error with a user-safe message

No exceptions leak into the UI layer.

---

## ExportViewModel

### Responsibility

The ViewModel bridges:

* UI intent
* Long-running export operations
* UI feedback

It owns no export logic.

---

### State Model

The export lifecycle is represented explicitly:

* Idle
* Exporting
* Success
* Error

This prevents ambiguous UI states.

---

### Progress Handling

Progress is currently coarse-grained.

This is a deliberate choice:

* File counts vary widely
* I/O progress is unreliable
* User trust matters more than fake precision

---

## Export Dialog UI

### ExportOptionsDialog

The user must explicitly choose:

* Export current book
* Export entire library

There are no default actions.

---

### ExportProgressDialog

During export:

* The dialog is modal
* Cancellation is disabled
* Feedback is minimal and calm

This prevents partial exports and corruption.

---

### ExportSuccessDialog

On success:

* File count is shown
* Directory path is displayed
* A direct “Open Folder” action is provided

This respects the user’s ownership of their data.

---

### ExportErrorDialog

Errors are:

* Plain language
* Non-technical
* Recoverable

No stack traces are exposed.

---

## Invariants Enforced by Export

### Invariant 1: No Data Loss

Every note that exists in the system is exportable.

There are no hidden states.

---

### Invariant 2: Graph Integrity

Links and backlinks are preserved explicitly.

The graph survives outside the app.

---

### Invariant 3: Tool Independence

No proprietary formats are used.

Markdown and YAML are intentional.

---

### Invariant 4: Human Readability

A person can open any file and understand it without tooling.

---

### Invariant 5: Repeatability

Exports are deterministic and repeatable.

Running export twice produces equivalent structure.

---

## Relationship to the Overall System

This phase completes the lifecycle:

* Library ingests sources
* Reader extracts meaning
* Notes refine understanding
* Links form a graph
* Export preserves the graph

Without export, the system would be incomplete.

---

## Summary

The Markdown Export Engine is the **moral contract** of the application.

It guarantees that:

* Knowledge is never trapped
* Ideas outlive software
* Structure survives migration
* Ownership remains with the user

At this point, the app stops being just a reader
and becomes a **knowledge instrument**.
