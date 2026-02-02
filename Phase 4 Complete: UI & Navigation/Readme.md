# EPUB Reader with Personal Knowledge Graph

## Phase 4 Book Details Screen Technical Documentation

## Purpose and Scope

This document explains the **Phase 4 Book Details Screen**, which serves as the **authoritative, long-lived view of a book as a source**.

Unlike the reader screen, which is transient and immersive, the Book Details screen is reflective and managerial. It answers:

* What is this book?
* How much have I engaged with it?
* What ideas and structure have emerged from it?
* What can I do with it next?

This documentation is intended for engineers and designers working on **library-level cognition and metadata management**.

---

## Architectural Role of the Book Details Screen

### Position in the Stack

* Consumes enriched domain models (`BookWithMetadata`)
* Aggregates data from multiple repositories
* Does not perform graph mutations directly
* Acts as a **control surface** for book-level actions

This screen treats the book as a **source object**, not a container for notes.

---

## High-Level Responsibilities

The Book Details screen is responsible for:

* Displaying canonical book metadata
* Summarizing engagement and progress
* Managing tags and ratings
* Providing entry points into reading and thinking
* Previewing highlights and downstream artifacts
* Offering destructive actions with full clarity

It explicitly avoids:

* Editing note content
* Managing note links
* Navigating the full knowledge graph

---

## BookDetailsViewModel

### Purpose

`BookDetailsViewModel` is the **aggregation layer** for all book-related information.

It coordinates:

* Book metadata retrieval
* Highlight previews
* Rating and tag mutations
* UI dialog state
* Error and loading state

---

### Core State Streams

#### BookWithMetadata

This is the primary model driving the screen.

It includes:

* Book identity and bibliographic metadata
* Reading progress
* Highlight count
* Note count
* Tags

This ensures the screen can be rendered without secondary queries.

---

#### Recent Highlights

* Limited to a small preview window
* Provides immediate textual grounding
* Encourages return to reading

This supports rapid recall of why the book mattered.

---

#### Similar Books

This is a placeholder stream intended for future enhancement.

Potential similarity signals include:

* Shared tags
* Shared authors
* Shared reading patterns

The current implementation intentionally defers this logic.

---

#### UI State

Tracks:

* Loading indicator
* Dialog visibility
* Error messages

This separation keeps Compose logic deterministic.

---

## BookDetailsScreen

### Role

`BookDetailsScreen` is the **canonical metadata view** for a book.

It provides a vertically scrollable, narrative layout that moves from identity to action.

---

### Screen Lifecycle

* Book data is loaded on first composition
* UI reacts to repository updates
* Dialogs are layered declaratively
* Destructive actions always require confirmation

---

## Book Header

### Purpose

The header establishes **identity and status**.

It answers:

* What book is this?
* How far along am I?
* How much value has it produced?

---

### Cover Display

* Uses the actual cover when available
* Falls back to a styled placeholder when missing
* Always preserves aspect ratio

The cover is treated as a visual anchor, not decoration.

---

### Progress Overlay

If reading has begun:

* Progress percentage is shown directly on the cover
* Visibility is subtle but persistent
* Zero progress is intentionally hidden

This reinforces continuity.

---

### Core Metadata

Displayed prominently:

* Title
* Author
* Rating control

These are the most cognitively salient attributes.

---

### RatingBar

The rating control is:

* Explicit
* Discrete
* Immediately persisted

It represents a **post-reading judgment**, not a reading tool.

---

### Engagement Stats

Stat chips summarize:

* Highlight count
* Note count

These act as signals of **idea density**, not just usage.

---

## Action Buttons

### Purpose

These buttons represent **forward actions**, not configuration.

---

### Open Reader

* Context-aware label
* “Start Reading” vs “Continue Reading”
* Always the primary action

This reinforces reading as the core activity.

---

### Export Notes

* Enabled only when notes exist
* Represents outward knowledge flow
* Reinforces data ownership

Export is treated as a serious, intentional act.

---

## Tags Section

### Conceptual Role

Tags provide **library-level organization**, not graph structure.

They answer:

* How do I classify this source?
* How might I retrieve it later?

---

### Tag Display

* Compact, dismissible chips
* Immediate feedback on removal
* No hierarchy implied

Tags are lightweight and flexible by design.

---

### AddTagDialog

* Prevents empty tags
* Surfaces existing tags for reuse
* Avoids silent duplication

This supports emergent taxonomy rather than rigid classification.

---

## Metadata Section

### Purpose

Provides **bibliographic completeness**.

This section is deliberately understated.

---

### Displayed Fields

Conditionally shown based on availability:

* Publisher
* Publication date
* ISBN
* Language
* Page count
* Import date

This supports archival and export use cases.

---

## Highlights Preview

### Purpose

Highlights act as **evidence of engagement**.

This section reminds the user what stood out.

---

### Design Principles

* Limited to recent items
* Text-first presentation
* Clear path to “View all”

Highlights are treated as memory anchors.

---

## Similar Books Section

### Current Status

This section is scaffolded for future expansion.

It is intended to support:

* Discovery
* Serendipity
* Pattern recognition

Its absence does not block core workflows.

---

## Destructive Actions

### Delete Book Confirmation

Deletion is:

* Explicit
* Irreversible
* Fully explained

The dialog makes clear that:

* Highlights will be deleted
* Notes will be deleted
* Graph structure will be affected

This preserves user trust.

---

## Design Invariants Enforced by This Screen

### Invariant 1: Books Are Sources

* Notes are summarized, not edited
* Graph navigation happens elsewhere
* The book is never reduced to a container

---

### Invariant 2: Metadata Is Stable

* Most fields are read-only
* Changes are deliberate
* Reloads reflect canonical state

---

### Invariant 3: Actions Are Intentional

* Reading, exporting, deleting are explicit
* No accidental mutations
* No hidden side effects

---

### Invariant 4: Engagement Is Visible

* Progress is surfaced
* Highlights are previewed
* Notes are counted

This reinforces the value created from the book.

---

## Summary

The Book Details screen is the **memory palace for a source**.

* The reader is where ideas are born
* The notes system is where ideas connect
* The book details screen is where value is assessed

By separating **source management** from **idea navigation**, this screen ensures that books remain stable anchors in a system built for long-term thinking.

This is not a settings page.
It is a reflection surface for intellectual investment.

