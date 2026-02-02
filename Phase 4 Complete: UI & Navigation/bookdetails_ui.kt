/**
 * Phase 4 Continued: Book Details Screen
 * 
 * Comprehensive book metadata view with:
 * - Cover and metadata display
 * - Tag management
 * - Rating control
 * - Action buttons (Open Reader, Export Notes)
 * - Recent highlights preview
 * - Similar books section
 */

package com.epreader.ui.bookdetails

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.epreader.data.local.Book
import com.epreader.data.local.Highlight
import com.epreader.data.local.Tag
import com.epreader.data.repository.BookRepository
import com.epreader.data.repository.BookWithMetadata
import com.epreader.data.repository.HighlightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================================
// BOOK DETAILS VIEWMODEL
// ============================================================================

@HiltViewModel
class BookDetailsViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val highlightRepository: HighlightRepository
) : ViewModel() {
    
    private val _bookWithMetadata = MutableStateFlow<BookWithMetadata?>(null)
    val bookWithMetadata: StateFlow<BookWithMetadata?> = _bookWithMetadata.asStateFlow()
    
    private val _recentHighlights = MutableStateFlow<List<Highlight>>(emptyList())
    val recentHighlights: StateFlow<List<Highlight>> = _recentHighlights.asStateFlow()
    
    private val _similarBooks = MutableStateFlow<List<BookWithMetadata>>(emptyList())
    val similarBooks: StateFlow<List<BookWithMetadata>> = _similarBooks.asStateFlow()
    
    private val _uiState = MutableStateFlow(BookDetailsUiState())
    val uiState: StateFlow<BookDetailsUiState> = _uiState.asStateFlow()
    
    /**
     * Load book details
     */
    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load book with metadata
                val bookWithMeta = bookRepository.getBookWithMetadata(bookId)
                _bookWithMetadata.value = bookWithMeta
                
                // Load recent highlights
                highlightRepository.getHighlightsByBook(bookId).collect { highlights ->
                    _recentHighlights.value = highlights.take(5)
                }
                
                // Load similar books (by shared tags or same author)
                loadSimilarBooks(bookWithMeta)
                
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Find similar books based on tags and author
     */
    private suspend fun loadSimilarBooks(currentBook: BookWithMetadata) {
        // This is a placeholder - would need proper repository method
        // For now, just clear
        _similarBooks.value = emptyList()
    }
    
    /**
     * Update rating
     */
    fun updateRating(rating: Float) {
        viewModelScope.launch {
            val book = _bookWithMetadata.value?.book ?: return@launch
            bookRepository.updateRating(book.id, rating)
            loadBook(book.id) // Reload to show updated rating
        }
    }
    
    /**
     * Add tag
     */
    fun addTag(tagName: String) {
        viewModelScope.launch {
            val book = _bookWithMetadata.value?.book ?: return@launch
            bookRepository.addTagToBook(book.id, tagName)
            loadBook(book.id) // Reload to show new tag
            _uiState.value = _uiState.value.copy(showAddTagDialog = false)
        }
    }
    
    /**
     * Remove tag
     */
    fun removeTag(tagId: Long) {
        viewModelScope.launch {
            val book = _bookWithMetadata.value?.book ?: return@launch
            bookRepository.removeTagFromBook(book.id, tagId)
            loadBook(book.id)
        }
    }
    
    /**
     * UI actions
     */
    fun showAddTagDialog() {
        _uiState.value = _uiState.value.copy(showAddTagDialog = true)
    }
    
    fun hideAddTagDialog() {
        _uiState.value = _uiState.value.copy(showAddTagDialog = false)
    }
    
    fun showDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = true)
    }
    
    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class BookDetailsUiState(
    val isLoading: Boolean = false,
    val showAddTagDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val error: String? = null
)

// ============================================================================
// BOOK DETAILS SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(
    bookId: Long,
    viewModel: BookDetailsViewModel,
    onNavigateBack: () -> Unit,
    onOpenReader: (Long, String) -> Unit,
    onExportNotes: (Long) -> Unit,
    onDeleteBook: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookWithMetadata by viewModel.bookWithMetadata.collectAsState()
    val recentHighlights by viewModel.recentHighlights.collectAsState()
    val similarBooks by viewModel.similarBooks.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    // Load book on first composition
    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::showDeleteDialog) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete book")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        
        if (uiState.isLoading || bookWithMetadata == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Header with cover and metadata
                item {
                    BookHeader(
                        bookWithMetadata = bookWithMetadata!!,
                        onRatingChange = viewModel::updateRating
                    )
                }
                
                // Action buttons
                item {
                    ActionButtons(
                        bookWithMetadata = bookWithMetadata!!,
                        onOpenReader = { onOpenReader(bookId, bookWithMetadata!!.book.filePath) },
                        onExportNotes = { onExportNotes(bookId) },
                        onAddTag = viewModel::showAddTagDialog
                    )
                }
                
                // Tags section
                item {
                    TagsSection(
                        tags = bookWithMetadata!!.tags,
                        onRemoveTag = viewModel::removeTag,
                        onAddTag = viewModel::showAddTagDialog
                    )
                }
                
                // Metadata section
                item {
                    MetadataSection(book = bookWithMetadata!!.book)
                }
                
                // Recent highlights preview
                if (recentHighlights.isNotEmpty()) {
                    item {
                        HighlightsPreview(
                            highlights = recentHighlights,
                            onViewAll = { onOpenReader(bookId, bookWithMetadata!!.book.filePath) }
                        )
                    }
                }
                
                // Similar books
                if (similarBooks.isNotEmpty()) {
                    item {
                        SimilarBooksSection(
                            books = similarBooks,
                            onBookClick = { /* Navigate to that book's details */ }
                        )
                    }
                }
            }
        }
        
        // Add tag dialog
        if (uiState.showAddTagDialog) {
            AddTagDialog(
                existingTags = bookWithMetadata?.tags ?: emptyList(),
                onSave = viewModel::addTag,
                onDismiss = viewModel::hideAddTagDialog
            )
        }
        
        // Delete confirmation
        if (uiState.showDeleteDialog) {
            DeleteBookConfirmationDialog(
                bookTitle = bookWithMetadata?.book?.title ?: "",
                onConfirm = {
                    onDeleteBook(bookId)
                    onNavigateBack()
                },
                onDismiss = viewModel::hideDeleteDialog
            )
        }
    }
}

// ============================================================================
// BOOK HEADER
// ============================================================================

@Composable
fun BookHeader(
    bookWithMetadata: BookWithMetadata,
    onRatingChange: (Float) -> Unit
) {
    val book = bookWithMetadata.book
    val progress = bookWithMetadata.progress
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Book cover
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (book.coverPath != null) {
                AsyncImage(
                    model = book.coverPath,
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Progress overlay
            if (progress != null && progress.progressPercent > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = "${progress.progressPercent.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        
        // Metadata column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = book.author,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Rating stars
            RatingBar(
                rating = book.rating,
                onRatingChange = onRatingChange
            )
            
            // Stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatChip(
                    icon = Icons.Default.Bookmark,
                    count = bookWithMetadata.highlightCount,
                    label = "highlights"
                )
                StatChip(
                    icon = Icons.Default.Note,
                    count = bookWithMetadata.noteCount,
                    label = "notes"
                )
            }
        }
    }
}

@Composable
fun RatingBar(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 1..5) {
            IconButton(
                onClick = { onRatingChange(i.toFloat()) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Rate $i stars",
                    tint = if (i <= rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$count $label",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ============================================================================
// ACTION BUTTONS
// ============================================================================

@Composable
fun ActionButtons(
    bookWithMetadata: BookWithMetadata,
    onOpenReader: () -> Unit,
    onExportNotes: () -> Unit,
    onAddTag: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onOpenReader,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.MenuBook, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (bookWithMetadata.progress?.progressPercent ?: 0f > 0) {
                    "Continue Reading"
                } else {
                    "Start Reading"
                }
            )
        }
        
        OutlinedButton(
            onClick = onExportNotes,
            enabled = bookWithMetadata.noteCount > 0
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Notes")
        }
    }
}

// ============================================================================
// TAGS SECTION
// ============================================================================

@Composable
fun TagsSection(
    tags: List<Tag>,
    onRemoveTag: (Long) -> Unit,
    onAddTag: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onAddTag) {
                Icon(Icons.Default.Add, contentDescription = "Add tag")
            }
        }
        
        if (tags.isEmpty()) {
            Text(
                text = "No tags yet. Add tags to organize your library.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    TagChip(
                        tag = tag,
                        onRemove = { onRemoveTag(tag.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Simple flow layout implementation
    Column(modifier = modifier) {
        content()
    }
}

@Composable
fun TagChip(
    tag: Tag,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove tag",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// ============================================================================
// METADATA SECTION
// ============================================================================

@Composable
fun MetadataSection(book: Book) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                book.publisher?.let {
                    MetadataRow("Publisher", it)
                }
                book.pubDate?.let {
                    MetadataRow("Published", it)
                }
                book.isbn?.let {
                    MetadataRow("ISBN", it)
                }
                book.language?.let {
                    MetadataRow("Language", it)
                }
                book.totalPages?.let {
                    MetadataRow("Pages", it.toString())
                }
                MetadataRow("Added", formatImportDate(book.importTimestamp))
            }
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================================================
// HIGHLIGHTS PREVIEW
// ============================================================================

@Composable
fun HighlightsPreview(
    highlights: List<Highlight>,
    onViewAll: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Highlights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(onClick = onViewAll) {
                Text("View all")
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
        
        highlights.forEach { highlight ->
            HighlightCard(highlight = highlight)
        }
    }
}

@Composable
fun HighlightCard(highlight: Highlight) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(android.graphics.Color.parseColor(highlight.color)).copy(alpha = 0.2f)
    ) {
        Text(
            text = "\"${highlight.text}\"",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================================
// SIMILAR BOOKS SECTION
// ============================================================================

@Composable
fun SimilarBooksSection(
    books: List<BookWithMetadata>,
    onBookClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier.padding(start = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Similar Books",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 24.dp)
        ) {
            items(books) { bookWithMeta ->
                // Reuse BookCard from library
                com.epreader.ui.library.BookCard(
                    bookWithMetadata = bookWithMeta,
                    onClick = { onBookClick(bookWithMeta.book.id) }
                )
            }
        }
    }
}

// ============================================================================
// DIALOGS
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTagDialog(
    existingTags: List<Tag>,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tagName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("Tag name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (existingTags.isNotEmpty()) {
                    Text(
                        text = "Existing tags:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = existingTags.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tagName.isNotBlank()) {
                        onSave(tagName.trim())
                    }
                },
                enabled = tagName.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteBookConfirmationDialog(
    bookTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete \"$bookTitle\"?") },
        text = { Text("This will permanently delete the book and all associated highlights and notes.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ============================================================================
// UTILITIES
// ============================================================================

fun formatImportDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
