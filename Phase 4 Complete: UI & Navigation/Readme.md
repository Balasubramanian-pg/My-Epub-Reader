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

# EPUB Reader with Personal Knowledge Graph

## Phase 4 Library Screen Technical Documentation

## Purpose and Scope

This document explains the **Phase 4 Library Screen**, which functions as the **primary entry point into the system**.

This is not a file browser.
This is not a database table.

The Library Screen is a **curated, narrative overview of intellectual territory**. It answers, at a glance:

* What am I reading?
* What have I invested time in?
* What sources are producing ideas?
* Where should I go next?

This documentation is intended for engineers and designers working on **discovery, navigation, and long-term reading continuity**.

---

## Architectural Role of the Library Screen

### Position in the Stack

* Consumes enriched `BookWithMetadata` models
* Aggregates reading progress, tags, ratings, and analytics
* Does not manipulate notes or links directly
* Acts as the **orientation layer** for the entire application

Every other screen is entered from here, implicitly or explicitly.

---

## Conceptual Model

The Library Screen treats books as:

* **Sources of thought**
* **Ongoing commitments**
* **Completed investments**
* **Clusters of future ideas**

The UI is optimized for **recognition over recall**.

---

## LibraryViewModel

### Purpose

`LibraryViewModel` is the **curation engine** of the library.

It is responsible for:

* Loading all books with enriched metadata
* Deriving carousel groupings
* Surfacing reading analytics
* Managing destructive actions safely
* Maintaining UI loading and error state

---

### Core State Streams

#### All Books

* Canonical stream of all books
* Includes progress, tags, highlights, notes
* Acts as the source for all derived views

No carousel fetches independently.

---

#### Carousel Streams

Each carousel is a **derived semantic view**, not a separate dataset.

##### Continue Reading

* Books with partial progress
* Sorted by most recently read
* Reinforces continuity and momentum

##### Recently Added

* Sorted by import timestamp
* Encourages exploration of new material

##### Top Rated

* Books rated highly by the user
* Represents retrospective value judgment

##### Books by Tag

* Grouped by user-defined tags
* Requires at least two books per tag
* Avoids visual noise from singleton categories

---

#### Reading Analytics

The weekly minutes read metric:

* Provides lightweight feedback
* Reinforces reading as a habit
* Avoids gamification excess

It is informative, not competitive.

---

#### UI State

Tracks:

* Loading state
* Import dialog visibility
* Delete confirmation state
* Error messaging

All transient concerns are isolated here.

---

## LibraryScreen

### Role

`LibraryScreen` is the **Netflix-style browsing interface** for books.

Its design goals are:

* Fast visual scanning
* Low-friction navigation
* Progressive disclosure of detail
* Strong empty-state guidance

---

### Screen States

#### Initial Loading

* Full-screen indicator
* Only shown when no cached data exists

#### Empty Library

* Explicit explanation
* Clear call to action
* No dead ends

#### Populated Library

* Vertical stack of horizontal carousels
* Each carousel answers a different cognitive question

---

## LibraryTopBar

### Purpose

Provides **global context and lightweight analytics**.

---

### Elements

* Screen title
* Weekly reading badge
* Import affordance

The analytics badge is intentionally subtle.
It informs without pressuring.

---

## BookCarousel

### Conceptual Role

Each carousel is a **lens**, not a category.

Carousels answer questions like:

* What should I continue?
* What’s new?
* What did I value?
* How do I group my sources?

---

### Design Characteristics

* Horizontal scrolling
* Fixed card size
* Stable ordering
* Clear section headers

This mirrors proven discovery patterns while preserving meaning.

---

## BookCard

### Purpose

The BookCard is the **atomic unit of library browsing**.

It must communicate maximum signal in minimal space.

---

### Visual Hierarchy

1. Cover image
2. Progress indicator
3. Title
4. Author
5. Optional rating
6. Optional progress text
7. Badges

Each element earns its place.

---

### Progress Representation

Progress is shown in two ways:

* Linear overlay on the cover
* Percentage text below

Only displayed when meaningful.

---

### Badges

Badges represent **idea density**, not usage:

* Highlight count
* Note count

They answer:
“Has this book produced thought?”

---

## EmptyLibraryState

### Purpose

The empty state is a **first impression**.

It must:

* Explain what the library is
* Remove ambiguity
* Invite immediate action

---

### Design Intent

* Friendly, not apologetic
* Clear explanation
* Single primary action

Empty is treated as a beginning, not a failure.

---

## Destructive Actions

### DeleteBookDialog

Deletion is explained in plain language.

The dialog explicitly lists:

* Book file deletion
* Highlight deletion
* Highlight-linked note deletion
* Reading progress removal
* Preservation of standalone notes

This reinforces trust and predictability.

---

## Design Invariants Enforced by This Screen

### Invariant 1: The Library Is Curated, Not Flat

* No raw lists
* No overwhelming grids
* Meaningful grouping only

---

### Invariant 2: Progress Is Respected

* Continue Reading is prioritized
* Recent engagement matters
* Abandoned books fade naturally

---

### Invariant 3: Value Is Visible

* Ratings surface judgment
* Highlights and notes surface insight
* Badges signal depth

---

### Invariant 4: Actions Are Intentional

* Import is explicit
* Delete is gated
* Navigation is deliberate

---

### Invariant 5: Discovery Is Gentle

* Tags suggest organization
* Carousels invite exploration
* No algorithmic overreach

---

## Relationship to Other Screens

* **Library Screen** answers “Where am I?”
* **Book Details Screen** answers “What is this source?”
* **Reader Screen** answers “What am I reading right now?”
* **Notes Screen** answers “What ideas live here?”

The Library Screen is the **map**, not the territory.

---

## Summary

The Library Screen is the **front door of the system**.

It transforms a folder of EPUB files into:

* A reading history
* A set of commitments
* A growing intellectual landscape

By using curated carousels, meaningful metadata, and respectful analytics, this screen ensures users always know **where they are**, **what they’ve done**, and **what they might do next**.

It does not shout.
It quietly orients.
