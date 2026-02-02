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

# EPUB Reader with Personal Knowledge Graph

## Phase 3 Knowledge Graph Notes System Technical Documentation

## Purpose and Scope

This document explains the **Phase 3 Knowledge Graph Notes System**, which brings together **notes, links, backlinks, discovery, and navigation** into a cohesive cognitive interface.

This layer is where the system fully transitions from “note-taking” to **graph-based thinking**.

It provides:

* Notes as first-class entities
* Explicit bidirectional linking
* Backlinks as a memory mechanism
* Related idea discovery via FTS
* Cross-book navigation and source tracing

This documentation is intended for engineers and designers working on the **core thinking experience** of the application.

---

## Architectural Role of the Knowledge Graph UI

### Position in the Stack

* Consumes `NoteWithLinks` domain models
* Delegates mutations to `NoteRepository`
* Reflects graph structure without inferring meaning
* Acts as a **graph navigator**, not a graph editor

The UI never invents structure.
It reveals structure that the user has created.

---

## High-Level Responsibilities

This layer is responsible for:

* Browsing the full note graph
* Filtering and searching notes
* Selecting and inspecting a note’s neighborhood
* Creating, removing, and inspecting links
* Visualizing backlinks as memory
* Bridging notes back to source material

It explicitly avoids:

* Automatic linking
* Implicit graph expansion
* Background graph mutation

---

## NotesViewModel

### Purpose

`NotesViewModel` is the **knowledge graph orchestrator**.

It coordinates:

* Full graph loading
* Search and filter logic
* Selection and detail loading
* Link creation and removal
* Related note discovery

It is the only place where graph-wide state is assembled.

---

### Core State Streams

#### All Notes

* Emits every note with its full graph context
* Includes outgoing links and backlinks
* Acts as the canonical in-memory graph snapshot

---

#### Selected Note

* Represents the currently focused node
* Drives the detail pane
* Always includes full neighborhood context

---

#### UI State

Encapsulates transient UI concerns:

* Dialog visibility
* Related note suggestions
* Deletion targets
* Error states

This keeps Compose logic predictable.

---

### Search and Filtering

Search and filter logic is composed reactively using:

* All notes
* Search query
* Filter mode

Filtering supports:

* All notes
* Standalone ideas
* Notes with links
* Notes created from highlights

This enables multiple cognitive entry points into the graph.

---

## Graph Mutations

### Standalone Note Creation

* Initiated via UI dialog
* Delegated to repository
* Immediately reflected in graph state

This supports thinking-first workflows.

---

### Note Updates

* Edits are explicit
* Modified timestamps are updated
* Detail pane refreshes automatically

---

### Note Deletion

Deletion guarantees:

* Explicit user confirmation
* Automatic link cleanup via cascade
* Safe graph re-stabilization

The UI never deletes silently.

---

### Linking Notes (Core Operation)

This is the most critical mutation.

When linking notes:

* Both notes must exist
* Self-links are forbidden upstream
* Links are explicit and directional
* Backlinks appear automatically

The UI reflects structure, but the repository enforces invariants.

---

### Unlinking Notes

Unlinking removes a single edge.

* No cascading deletions
* Backlinks update automatically
* Graph remains consistent

---

## NotesScreen

### Role

`NotesScreen` is the **main graph workspace**.

It provides:

* A two-pane layout
* Global search and filtering
* Direct access to creation and linking

---

### Two-Pane Design

#### Left Pane: Notes List

* Displays filtered notes
* Shows link counts and origin chips
* Highlights current selection

This pane answers:
“What ideas do I have?”

---

#### Right Pane: Note Detail

* Displays full note content
* Shows links, backlinks, and suggestions
* Enables editing, linking, and deletion

This pane answers:
“How does this idea relate to others?”

---

## NotesTopBar

### Purpose

Provides **graph-wide orientation and control**.

Features:

* Live search
* Filter dropdown
* Clear, visible state

Search is always scoped to the entire graph.

---

## NotesListPane

### Empty State

When no notes exist, the UI:

* Explains what notes are
* Encourages first creation
* Avoids blank or confusing screens

---

### NoteListItem

Each list item surfaces:

* Title and body preview
* Link count
* Origin chip:

  * Standalone
  * From highlight
  * From book
* Creation date

This allows rapid scanning of the idea space.

---

## NoteDetailPane

### Conceptual Role

This is the **knowledge graph navigator**.

It shows a note **in context**, not in isolation.

---

### Editing Mode

* Explicit toggle
* Draft state is local
* Save and cancel are clear

Edits never occur implicitly.

---

### Source Context

If a note originates from a highlight:

* Highlight text is shown
* Book metadata is displayed
* Jump-to-passage is available

This preserves intellectual provenance.

---

### Linked Notes Section

Shows outgoing links.

* Represents intentional references
* Allows unlinking
* Enables forward navigation

---

### Backlinks Section (Memory Mechanism)

Shows incoming links.

This is critical.

Backlinks answer:
“Where has this idea been used?”

They are:

* Automatic
* Non-editable
* Central to long-term recall

---

### Related Notes (FTS-Based Suggestions)

These are **suggestions, not structure**.

* Based on textual similarity
* Do not imply meaning
* Require explicit linking to persist

This preserves trust in the graph.

---

### Metadata

Displays:

* Creation date
* Modification date
* Stable identifier

This supports auditing, export, and debugging.

---

## Graph Navigation Components

### GraphSection

Used for:

* Linked notes
* Backlinks

Provides:

* Count visibility
* Clear iconography
* Uniform navigation affordances

---

### LinkedNoteCard

A minimal representation of a neighboring node.

Supports:

* Click-through navigation
* Optional unlinking
* Preview-level context

---

### RelatedNotesSection

Visually distinguished from explicit links.

This reinforces the conceptual boundary between:

* Suggestions
* Commitments

---

## SourceCard

### Purpose

Bridges the knowledge graph back to reading.

Provides:

* Highlight context
* Book identity
* Direct navigation to the source passage

This closes the loop between reading and thinking.

---

## Design Invariants Enforced by This Layer

### Invariant 1: Notes Are First-Class

* Standalone creation
* Independent browsing
* Equal visual weight

---

### Invariant 2: Links Are Intentional

* Explicit user action
* No automation
* Clear affordances

---

### Invariant 3: Backlinks Are Non-Negotiable

* Always visible
* Always accurate
* Never editable directly

---

### Invariant 4: Discovery Is Separate from Structure

* FTS suggests
* User decides
* Graph remembers

---

### Invariant 5: Sources Are Preserved

* Highlights remain visible
* Books remain navigable
* Context is never lost

---

## Summary

This screen is the **center of gravity** for the entire system.

* The reader produces ideas
* The notes system structures them
* The graph preserves meaning over time

By making links explicit and backlinks unavoidable,
this layer turns notes into memory and memory into insight.

When users return months later,
this is the screen that will remember for them.
