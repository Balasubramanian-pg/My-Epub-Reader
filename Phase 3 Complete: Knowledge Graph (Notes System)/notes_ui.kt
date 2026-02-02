/**
 * Phase 3: Knowledge Graph - Notes System with Linking
 * 
 * This implements the core of the Personal Knowledge Graph:
 * - Notes as first-class entities
 * - Bidirectional linking between notes
 * - Backlinks visualization
 * - Related notes discovery via FTS
 * - Cross-book idea navigation
 */

package com.epreader.ui.notes

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epreader.data.local.Note
import com.epreader.data.local.Book
import com.epreader.data.local.Highlight
import com.epreader.data.repository.NoteRepository
import com.epreader.data.repository.NoteWithLinks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ============================================================================
// NOTES VIEWMODEL - Knowledge Graph Core
// ============================================================================

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {
    
    // All notes with their graph context
    private val _allNotes = MutableStateFlow<List<NoteWithLinks>>(emptyList())
    val allNotes: StateFlow<List<NoteWithLinks>> = _allNotes.asStateFlow()
    
    // Currently selected note for detail view
    private val _selectedNote = MutableStateFlow<NoteWithLinks?>(null)
    val selectedNote: StateFlow<NoteWithLinks?> = _selectedNote.asStateFlow()
    
    // UI state
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()
    
    // Search and filtering
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _filterMode = MutableStateFlow(FilterMode.ALL)
    val filterMode: StateFlow<FilterMode> = _filterMode.asStateFlow()
    
    init {
        loadAllNotes()
        observeSearchAndFilter()
    }
    
    /**
     * Load all notes with their graph context
     */
    private fun loadAllNotes() {
        viewModelScope.launch {
            noteRepository.getAllNotesWithLinks().collect { notes ->
                _allNotes.value = notes
            }
        }
    }
    
    /**
     * Apply search and filter
     */
    private fun observeSearchAndFilter() {
        viewModelScope.launch {
            combine(
                allNotes,
                searchQuery,
                filterMode
            ) { notes, query, mode ->
                notes.filter { noteWithLinks ->
                    val note = noteWithLinks.note
                    val matchesSearch = if (query.isBlank()) {
                        true
                    } else {
                        note.title.contains(query, ignoreCase = true) ||
                        note.body.contains(query, ignoreCase = true)
                    }
                    
                    val matchesFilter = when (mode) {
                        FilterMode.ALL -> true
                        FilterMode.STANDALONE -> note.bookId == null
                        FilterMode.WITH_LINKS -> noteWithLinks.linkedNotes.isNotEmpty() || 
                                                 noteWithLinks.backlinks.isNotEmpty()
                        FilterMode.FROM_HIGHLIGHTS -> note.highlightId != null
                    }
                    
                    matchesSearch && matchesFilter
                }
            }.collect { filtered ->
                _uiState.value = _uiState.value.copy(filteredNotes = filtered)
            }
        }
    }
    
    /**
     * Create standalone note (thinking-first flow)
     */
    fun createStandaloneNote(title: String, body: String) {
        viewModelScope.launch {
            noteRepository.createStandaloneNote(title, body)
            _uiState.value = _uiState.value.copy(showCreateDialog = false)
        }
    }
    
    /**
     * Update existing note
     */
    fun updateNote(noteId: Long, title: String, body: String) {
        viewModelScope.launch {
            noteRepository.updateNote(noteId, title, body)
            // Reload selected note
            loadNoteDetail(noteId)
        }
    }
    
    /**
     * Delete note (with confirmation)
     */
    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
            _selectedNote.value = null
            _uiState.value = _uiState.value.copy(
                showDeleteConfirmation = false,
                noteToDelete = null
            )
        }
    }
    
    /**
     * CRITICAL: Link two notes together
     * This is the core knowledge graph operation
     */
    fun linkNotes(fromNoteId: Long, toNoteId: Long) {
        viewModelScope.launch {
            try {
                noteRepository.linkNotes(fromNoteId, toNoteId)
                // Reload both notes to show updated links
                loadNoteDetail(fromNoteId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to create link"
                )
            }
        }
    }
    
    /**
     * Remove link between notes
     */
    fun unlinkNotes(fromNoteId: Long, toNoteId: Long) {
        viewModelScope.launch {
            noteRepository.unlinkNotes(fromNoteId, toNoteId)
            loadNoteDetail(fromNoteId)
        }
    }
    
    /**
     * Load detailed view of a note with all graph connections
     */
    fun loadNoteDetail(noteId: Long) {
        viewModelScope.launch {
            val noteWithLinks = noteRepository.getNoteWithLinks(noteId)
            _selectedNote.value = noteWithLinks
            
            // Also load related notes via FTS similarity
            val relatedNotes = noteRepository.getRelatedNotes(noteId, limit = 5)
            _uiState.value = _uiState.value.copy(
                relatedNotes = relatedNotes
            )
        }
    }
    
    /**
     * Get notes available for linking (excludes current note and already linked)
     */
    fun getNotesForLinking(currentNoteId: Long): List<Note> {
        val current = _selectedNote.value ?: return emptyList()
        val linkedIds = current.linkedNotes.map { it.id }.toSet()
        
        return _allNotes.value
            .map { it.note }
            .filter { note ->
                note.id != currentNoteId && !linkedIds.contains(note.id)
            }
    }
    
    /**
     * UI Actions
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun setFilterMode(mode: FilterMode) {
        _filterMode.value = mode
    }
    
    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }
    
    fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }
    
    fun showLinkDialog() {
        _uiState.value = _uiState.value.copy(showLinkDialog = true)
    }
    
    fun hideLinkDialog() {
        _uiState.value = _uiState.value.copy(showLinkDialog = false)
    }
    
    fun showDeleteConfirmation(noteId: Long) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = true,
            noteToDelete = noteId
        )
    }
    
    fun hideDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = false,
            noteToDelete = null
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class NotesUiState(
    val filteredNotes: List<NoteWithLinks> = emptyList(),
    val relatedNotes: List<Note> = emptyList(),
    val showCreateDialog: Boolean = false,
    val showLinkDialog: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val noteToDelete: Long? = null,
    val error: String? = null
)

enum class FilterMode {
    ALL,
    STANDALONE,      // Notes without books
    WITH_LINKS,      // Notes with connections
    FROM_HIGHLIGHTS  // Notes created from highlights
}

// ============================================================================
// NOTES SCREEN - Main Entry Point
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onNavigateToReader: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedNote by viewModel.selectedNote.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()
    
    Scaffold(
        topBar = {
            NotesTopBar(
                searchQuery = searchQuery,
                onSearchChange = viewModel::setSearchQuery,
                filterMode = filterMode,
                onFilterChange = viewModel::setFilterMode
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create note")
            }
        },
        modifier = modifier
    ) { padding ->
        
        // Two-pane layout: List + Detail
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Left pane: Notes list
            NotesListPane(
                notes = uiState.filteredNotes,
                selectedNoteId = selectedNote?.note?.id,
                onNoteClick = viewModel::loadNoteDetail,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            
            // Right pane: Note detail (if selected)
            if (selectedNote != null) {
                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )
                
                NoteDetailPane(
                    noteWithLinks = selectedNote!!,
                    relatedNotes = uiState.relatedNotes,
                    onEdit = { noteId, title, body ->
                        viewModel.updateNote(noteId, title, body)
                    },
                    onDelete = viewModel::showDeleteConfirmation,
                    onLinkNote = viewModel::showLinkDialog,
                    onUnlink = viewModel::unlinkNotes,
                    onNavigateToNote = viewModel::loadNoteDetail,
                    onNavigateToReader = onNavigateToReader,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
        
        // Dialogs
        if (uiState.showCreateDialog) {
            CreateNoteDialog(
                onSave = viewModel::createStandaloneNote,
                onDismiss = viewModel::hideCreateDialog
            )
        }
        
        if (uiState.showLinkDialog && selectedNote != null) {
            LinkNoteDialog(
                currentNoteId = selectedNote!!.note.id,
                availableNotes = viewModel.getNotesForLinking(selectedNote!!.note.id),
                onLink = { targetId ->
                    viewModel.linkNotes(selectedNote!!.note.id, targetId)
                    viewModel.hideLinkDialog()
                },
                onDismiss = viewModel::hideLinkDialog
            )
        }
        
        if (uiState.showDeleteConfirmation && uiState.noteToDelete != null) {
            DeleteConfirmationDialog(
                onConfirm = { viewModel.deleteNote(uiState.noteToDelete!!) },
                onDismiss = viewModel::hideDeleteConfirmation
            )
        }
        
        // Error snackbar
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // Show snackbar
                viewModel.clearError()
            }
        }
    }
}

// ============================================================================
// TOP BAR WITH SEARCH AND FILTER
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesTopBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterMode: FilterMode,
    onFilterChange: (FilterMode) -> Unit
) {
    var showFilterMenu by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = { Text("Notes") },
        actions = {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search notes...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.width(300.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Filter button
            Box {
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
                
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    FilterMode.values().forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.displayName()) },
                            onClick = {
                                onFilterChange(mode)
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (mode == filterMode) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}

fun FilterMode.displayName(): String = when (this) {
    FilterMode.ALL -> "All Notes"
    FilterMode.STANDALONE -> "Standalone Ideas"
    FilterMode.WITH_LINKS -> "Linked Notes"
    FilterMode.FROM_HIGHLIGHTS -> "From Highlights"
}

// ============================================================================
// NOTES LIST PANE
// ============================================================================

@Composable
fun NotesListPane(
    notes: List<NoteWithLinks>,
    selectedNoteId: Long?,
    onNoteClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (notes.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Note,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No notes yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Create your first note to start building your knowledge graph",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = notes,
                key = { it.note.id }
            ) { noteWithLinks ->
                NoteListItem(
                    noteWithLinks = noteWithLinks,
                    isSelected = noteWithLinks.note.id == selectedNoteId,
                    onClick = { onNoteClick(noteWithLinks.note.id) }
                )
            }
        }
    }
}

@Composable
fun NoteListItem(
    noteWithLinks: NoteWithLinks,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val note = noteWithLinks.note
    val linkCount = noteWithLinks.linkedNotes.size + noteWithLinks.backlinks.size
    
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Body preview
            Text(
                text = note.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Metadata row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Link count
                if (linkCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = linkCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Source indicator
                when {
                    noteWithLinks.highlight != null -> {
                        Chip(
                            icon = Icons.Default.Bookmark,
                            label = "From highlight"
                        )
                    }
                    noteWithLinks.book != null -> {
                        Chip(
                            icon = Icons.Default.Book,
                            label = noteWithLinks.book.title
                        )
                    }
                    else -> {
                        Chip(
                            icon = Icons.Default.LightbulbOutline,
                            label = "Standalone"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Date
                Text(
                    text = formatDate(note.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun Chip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// NOTE DETAIL PANE - The Knowledge Graph Navigator
// ============================================================================

@Composable
fun NoteDetailPane(
    noteWithLinks: NoteWithLinks,
    relatedNotes: List<Note>,
    onEdit: (Long, String, String) -> Unit,
    onDelete: (Long) -> Unit,
    onLinkNote: () -> Unit,
    onUnlink: (Long, Long) -> Unit,
    onNavigateToNote: (Long) -> Unit,
    onNavigateToReader: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(noteWithLinks.note.title) }
    var editBody by remember { mutableStateOf(noteWithLinks.note.body) }
    
    LaunchedEffect(noteWithLinks.note.id) {
        editTitle = noteWithLinks.note.title
        editBody = noteWithLinks.note.body
        isEditing = false
    }
    
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header with actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Editing Note" else "Note",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isEditing) {
                        TextButton(onClick = { isEditing = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onEdit(noteWithLinks.note.id, editTitle, editBody)
                                isEditing = false
                            }
                        ) {
                            Text("Save")
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = onLinkNote) {
                            Icon(Icons.Default.AddLink, contentDescription = "Link note")
                        }
                        IconButton(onClick = { onDelete(noteWithLinks.note.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
        
        // Note content
        item {
            if (isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBody,
                        onValueChange = { editBody = it },
                        label = { Text("Body") },
                        minLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = noteWithLinks.note.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = noteWithLinks.note.body,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        
        // Source context (if from highlight)
        if (noteWithLinks.highlight != null && noteWithLinks.book != null) {
            item {
                SourceCard(
                    book = noteWithLinks.book,
                    highlight = noteWithLinks.highlight,
                    onNavigateToReader = onNavigateToReader
                )
            }
        }
        
        // CRITICAL: Linked Notes Section
        if (noteWithLinks.linkedNotes.isNotEmpty()) {
            item {
                GraphSection(
                    title = "Linked Notes",
                    icon = Icons.Default.Link,
                    notes = noteWithLinks.linkedNotes,
                    onNoteClick = onNavigateToNote,
                    onUnlink = { targetId ->
                        onUnlink(noteWithLinks.note.id, targetId)
                    }
                )
            }
        }
        
        // CRITICAL: Backlinks Section (The Memory Mechanism)
        if (noteWithLinks.backlinks.isNotEmpty()) {
            item {
                GraphSection(
                    title = "Referenced By",
                    icon = Icons.Default.CallReceived,
                    notes = noteWithLinks.backlinks,
                    onNoteClick = onNavigateToNote,
                    showUnlink = false
                )
            }
        }
        
        // Related notes (FTS-based suggestions)
        if (relatedNotes.isNotEmpty()) {
            item {
                RelatedNotesSection(
                    notes = relatedNotes,
                    onNoteClick = onNavigateToNote
                )
            }
        }
        
        // Metadata
        item {
            MetadataCard(note = noteWithLinks.note)
        }
    }
}

@Composable
fun SourceCard(
    book: Book,
    highlight: Highlight,
    onNavigateToReader: (Long, String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Source",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Highlighted text
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(android.graphics.Color.parseColor(highlight.color)).copy(alpha = 0.3f)
            ) {
                Text(
                    text = "\"${highlight.text}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            // Book info with jump button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                TextButton(
                    onClick = { onNavigateToReader(book.id, book.filePath) }
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Jump to passage")
                }
            }
        }
    }
}

@Composable
fun GraphSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onUnlink: ((Long) -> Unit)? = null,
    showUnlink: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$title (${notes.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        notes.forEach { note ->
            LinkedNoteCard(
                note = note,
                onClick = { onNoteClick(note.id) },
                onUnlink = if (showUnlink && onUnlink != null) {
                    { onUnlink(note.id) }
                } else null
            )
        }
    }
}

@Composable
fun LinkedNoteCard(
    note: Note,
    onClick: () -> Unit,
    onUnlink: (() -> Unit)?
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (onUnlink != null) {
                IconButton(onClick = onUnlink) {
                    Icon(
                        imageVector = Icons.Default.LinkOff,
                        contentDescription = "Remove link",
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun RelatedNotesSection(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = "Related Ideas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Text(
            text = "Suggested based on shared concepts",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        notes.forEach { note ->
            LinkedNoteCard(
                note = note,
                onClick = { onNoteClick(note.id) },
                onUnlink = null
            )
        }
    }
}

@Composable
fun MetadataCard(note: Note) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MetadataRow("Created", formatDate(note.createdAt))
            MetadataRow("Modified", formatDate(note.modifiedAt))
            MetadataRow("ID", note.id.toString())
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Continued in next file...
