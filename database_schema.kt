/**
 * EPUB Reader with Personal Knowledge Graph - Database Layer
 * Phase 1: Complete Room Schema with FTS Support
 * 
 * This file contains all core entities, DAOs, and database configuration
 * for the local-first EPUB reader with bidirectional note linking.
 */

package com.epreader.data.local

import androidx.room.*
import androidx.room.FtsOptions.Order
import kotlinx.coroutines.flow.Flow

// ============================================================================
// CORE ENTITIES
// ============================================================================

/**
 * Book Entity - Represents an EPUB source
 * Books are sources, not silos. They own highlights but notes are independent.
 */
@Entity(
    tableName = "books",
    indices = [
        Index(value = ["title", "author"]),
        Index(value = ["import_timestamp"])
    ]
)
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "author")
    val author: String,
    
    @ColumnInfo(name = "isbn")
    val isbn: String? = null,
    
    @ColumnInfo(name = "publisher")
    val publisher: String? = null,
    
    @ColumnInfo(name = "pub_date")
    val pubDate: String? = null,
    
    @ColumnInfo(name = "file_path")
    val filePath: String, // Path to EPUB file in app-specific storage
    
    @ColumnInfo(name = "cover_path")
    val coverPath: String? = null,
    
    @ColumnInfo(name = "rating")
    val rating: Float = 0f, // 0-5 stars
    
    @ColumnInfo(name = "import_timestamp")
    val importTimestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "drm_status")
    val drmStatus: DrmStatus = DrmStatus.UNKNOWN,
    
    @ColumnInfo(name = "total_pages")
    val totalPages: Int? = null,
    
    @ColumnInfo(name = "language")
    val language: String? = null
)

enum class DrmStatus {
    NONE,           // DRM-free, full features available
    DRM_PROTECTED,  // DRM detected, limited features
    UNKNOWN         // Not yet checked
}

/**
 * Chapter Entity - Represents book structure
 * Used for navigation and progress tracking
 */
@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["book_id", "chapter_index"])]
)
data class Chapter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "book_id")
    val bookId: Long,
    
    @ColumnInfo(name = "chapter_index")
    val chapterIndex: Int,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "cfi_or_href")
    val cfiOrHref: String // Readium locator for deep linking
)

/**
 * Highlight Entity - Context node in knowledge graph
 * Represents quoted passages that ground thinking
 */
@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Chapter::class,
            parentColumns = ["id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["chapter_id"]),
        Index(value = ["created_at"])
    ]
)
data class Highlight(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "book_id")
    val bookId: Long,
    
    @ColumnInfo(name = "chapter_id")
    val chapterId: Long? = null,
    
    @ColumnInfo(name = "cfi_or_range")
    val cfiOrRange: String, // Precise location locator
    
    @ColumnInfo(name = "text")
    val text: String, // The actual highlighted text
    
    @ColumnInfo(name = "color")
    val color: String = "#FFEB3B", // Hex color code
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Note Entity - PRIMARY NODE in knowledge graph
 * Notes are ideas, not just annotations. They are first-class citizens.
 * 
 * Critical: Notes can exist without books (thinking-first flow)
 */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Highlight::class,
            parentColumns = ["id"],
            childColumns = ["highlight_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["highlight_id"]),
        Index(value = ["created_at"]),
        Index(value = ["modified_at"])
    ]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "body")
    val body: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long = System.currentTimeMillis(),
    
    // Optional references - notes can be standalone
    @ColumnInfo(name = "book_id")
    val bookId: Long? = null,
    
    @ColumnInfo(name = "highlight_id")
    val highlightId: Long? = null
)

/**
 * NoteLink Entity - CORE EDGE in knowledge graph
 * Represents explicit, human-curated relationships between ideas
 * 
 * Critical: This is directional. UI must show backlinks via reverse lookup.
 */
@Entity(
    tableName = "note_links",
    primaryKeys = ["from_note_id", "to_note_id"],
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["from_note_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["to_note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["from_note_id"]),
        Index(value = ["to_note_id"])
    ]
)
data class NoteLink(
    @ColumnInfo(name = "from_note_id")
    val fromNoteId: Long,
    
    @ColumnInfo(name = "to_note_id")
    val toNoteId: Long,
    
    @ColumnInfo(name = "link_type")
    val linkType: String = "relates_to", // Future: semantic types
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Tag Entity - For categorization
 */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * BookTag - Many-to-many relationship
 */
@Entity(
    tableName = "book_tags",
    primaryKeys = ["book_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["tag_id"])
    ]
)
data class BookTag(
    @ColumnInfo(name = "book_id")
    val bookId: Long,
    
    @ColumnInfo(name = "tag_id")
    val tagId: Long
)

/**
 * ReadingProgress - Track last read location per book
 */
@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["book_id"], unique = true)]
)
data class ReadingProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "book_id")
    val bookId: Long,
    
    @ColumnInfo(name = "last_cfi")
    val lastCfi: String, // Readium CFI locator
    
    @ColumnInfo(name = "progress_percent")
    val progressPercent: Float = 0f, // 0-100
    
    @ColumnInfo(name = "last_read_at")
    val lastReadAt: Long = System.currentTimeMillis()
)

/**
 * ReadingSession - For analytics tracking
 */
@Entity(
    tableName = "reading_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["start_time"])
    ]
)
data class ReadingSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "book_id")
    val bookId: Long,
    
    @ColumnInfo(name = "start_time")
    val startTime: Long,
    
    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,
    
    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int = 0
)

// ============================================================================
// FTS (FULL-TEXT SEARCH) ENTITIES
// ============================================================================

/**
 * FTS Virtual Table for Book Content
 * Critical: This enables cross-book idea discovery
 * 
 * Populated during EPUB import by extracting full chapter text
 */
@Fts4(contentEntity = Book::class)
@Entity(tableName = "fts_books_text")
data class BookTextFts(
    @ColumnInfo(name = "rowid")
    val rowid: Long,
    
    @ColumnInfo(name = "book_id")
    val bookId: Long,
    
    @ColumnInfo(name = "chapter_id")
    val chapterId: Long?,
    
    @ColumnInfo(name = "text_content")
    val textContent: String,
    
    @ColumnInfo(name = "cfi_locator")
    val cfiLocator: String // For jump-to-passage
)

/**
 * FTS Virtual Table for Notes
 * Enables searching across all notes
 */
@Fts4(contentEntity = Note::class)
@Entity(tableName = "fts_notes")
data class NoteFts(
    @ColumnInfo(name = "rowid")
    val rowid: Long,
    
    @ColumnInfo(name = "note_id")
    val noteId: Long,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "body")
    val body: String
)

// ============================================================================
// DAOs (Data Access Objects)
// ============================================================================

@Dao
interface BookDao {
    
    @Query("SELECT * FROM books ORDER BY import_timestamp DESC")
    fun getAllBooksFlow(): Flow<List<Book>>
    
    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): Book?
    
    @Query("""
        SELECT * FROM books 
        WHERE id IN (
            SELECT book_id FROM reading_progress 
            WHERE progress_percent < 100 
            ORDER BY last_read_at DESC
        )
        LIMIT 10
    """)
    fun getContinueReading(): Flow<List<Book>>
    
    @Query("SELECT * FROM books ORDER BY import_timestamp DESC LIMIT :limit")
    fun getRecentlyAdded(limit: Int = 10): Flow<List<Book>>
    
    @Query("SELECT * FROM books WHERE rating >= :minRating ORDER BY rating DESC")
    fun getTopRated(minRating: Float = 4.0f): Flow<List<Book>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long
    
    @Update
    suspend fun updateBook(book: Book)
    
    @Delete
    suspend fun deleteBook(book: Book)
    
    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int
}

@Dao
interface ChapterDao {
    
    @Query("SELECT * FROM chapters WHERE book_id = :bookId ORDER BY chapter_index")
    fun getChaptersByBook(bookId: Long): Flow<List<Chapter>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<Chapter>)
    
    @Query("DELETE FROM chapters WHERE book_id = :bookId")
    suspend fun deleteChaptersByBook(bookId: Long)
}

@Dao
interface HighlightDao {
    
    @Query("SELECT * FROM highlights WHERE book_id = :bookId ORDER BY created_at DESC")
    fun getHighlightsByBook(bookId: Long): Flow<List<Highlight>>
    
    @Query("SELECT * FROM highlights WHERE id = :highlightId")
    suspend fun getHighlightById(highlightId: Long): Highlight?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight): Long
    
    @Update
    suspend fun updateHighlight(highlight: Highlight)
    
    @Delete
    suspend fun deleteHighlight(highlight: Highlight)
    
    @Query("SELECT COUNT(*) FROM highlights WHERE book_id = :bookId")
    suspend fun getHighlightCountByBook(bookId: Long): Int
}

@Dao
interface NoteDao {
    
    @Query("SELECT * FROM notes ORDER BY modified_at DESC")
    fun getAllNotesFlow(): Flow<List<Note>>
    
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): Note?
    
    @Query("SELECT * FROM notes WHERE book_id = :bookId ORDER BY created_at DESC")
    fun getNotesByBook(bookId: Long): Flow<List<Note>>
    
    @Query("SELECT * FROM notes WHERE highlight_id = :highlightId")
    suspend fun getNotesByHighlight(highlightId: Long): List<Note>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long
    
    @Update
    suspend fun updateNote(note: Note)
    
    @Delete
    suspend fun deleteNote(note: Note)
    
    /**
     * Get notes linked FROM this note (outgoing links)
     */
    @Query("""
        SELECT notes.* FROM notes
        INNER JOIN note_links ON notes.id = note_links.to_note_id
        WHERE note_links.from_note_id = :noteId
    """)
    fun getLinkedNotes(noteId: Long): Flow<List<Note>>
    
    /**
     * CRITICAL: Get notes that link TO this note (backlinks)
     * This is the memory mechanism of the knowledge graph
     */
    @Query("""
        SELECT notes.* FROM notes
        INNER JOIN note_links ON notes.id = note_links.from_note_id
        WHERE note_links.to_note_id = :noteId
    """)
    fun getBacklinks(noteId: Long): Flow<List<Note>>
    
    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getNoteCount(): Int
}

@Dao
interface NoteLinkDao {
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(noteLink: NoteLink)
    
    @Delete
    suspend fun deleteLink(noteLink: NoteLink)
    
    @Query("DELETE FROM note_links WHERE from_note_id = :noteId OR to_note_id = :noteId")
    suspend fun deleteAllLinksForNote(noteId: Long)
    
    @Query("""
        SELECT COUNT(*) FROM note_links 
        WHERE from_note_id = :noteId OR to_note_id = :noteId
    """)
    suspend fun getLinkCount(noteId: Long): Int
    
    /**
     * Check if a link already exists (prevent duplicates)
     */
    @Query("""
        SELECT COUNT(*) FROM note_links 
        WHERE from_note_id = :fromId AND to_note_id = :toId
    """)
    suspend fun linkExists(fromId: Long, toId: Long): Int
}

@Dao
interface TagDao {
    
    @Query("SELECT * FROM tags ORDER BY name")
    fun getAllTags(): Flow<List<Tag>>
    
    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): Tag?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long
    
    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN book_tags ON tags.id = book_tags.tag_id
        WHERE book_tags.book_id = :bookId
    """)
    fun getTagsByBook(bookId: Long): Flow<List<Tag>>
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToBook(bookTag: BookTag)
    
    @Delete
    suspend fun removeTagFromBook(bookTag: BookTag)
}

@Dao
interface ReadingProgressDao {
    
    @Query("SELECT * FROM reading_progress WHERE book_id = :bookId LIMIT 1")
    suspend fun getProgressByBook(bookId: Long): ReadingProgress?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: ReadingProgress)
    
    @Query("UPDATE reading_progress SET progress_percent = 100 WHERE book_id = :bookId")
    suspend fun markBookComplete(bookId: Long)
}

@Dao
interface ReadingSessionDao {
    
    @Insert
    suspend fun insertSession(session: ReadingSession): Long
    
    @Update
    suspend fun updateSession(session: ReadingSession)
    
    @Query("""
        SELECT COALESCE(SUM(duration_minutes), 0) 
        FROM reading_sessions 
        WHERE start_time >= :startTime
    """)
    suspend fun getTotalMinutesRead(startTime: Long): Int
    
    @Query("""
        SELECT COALESCE(SUM(duration_minutes), 0) 
        FROM reading_sessions 
        WHERE start_time >= :startTime AND start_time < :endTime
    """)
    suspend fun getMinutesReadInRange(startTime: Long, endTime: Long): Int
}

@Dao
interface SearchDao {
    
    /**
     * FTS search across book content
     * Returns matching passages with snippets
     */
    @Query("""
        SELECT fts_books_text.*, books.title as book_title
        FROM fts_books_text
        INNER JOIN books ON fts_books_text.book_id = books.id
        WHERE fts_books_text MATCH :query
        ORDER BY rank
        LIMIT :limit
    """)
    suspend fun searchBookContent(query: String, limit: Int = 50): List<BookSearchResult>
    
    /**
     * FTS search across notes
     */
    @Query("""
        SELECT fts_notes.*, notes.created_at, notes.book_id
        FROM fts_notes
        INNER JOIN notes ON fts_notes.note_id = notes.id
        WHERE fts_notes MATCH :query
        ORDER BY rank
        LIMIT :limit
    """)
    suspend fun searchNotes(query: String, limit: Int = 50): List<NoteSearchResult>
    
    /**
     * Insert into FTS table for book content
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun indexBookText(bookText: BookTextFts)
    
    /**
     * Insert into FTS table for notes
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun indexNote(noteFts: NoteFts)
    
    @Query("DELETE FROM fts_books_text WHERE book_id = :bookId")
    suspend fun deleteBookIndex(bookId: Long)
    
    @Query("DELETE FROM fts_notes WHERE note_id = :noteId")
    suspend fun deleteNoteIndex(noteId: Long)
}

// ============================================================================
// SEARCH RESULT DATA CLASSES
// ============================================================================

data class BookSearchResult(
    val rowid: Long,
    val bookId: Long,
    val chapterId: Long?,
    val textContent: String,
    val cfiLocator: String,
    @ColumnInfo(name = "book_title")
    val bookTitle: String
)

data class NoteSearchResult(
    val rowid: Long,
    val noteId: Long,
    val title: String,
    val body: String,
    val createdAt: Long,
    val bookId: Long?
)

// ============================================================================
// DATABASE CONFIGURATION
// ============================================================================

@Database(
    entities = [
        Book::class,
        Chapter::class,
        Highlight::class,
        Note::class,
        NoteLink::class,
        Tag::class,
        BookTag::class,
        ReadingProgress::class,
        ReadingSession::class,
        BookTextFts::class,
        NoteFts::class
    ],
    version = 1,
    exportSchema = true
)
abstract class EPubReaderDatabase : RoomDatabase() {
    
    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun highlightDao(): HighlightDao
    abstract fun noteDao(): NoteDao
    abstract fun noteLinkDao(): NoteLinkDao
    abstract fun tagDao(): TagDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun searchDao(): SearchDao
    
    companion object {
        const val DATABASE_NAME = "epub_reader.db"
    }
}

// ============================================================================
// DATABASE BUILDER (Application setup)
// ============================================================================

/**
 * Call this from your Application class or dependency injection setup
 */
fun buildDatabase(context: android.content.Context): EPubReaderDatabase {
    return Room.databaseBuilder(
        context.applicationContext,
        EPubReaderDatabase::class.java,
        EPubReaderDatabase.DATABASE_NAME
    )
    .fallbackToDestructiveMigration() // For MVP only - remove for production
    .build()
}
