/**
 * Repository Layer - Business Logic & Graph Integrity
 * 
 * This layer enforces the conceptual model:
 * - Books are sources
 * - Notes are ideas (first-class)
 * - Links create meaning
 * - Backlinks provide memory
 */

package com.epreader.data.repository

import com.epreader.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ============================================================================
// DOMAIN MODELS (What the UI layer sees)
// ============================================================================

/**
 * Enriched book model with metadata
 */
data class BookWithMetadata(
    val book: Book,
    val highlightCount: Int,
    val noteCount: Int,
    val tags: List<Tag>,
    val progress: ReadingProgress?
)

/**
 * Note with its graph context
 * Critical: This shows both outgoing links AND backlinks
 */
data class NoteWithLinks(
    val note: Note,
    val linkedNotes: List<Note>,      // Outgoing: notes this note links to
    val backlinks: List<Note>,         // Incoming: notes that link to this note
    val highlight: Highlight?,         // Optional grounding highlight
    val book: Book?                    // Optional source book
)

/**
 * Search result with context
 */
sealed class SearchResult {
    data class BookPassage(
        val bookId: Long,
        val bookTitle: String,
        val snippet: String,
        val cfiLocator: String
    ) : SearchResult()
    
    data class NoteResult(
        val note: Note,
        val snippet: String
    ) : SearchResult()
}

// ============================================================================
// BOOK REPOSITORY
// ============================================================================

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val highlightDao: HighlightDao,
    private val noteDao: NoteDao,
    private val tagDao: TagDao,
    private val progressDao: ReadingProgressDao,
    private val searchDao: SearchDao
) {
    
    /**
     * Get all books with enriched metadata for UI
     */
    fun getAllBooksWithMetadata(): Flow<List<BookWithMetadata>> {
        return bookDao.getAllBooksFlow().map { books ->
            books.map { book -> getBookWithMetadata(book.id) }
        }
    }
    
    /**
     * Get single book with full context
     */
    suspend fun getBookWithMetadata(bookId: Long): BookWithMetadata {
        val book = bookDao.getBookById(bookId) 
            ?: throw IllegalArgumentException("Book not found: $bookId")
        
        return BookWithMetadata(
            book = book,
            highlightCount = highlightDao.getHighlightCountByBook(bookId),
            noteCount = noteDao.getNotesByBook(bookId).first().size,
            tags = tagDao.getTagsByBook(bookId).first(),
            progress = progressDao.getProgressByBook(bookId)
        )
    }
    
    /**
     * Import new book - this is the entry point for EPUB processing
     */
    suspend fun importBook(
        title: String,
        author: String,
        filePath: String,
        coverPath: String? = null,
        drmStatus: DrmStatus = DrmStatus.UNKNOWN,
        chapters: List<Chapter> = emptyList()
    ): Long {
        val book = Book(
            title = title,
            author = author,
            filePath = filePath,
            coverPath = coverPath,
            drmStatus = drmStatus
        )
        
        val bookId = bookDao.insertBook(book)
        
        // Insert chapters if provided
        if (chapters.isNotEmpty()) {
            val chaptersWithBookId = chapters.map { it.copy(bookId = bookId) }
            chapterDao.insertChapters(chaptersWithBookId)
        }
        
        // Initialize reading progress
        progressDao.insertOrUpdateProgress(
            ReadingProgress(bookId = bookId, lastCfi = "", progressPercent = 0f)
        )
        
        return bookId
    }
    
    /**
     * Delete book - CASCADE will handle cleanup
     * But FTS index must be manually deleted
     */
    suspend fun deleteBook(bookId: Long) {
        val book = bookDao.getBookById(bookId) ?: return
        searchDao.deleteBookIndex(bookId)
        bookDao.deleteBook(book)
    }
    
    /**
     * Update book rating
     */
    suspend fun updateRating(bookId: Long, rating: Float) {
        val book = bookDao.getBookById(bookId) ?: return
        bookDao.updateBook(book.copy(rating = rating))
    }
    
    /**
     * Tag management
     */
    suspend fun addTagToBook(bookId: Long, tagName: String) {
        // Get or create tag
        val tag = tagDao.getTagByName(tagName) ?: run {
            val newTagId = tagDao.insertTag(Tag(name = tagName))
            Tag(id = newTagId, name = tagName)
        }
        
        tagDao.addTagToBook(BookTag(bookId = bookId, tagId = tag.id))
    }
    
    suspend fun removeTagFromBook(bookId: Long, tagId: Long) {
        tagDao.removeTagFromBook(BookTag(bookId = bookId, tagId = tagId))
    }
    
    /**
     * Index book text for FTS
     * This should be called during EPUB import after text extraction
     */
    suspend fun indexBookText(
        bookId: Long,
        chapterId: Long?,
        textContent: String,
        cfiLocator: String
    ) {
        // Only index if book is not DRM-protected
        val book = bookDao.getBookById(bookId)
        if (book?.drmStatus == DrmStatus.DRM_PROTECTED) {
            return // Skip indexing for DRM books
        }
        
        searchDao.indexBookText(
            BookTextFts(
                rowid = 0, // Auto-generated
                bookId = bookId,
                chapterId = chapterId,
                textContent = textContent,
                cfiLocator = cfiLocator
            )
        )
    }
}

// ============================================================================
// HIGHLIGHT REPOSITORY
// ============================================================================

@Singleton
class HighlightRepository @Inject constructor(
    private val highlightDao: HighlightDao
) {
    
    fun getHighlightsByBook(bookId: Long): Flow<List<Highlight>> {
        return highlightDao.getHighlightsByBook(bookId)
    }
    
    suspend fun createHighlight(
        bookId: Long,
        chapterId: Long?,
        cfiOrRange: String,
        text: String,
        color: String = "#FFEB3B"
    ): Long {
        return highlightDao.insertHighlight(
            Highlight(
                bookId = bookId,
                chapterId = chapterId,
                cfiOrRange = cfiOrRange,
                text = text,
                color = color
            )
        )
    }
    
    suspend fun deleteHighlight(highlightId: Long) {
        val highlight = highlightDao.getHighlightById(highlightId) ?: return
        highlightDao.deleteHighlight(highlight)
    }
    
    suspend fun updateHighlightColor(highlightId: Long, color: String) {
        val highlight = highlightDao.getHighlightById(highlightId) ?: return
        highlightDao.updateHighlight(highlight.copy(color = color))
    }
}

// ============================================================================
// NOTE REPOSITORY - KNOWLEDGE GRAPH CORE
// ============================================================================

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val noteLinkDao: NoteLinkDao,
    private val highlightDao: HighlightDao,
    private val bookDao: BookDao,
    private val searchDao: SearchDao
) {
    
    /**
     * Get all notes with their graph context
     */
    fun getAllNotesWithLinks(): Flow<List<NoteWithLinks>> {
        return noteDao.getAllNotesFlow().map { notes ->
            notes.map { note -> getNoteWithLinks(note.id) }
        }
    }
    
    /**
     * Get single note with full graph context
     * CRITICAL: This includes backlinks (the memory mechanism)
     */
    suspend fun getNoteWithLinks(noteId: Long): NoteWithLinks {
        val note = noteDao.getNoteById(noteId)
            ?: throw IllegalArgumentException("Note not found: $noteId")
        
        return NoteWithLinks(
            note = note,
            linkedNotes = noteDao.getLinkedNotes(noteId).first(),
            backlinks = noteDao.getBacklinks(noteId).first(),
            highlight = note.highlightId?.let { highlightDao.getHighlightById(it) },
            book = note.bookId?.let { bookDao.getBookById(it) }
        )
    }
    
    /**
     * Create standalone note (thinking-first flow)
     * This is critical: notes can exist without books
     */
    suspend fun createStandaloneNote(title: String, body: String): Long {
        val noteId = noteDao.insertNote(
            Note(title = title, body = body)
        )
        
        // Index for FTS
        indexNote(noteId, title, body)
        
        return noteId
    }
    
    /**
     * Create note from highlight (reading-first flow)
     */
    suspend fun createNoteFromHighlight(
        highlightId: Long,
        title: String,
        body: String
    ): Long {
        val highlight = highlightDao.getHighlightById(highlightId)
            ?: throw IllegalArgumentException("Highlight not found: $highlightId")
        
        val noteId = noteDao.insertNote(
            Note(
                title = title,
                body = body,
                bookId = highlight.bookId,
                highlightId = highlightId
            )
        )
        
        // Index for FTS
        indexNote(noteId, title, body)
        
        return noteId
    }
    
    /**
     * Update note content
     */
    suspend fun updateNote(noteId: Long, title: String, body: String) {
        val note = noteDao.getNoteById(noteId) ?: return
        
        noteDao.updateNote(
            note.copy(
                title = title,
                body = body,
                modifiedAt = System.currentTimeMillis()
            )
        )
        
        // Re-index
        indexNote(noteId, title, body)
    }
    
    /**
     * CRITICAL: Create bidirectional link between notes
     * This is the core of the knowledge graph
     */
    suspend fun linkNotes(fromNoteId: Long, toNoteId: Long) {
        // Validate both notes exist
        noteDao.getNoteById(fromNoteId) 
            ?: throw IllegalArgumentException("Source note not found: $fromNoteId")
        noteDao.getNoteById(toNoteId)
            ?: throw IllegalArgumentException("Target note not found: $toNoteId")
        
        // Prevent self-linking
        if (fromNoteId == toNoteId) {
            throw IllegalArgumentException("Cannot link note to itself")
        }
        
        // Check if link already exists
        if (noteLinkDao.linkExists(fromNoteId, toNoteId) > 0) {
            return // Link already exists, do nothing
        }
        
        // Create directional link
        noteLinkDao.insertLink(
            NoteLink(fromNoteId = fromNoteId, toNoteId = toNoteId)
        )
    }
    
    /**
     * Remove link between notes
     */
    suspend fun unlinkNotes(fromNoteId: Long, toNoteId: Long) {
        noteLinkDao.deleteLink(
            NoteLink(fromNoteId = fromNoteId, toNoteId = toNoteId)
        )
    }
    
    /**
     * Delete note - cleanup links automatically via CASCADE
     */
    suspend fun deleteNote(noteId: Long) {
        val note = noteDao.getNoteById(noteId) ?: return
        
        // Clean up FTS index
        searchDao.deleteNoteIndex(noteId)
        
        // Delete note (CASCADE will handle links)
        noteDao.deleteNote(note)
    }
    
    /**
     * Get related notes based on FTS similarity
     * This supplements explicit links with suggestions
     */
    suspend fun getRelatedNotes(noteId: Long, limit: Int = 5): List<Note> {
        val note = noteDao.getNoteById(noteId) ?: return emptyList()
        
        // Simple approach: search for key terms from note body
        val searchTerms = extractKeyTerms(note.body)
        if (searchTerms.isEmpty()) return emptyList()
        
        val results = searchDao.searchNotes(searchTerms, limit = limit + 1)
        
        // Filter out the current note and convert to Note objects
        return results
            .filter { it.noteId != noteId }
            .take(limit)
            .mapNotNull { noteDao.getNoteById(it.noteId) }
    }
    
    /**
     * Index note for FTS
     */
    private suspend fun indexNote(noteId: Long, title: String, body: String) {
        searchDao.indexNote(
            NoteFts(
                rowid = noteId,
                noteId = noteId,
                title = title,
                body = body
            )
        )
    }
    
    /**
     * Extract key terms for similarity search
     * Simple implementation - can be enhanced
     */
    private fun extractKeyTerms(text: String): String {
        return text
            .split("\\s+".toRegex())
            .filter { it.length > 4 } // Only words longer than 4 chars
            .distinct()
            .take(5)
            .joinToString(" OR ")
    }
}

// ============================================================================
// SEARCH REPOSITORY
// ============================================================================

@Singleton
class SearchRepository @Inject constructor(
    private val searchDao: SearchDao,
    private val bookDao: BookDao,
    private val noteDao: NoteDao
) {
    
    /**
     * Global search across books and notes
     */
    suspend fun globalSearch(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        
        // Search book content
        val bookResults = searchDao.searchBookContent(query)
        results.addAll(bookResults.map { 
            SearchResult.BookPassage(
                bookId = it.bookId,
                bookTitle = it.bookTitle,
                snippet = it.textContent.take(200), // Truncate
                cfiLocator = it.cfiLocator
            )
        })
        
        // Search notes
        val noteResults = searchDao.searchNotes(query)
        results.addAll(noteResults.map {
            val note = noteDao.getNoteById(it.noteId)
            SearchResult.NoteResult(
                note = note ?: return@map null,
                snippet = it.body.take(200)
            )
        }.filterNotNull())
        
        return results
    }
}

// ============================================================================
// READING SESSION REPOSITORY (Analytics)
// ============================================================================

@Singleton
class ReadingSessionRepository @Inject constructor(
    private val sessionDao: ReadingSessionDao,
    private val progressDao: ReadingProgressDao
) {
    
    /**
     * Start new reading session
     */
    suspend fun startSession(bookId: Long): Long {
        return sessionDao.insertSession(
            ReadingSession(
                bookId = bookId,
                startTime = System.currentTimeMillis()
            )
        )
    }
    
    /**
     * End reading session and update progress
     */
    suspend fun endSession(
        sessionId: Long,
        bookId: Long,
        currentCfi: String,
        progressPercent: Float
    ) {
        val endTime = System.currentTimeMillis()
        
        // Calculate duration (simple approach)
        // In production: use more sophisticated active time tracking
        sessionDao.updateSession(
            ReadingSession(
                id = sessionId,
                bookId = bookId,
                startTime = endTime - (5 * 60 * 1000), // Placeholder
                endTime = endTime,
                durationMinutes = 5 // Placeholder
            )
        )
        
        // Update reading progress
        progressDao.insertOrUpdateProgress(
            ReadingProgress(
                bookId = bookId,
                lastCfi = currentCfi,
                progressPercent = progressPercent,
                lastReadAt = endTime
            )
        )
    }
    
    /**
     * Get minutes read this week
     */
    suspend fun getMinutesReadThisWeek(): Int {
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        return sessionDao.getTotalMinutesRead(weekAgo)
    }
    
    /**
     * Mark book as complete
     */
    suspend fun markBookComplete(bookId: Long) {
        progressDao.markBookComplete(bookId)
    }
}

// ============================================================================
// GRAPH INTEGRITY RULES (Conceptual Documentation)
// ============================================================================

/**
 * KNOWLEDGE GRAPH INVARIANTS
 * 
 * These rules must ALWAYS hold true:
 * 
 * 1. Notes are independent entities
 *    - A note can exist without a book
 *    - A note can exist without a highlight
 *    
 * 2. Links are explicit and human-curated
 *    - No auto-linking without user confirmation
 *    - System may suggest, user must decide
 *    
 * 3. Backlinks are automatic consequences
 *    - For every A → B link
 *    - B must show A in backlinks
 *    
 * 4. Highlights provide grounding
 *    - Highlights anchor notes to source text
 *    - But notes are not "just annotations"
 *    
 * 5. Books are sources, not containers
 *    - Notes transcend individual books
 *    - Cross-book linking is fundamental
 *    
 * 6. FTS suggests, graph remembers
 *    - FTS finds candidates
 *    - Explicit links preserve meaning
 *    
 * 7. Everything is local and exportable
 *    - No data leaves the device
 *    - Export preserves graph structure
 */
