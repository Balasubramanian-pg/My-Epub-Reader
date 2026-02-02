/**
 * Phase 5: Export & Polish - Markdown Export Engine
 * 
 * Complete export system that converts notes and their graph structure
 * to portable Markdown files with:
 * - YAML frontmatter (title, created_at, source_book, location, tags, backlinks)
 * - Highlighted quote in blockquote
 * - Note body
 * - Wikilink-style internal links [[note-title]]
 * - Organized folder structure per book
 */

package com.epreader.export

import android.content.Context
import android.os.Environment
import com.epreader.data.local.Book
import com.epreader.data.local.Highlight
import com.epreader.data.local.Note
import com.epreader.data.local.Tag
import com.epreader.data.repository.NoteRepository
import com.epreader.data.repository.BookRepository
import com.epreader.data.repository.HighlightRepository
import com.epreader.data.repository.NoteWithLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// ============================================================================
// MARKDOWN EXPORT SERVICE
// ============================================================================

@Singleton
class MarkdownExportService @Inject constructor(
    private val context: Context,
    private val noteRepository: NoteRepository,
    private val bookRepository: BookRepository,
    private val highlightRepository: HighlightRepository
) {
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    private val fileNameDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    /**
     * Export all notes for a specific book
     * Returns the root export directory
     */
    suspend fun exportBookNotes(bookId: Long): ExportResult = withContext(Dispatchers.IO) {
        try {
            // Get book metadata
            val bookWithMeta = bookRepository.getBookWithMetadata(bookId)
            val book = bookWithMeta.book
            
            // Get all notes for this book
            val notes = noteRepository.getNotesByBook(bookId).first()
            
            if (notes.isEmpty()) {
                return@withContext ExportResult.NoContent("No notes found for this book")
            }
            
            // Create export directory
            val exportDir = createBookExportDirectory(book)
            
            // Export each note
            val exportedFiles = mutableListOf<File>()
            notes.forEach { noteWithLinks ->
                val file = exportNote(noteWithLinks, book, exportDir)
                exportedFiles.add(file)
            }
            
            // Create index file
            val indexFile = createIndexFile(book, notes, exportDir)
            exportedFiles.add(indexFile)
            
            ExportResult.Success(
                directory = exportDir,
                filesCreated = exportedFiles,
                noteCount = notes.size
            )
            
        } catch (e: Exception) {
            ExportResult.Error("Export failed: ${e.message}")
        }
    }
    
    /**
     * Export all notes in the library (across all books)
     */
    suspend fun exportAllNotes(): ExportResult = withContext(Dispatchers.IO) {
        try {
            // Get all notes
            val allNotes = noteRepository.getAllNotesWithLinks().first()
            
            if (allNotes.isEmpty()) {
                return@withContext ExportResult.NoContent("No notes found in library")
            }
            
            // Create root export directory
            val rootDir = createRootExportDirectory()
            
            // Group notes by book
            val notesByBook = allNotes.groupBy { it.note.bookId }
            val exportedFiles = mutableListOf<File>()
            
            // Export notes for each book
            notesByBook.forEach { (bookId, notes) ->
                if (bookId != null) {
                    val book = bookRepository.getBookWithMetadata(bookId).book
                    val bookDir = File(rootDir, sanitizeFileName(book.title))
                    bookDir.mkdirs()
                    
                    notes.forEach { noteWithLinks ->
                        val file = exportNote(noteWithLinks, book, bookDir)
                        exportedFiles.add(file)
                    }
                    
                    // Create book index
                    val indexFile = createIndexFile(book, notes, bookDir)
                    exportedFiles.add(indexFile)
                }
            }
            
            // Handle standalone notes (no book)
            val standaloneNotes = notesByBook[null] ?: emptyList()
            if (standaloneNotes.isNotEmpty()) {
                val standaloneDir = File(rootDir, "Standalone Notes")
                standaloneDir.mkdirs()
                
                standaloneNotes.forEach { noteWithLinks ->
                    val file = exportNote(noteWithLinks, null, standaloneDir)
                    exportedFiles.add(file)
                }
            }
            
            // Create master index
            val masterIndex = createMasterIndexFile(rootDir, notesByBook.keys.size, allNotes.size)
            exportedFiles.add(masterIndex)
            
            ExportResult.Success(
                directory = rootDir,
                filesCreated = exportedFiles,
                noteCount = allNotes.size
            )
            
        } catch (e: Exception) {
            ExportResult.Error("Export failed: ${e.message}")
        }
    }
    
    /**
     * Export a single note
     */
    suspend fun exportSingleNote(noteId: Long): ExportResult = withContext(Dispatchers.IO) {
        try {
            val noteWithLinks = noteRepository.getNoteWithLinks(noteId)
            val note = noteWithLinks.note
            
            // Get book if available
            val book = note.bookId?.let { bookRepository.getBookWithMetadata(it).book }
            
            // Create export directory
            val exportDir = if (book != null) {
                createBookExportDirectory(book)
            } else {
                File(createRootExportDirectory(), "Standalone Notes").apply { mkdirs() }
            }
            
            // Export note
            val file = exportNote(noteWithLinks, book, exportDir)
            
            ExportResult.Success(
                directory = exportDir,
                filesCreated = listOf(file),
                noteCount = 1
            )
            
        } catch (e: Exception) {
            ExportResult.Error("Export failed: ${e.message}")
        }
    }
    
    // ========================================================================
    // PRIVATE EXPORT METHODS
    // ========================================================================
    
    /**
     * Export a single note to markdown file
     */
    private suspend fun exportNote(
        noteWithLinks: NoteWithLinks,
        book: Book?,
        outputDir: File
    ): File {
        val note = noteWithLinks.note
        val highlight = noteWithLinks.highlight
        
        // Generate filename
        val datePrefix = fileNameDateFormatter.format(Date(note.createdAt))
        val titleSlug = sanitizeFileName(note.title)
        val filename = "${datePrefix}_${note.id}_$titleSlug.md"
        
        val file = File(outputDir, filename)
        
        // Build markdown content
        val markdown = buildMarkdownContent(
            note = note,
            highlight = highlight,
            book = book,
            linkedNotes = noteWithLinks.linkedNotes,
            backlinks = noteWithLinks.backlinks
        )
        
        file.writeText(markdown)
        
        return file
    }
    
    /**
     * Build complete markdown content for a note
     */
    private fun buildMarkdownContent(
        note: Note,
        highlight: Highlight?,
        book: Book?,
        linkedNotes: List<Note>,
        backlinks: List<Note>
    ): String {
        val builder = StringBuilder()
        
        // YAML Frontmatter
        builder.appendLine("---")
        builder.appendLine("title: \"${escapeYaml(note.title)}\"")
        builder.appendLine("created_at: ${dateFormatter.format(Date(note.createdAt))}")
        builder.appendLine("modified_at: ${dateFormatter.format(Date(note.modifiedAt))}")
        
        if (book != null) {
            builder.appendLine("source_book: \"${escapeYaml(book.title)}\"")
            builder.appendLine("author: \"${escapeYaml(book.author)}\"")
        }
        
        if (highlight != null) {
            builder.appendLine("location_cfi: \"${escapeYaml(highlight.cfiOrRange)}\"")
        }
        
        // Backlinks
        if (backlinks.isNotEmpty()) {
            builder.appendLine("backlinks:")
            backlinks.forEach { backlink ->
                val backlinkFilename = generateNoteFilename(backlink)
                builder.appendLine("  - \"$backlinkFilename\"")
            }
        }
        
        // Outgoing links
        if (linkedNotes.isNotEmpty()) {
            builder.appendLine("links:")
            linkedNotes.forEach { linked ->
                val linkedFilename = generateNoteFilename(linked)
                builder.appendLine("  - \"$linkedFilename\"")
            }
        }
        
        builder.appendLine("---")
        builder.appendLine()
        
        // Highlight quote (if exists)
        if (highlight != null) {
            builder.appendLine("> ${highlight.text}")
            builder.appendLine()
            if (book != null) {
                builder.appendLine("*— ${book.title} by ${book.author}*")
                builder.appendLine()
            }
        }
        
        // Note title as H1
        builder.appendLine("# ${note.title}")
        builder.appendLine()
        
        // Note body
        builder.appendLine(note.body)
        builder.appendLine()
        
        // Linked notes section
        if (linkedNotes.isNotEmpty()) {
            builder.appendLine("## Related Notes")
            builder.appendLine()
            linkedNotes.forEach { linked ->
                builder.appendLine("- [[${linked.title}]]")
            }
            builder.appendLine()
        }
        
        // Backlinks section
        if (backlinks.isNotEmpty()) {
            builder.appendLine("## Referenced By")
            builder.appendLine()
            backlinks.forEach { backlink ->
                builder.appendLine("- [[${backlink.title}]]")
            }
            builder.appendLine()
        }
        
        return builder.toString()
    }
    
    /**
     * Create index file for a book's notes
     */
    private fun createIndexFile(
        book: Book,
        notes: List<NoteWithLinks>,
        outputDir: File
    ): File {
        val indexFile = File(outputDir, "INDEX.md")
        
        val content = buildString {
            appendLine("# Notes from \"${book.title}\"")
            appendLine()
            appendLine("**Author:** ${book.author}")
            appendLine("**Notes Created:** ${notes.size}")
            appendLine("**Export Date:** ${SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())}")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## All Notes")
            appendLine()
            
            notes.sortedByDescending { it.note.createdAt }.forEach { noteWithLinks ->
                val note = noteWithLinks.note
                val filename = generateNoteFilename(note)
                appendLine("- [${note.title}]($filename)")
            }
        }
        
        indexFile.writeText(content)
        return indexFile
    }
    
    /**
     * Create master index for full library export
     */
    private fun createMasterIndexFile(
        rootDir: File,
        bookCount: Int,
        noteCount: Int
    ): File {
        val indexFile = File(rootDir, "README.md")
        
        val content = buildString {
            appendLine("# EPUB Reader Knowledge Export")
            appendLine()
            appendLine("**Export Date:** ${SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.US).format(Date())}")
            appendLine("**Books:** $bookCount")
            appendLine("**Total Notes:** $noteCount")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## About This Export")
            appendLine()
            appendLine("This export contains all your notes and highlights from your personal EPUB library.")
            appendLine("Each book has its own folder with individual note files in Markdown format.")
            appendLine()
            appendLine("### File Format")
            appendLine()
            appendLine("- Each note is a separate `.md` file")
            appendLine("- YAML frontmatter contains metadata (title, dates, source, links)")
            appendLine("- Original highlighted quotes included in blockquotes")
            appendLine("- Internal links preserved as `[[Note Title]]` format (Obsidian-compatible)")
            appendLine()
            appendLine("### Folder Structure")
            appendLine()
            appendLine("```")
            appendLine("Export/")
            appendLine("├── README.md (this file)")
            appendLine("├── Book Title 1/")
            appendLine("│   ├── INDEX.md")
            appendLine("│   ├── 2026-02-02_1_note-title.md")
            appendLine("│   └── ...")
            appendLine("├── Book Title 2/")
            appendLine("│   └── ...")
            appendLine("└── Standalone Notes/")
            appendLine("    └── ...")
            appendLine("```")
            appendLine()
            appendLine("### Importing to Other Tools")
            appendLine()
            appendLine("- **Obsidian:** Import the entire folder as a vault")
            appendLine("- **Notion:** Import individual markdown files")
            appendLine("- **Roam Research:** Use markdown import")
            appendLine("- **Git:** Track as a repository for version control")
        }
        
        indexFile.writeText(content)
        return indexFile
    }
    
    // ========================================================================
    // DIRECTORY MANAGEMENT
    // ========================================================================
    
    /**
     * Create root export directory
     */
    private fun createRootExportDirectory(): File {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
        val exportDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "EPubReaderExport/$timestamp"
        )
        exportDir.mkdirs()
        return exportDir
    }
    
    /**
     * Create book-specific export directory
     */
    private fun createBookExportDirectory(book: Book): File {
        val rootDir = createRootExportDirectory()
        val bookDir = File(rootDir, sanitizeFileName(book.title))
        bookDir.mkdirs()
        return bookDir
    }
    
    // ========================================================================
    // UTILITY METHODS
    // ========================================================================
    
    /**
     * Generate filename for a note (for backlinking)
     */
    private fun generateNoteFilename(note: Note): String {
        val datePrefix = fileNameDateFormatter.format(Date(note.createdAt))
        val titleSlug = sanitizeFileName(note.title)
        return "${datePrefix}_${note.id}_$titleSlug.md"
    }
    
    /**
     * Sanitize filename to be filesystem-safe
     */
    private fun sanitizeFileName(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .take(50)
            .trim('-')
            .lowercase()
    }
    
    /**
     * Escape YAML special characters
     */
    private fun escapeYaml(value: String): String {
        return value
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
    }
}

// ============================================================================
// EXPORT RESULT TYPES
// ============================================================================

sealed class ExportResult {
    data class Success(
        val directory: File,
        val filesCreated: List<File>,
        val noteCount: Int
    ) : ExportResult()
    
    data class NoContent(val message: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

// ============================================================================
// EXPORT VIEWMODEL
// ============================================================================

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportService: MarkdownExportService
) : ViewModel() {
    
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()
    
    /**
     * Export all notes for a specific book
     */
    fun exportBookNotes(bookId: Long) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting(progress = 0f)
            
            val result = exportService.exportBookNotes(bookId)
            
            _exportState.value = when (result) {
                is ExportResult.Success -> ExportState.Success(
                    directory = result.directory,
                    filesCreated = result.filesCreated.size,
                    message = "Exported ${result.noteCount} notes"
                )
                is ExportResult.NoContent -> ExportState.Error(result.message)
                is ExportResult.Error -> ExportState.Error(result.message)
            }
        }
    }
    
    /**
     * Export all notes in library
     */
    fun exportAllNotes() {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting(progress = 0f)
            
            val result = exportService.exportAllNotes()
            
            _exportState.value = when (result) {
                is ExportResult.Success -> ExportState.Success(
                    directory = result.directory,
                    filesCreated = result.filesCreated.size,
                    message = "Exported ${result.noteCount} notes from ${result.directory.listFiles()?.size ?: 0} books"
                )
                is ExportResult.NoContent -> ExportState.Error(result.message)
                is ExportResult.Error -> ExportState.Error(result.message)
            }
        }
    }
    
    /**
     * Export a single note
     */
    fun exportSingleNote(noteId: Long) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting(progress = 0f)
            
            val result = exportService.exportSingleNote(noteId)
            
            _exportState.value = when (result) {
                is ExportResult.Success -> ExportState.Success(
                    directory = result.directory,
                    filesCreated = result.filesCreated.size,
                    message = "Note exported successfully"
                )
                is ExportResult.NoContent -> ExportState.Error(result.message)
                is ExportResult.Error -> ExportState.Error(result.message)
            }
        }
    }
    
    /**
     * Reset export state
     */
    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }
}

sealed class ExportState {
    object Idle : ExportState()
    data class Exporting(val progress: Float) : ExportState()
    data class Success(
        val directory: File,
        val filesCreated: Int,
        val message: String
    ) : ExportState()
    data class Error(val message: String) : ExportState()
}

// ============================================================================
// EXPORT DIALOG UI
// ============================================================================

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExportDialog(
    exportState: ExportState,
    onExportBook: () -> Unit,
    onExportAll: () -> Unit,
    onOpenFolder: (File) -> Unit,
    onDismiss: () -> Unit
) {
    when (exportState) {
        ExportState.Idle -> {
            ExportOptionsDialog(
                onExportBook = onExportBook,
                onExportAll = onExportAll,
                onDismiss = onDismiss
            )
        }
        is ExportState.Exporting -> {
            ExportProgressDialog(progress = exportState.progress)
        }
        is ExportState.Success -> {
            ExportSuccessDialog(
                directory = exportState.directory,
                filesCreated = exportState.filesCreated,
                message = exportState.message,
                onOpenFolder = { onOpenFolder(exportState.directory) },
                onDismiss = onDismiss
            )
        }
        is ExportState.Error -> {
            ExportErrorDialog(
                message = exportState.message,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
fun ExportOptionsDialog(
    onExportBook: () -> Unit,
    onExportAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
        title = { Text("Export Notes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Choose what to export:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedCard(
                    onClick = onExportBook,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Book, contentDescription = null)
                        Column {
                            Text(
                                "This Book Only",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Export notes from this book",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                OutlinedCard(
                    onClick = onExportAll,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LibraryBooks, contentDescription = null)
                        Column {
                            Text(
                                "Entire Library",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Export all notes from all books",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExportProgressDialog(progress: Float) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Exporting...") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text("Creating Markdown files...")
            }
        },
        confirmButton = {}
    )
}

@Composable
fun ExportSuccessDialog(
    directory: File,
    filesCreated: Int,
    message: String,
    onOpenFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Export Complete") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message)
                Text(
                    "$filesCreated files created",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Location: ${directory.absolutePath}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpenFolder) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun ExportErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Export Failed") },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
