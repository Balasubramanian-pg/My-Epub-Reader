# EPUB Reader with Personal Knowledge Graph

## Phase 3 Notes System Dialogs and Utilities Technical Documentation

## Purpose and Scope

This document explains the **Phase 3 Notes System UI components**, focusing on dialogs, utilities, and supporting interaction flows that enable **note creation, linking, deletion, and graph hygiene**.

This layer is where **ideas are explicitly shaped and connected**.

Unlike the reader UI, this layer is not about immersion. It is about **deliberate cognition**, clarity, and structural intent.

This documentation is intended for UI engineers, product designers, and developers extending the knowledge graph experience.

---

## Architectural Role of the Notes UI Layer

### Position in the Stack

* Consumes domain models from repositories
* Emits explicit user intent
* Never mutates graph structure directly
* Delegates all persistence to repositories

This layer is intentionally declarative.
It asks questions, presents options, and collects decisions.

---

## High-Level Responsibilities

The Notes UI layer is responsible for:

* Standalone note creation
* Explicit note-to-note linking
* Safe deletion with user awareness
* Utility formatting for temporal context
* Supporting future graph visualization

It explicitly avoids:

* Automatic linking
* Implicit graph mutations
* Business rule enforcement

---

## CreateNoteDialog

### Purpose

Enables **thinking-first note creation** without requiring a book or highlight.

This dialog formalizes the idea that notes are **independent cognitive objects**.

---

### Design Intent

* Encourage standalone ideation
* Set correct mental model early
* Reduce friction for capturing thoughts

---

### Interaction Rules

* Title and body are mandatory
* Save is disabled until content is meaningful
* Cancellation is always safe

The dialog explains what makes this note different from annotations.

---

## LinkNoteDialog

### Conceptual Importance

This is one of the most critical components in the entire system.

It is the **manual edge constructor** of the knowledge graph.

---

### Purpose

Allows users to explicitly connect one note to another.

Links are:

* Intentional
* Directional internally
* Bidirectional in experience

---

### Search-Based Linking

The dialog includes:

* Live search over available notes
* Title and body matching
* Immediate visual feedback

This design scales gracefully as the note corpus grows.

---

### LinkableNoteCard

### Purpose

Represents a candidate note in the linking process.

### Included Context

* Title
* Body preview
* Creation date

This ensures users understand *what* they are linking, not just *that* they are linking.

---

### Design Safeguards

* Current note is excluded upstream
* Clicking creates a single explicit link
* No batch linking

Every link is a conscious act.

---

## DeleteConfirmationDialog

### Purpose

Prevents accidental destruction of ideas and structure.

---

### Design Philosophy

Deletion is:

* Explicit
* Informed
* Reversible only by recreation

---

### User Education

The dialog explains:

* The note will be deleted
* Links will be removed
* Other notes remain intact

This preserves trust in the system.

---

## Utility Functions

### formatDate

### Purpose

Provides **relative time context** for notes.

Humans reason better with “recent” than with timestamps.

---

### Behavior

* Short-term events are relative
* Older events are absolute
* Formatting adapts automatically

This supports scanning and memory recall.

---

### formatFullDate

Provides precise timestamps for:

* Metadata views
* Debugging
* Export contexts

---

## Preview Data

### Purpose

Supports Compose previews and design iteration.

---

### Characteristics

* Representative note content
* Mixed standalone and book-linked notes
* Realistic timestamps

This enables meaningful UI testing without live data.

---

## GraphVisualizationScreen (Stretch Goal)

### Purpose

Placeholder for future **interactive graph exploration**.

---

### Intended Capabilities

Future implementations may include:

* Force-directed layouts
* Canvas-based interaction
* WebView-based D3 rendering
* Custom Compose drawing

This screen is intentionally deferred to avoid premature complexity.

---

## Conceptual Integrity Checklist

This layer completes the cognitive loop.

### Enforced Principles

#### Notes Are First-Class

* Standalone creation exists
* No book dependency
* No highlight requirement

---

#### Links Are Explicit

* No automatic graph mutation
* Search-assisted, user-confirmed linking
* One link per action

---

#### Backlinks Are Guaranteed

* UI always reflects reverse connections
* No additional logic required
* Memory emerges naturally

---

#### Search Suggests, Structure Persists

* FTS helps rediscover ideas
* Links preserve meaning over time

---

#### Sources Are Preserved

* Notes remember highlights
* Books remain contextual anchors
* Navigation remains reversible

---

## Supported Usage Flows

### Reading-First Flow

* Read book
* Highlight passage
* Create note
* Link to existing ideas

---

### Thinking-First Flow

* Create standalone note
* Explore related notes
* Link later

---

### Linking-Later Flow

* Browse notes
* Discover resonance
* Create explicit link

---

### Rediscovery Flow

* Search ideas
* Land on note
* Follow backlinks
* Reconstruct thought lineage

---

## Summary

The Notes System UI layer is where the **knowledge graph becomes intentional**.

* Creation is deliberate
* Linking is explicit
* Deletion is respectful
* Context is always visible

This layer transforms notes from text into structure
and structure into memory.

By the time a graph visualization exists,
the graph itself will already be correct.

