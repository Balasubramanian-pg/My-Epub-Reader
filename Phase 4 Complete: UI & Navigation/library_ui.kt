/**
 * Phase 4: UI & Navigation - Library Screen
 * 
 * Netflix-style browsing interface with:
 * - Horizontal carousels for different book categories
 * - Continue Reading, Recently Added, Top Rated, By Tag
 * - Book cards with metadata and progress
 * - Pull to refresh
 * - Empty states
 */

package com.epreader.ui.library

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import com.epreader.data.local.Tag
import com.epreader.data.local.ReadingProgress
import com.epreader.data.repository.BookRepository
import com.epreader.data.repository.BookWithMetadata
import com.epreader.data.repository.ReadingSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================================
// LIBRARY VIEWMODEL
// ============================================================================

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val sessionRepository: ReadingSessionRepository
) : ViewModel() {
    
    // All books with enriched metadata
    private val _allBooks = MutableStateFlow<List<BookWithMetadata>>(emptyList())
    val allBooks: StateFlow<List<BookWithMetadata>> = _allBooks.asStateFlow()
    
    // Carousel data
    private val _continueReading = MutableStateFlow<List<BookWithMetadata>>(emptyList())
    val continueReading: StateFlow<List<BookWithMetadata>> = _continueReading.asStateFlow()
    
    private val _recentlyAdded = MutableStateFlow<List<BookWithMetadata>>(emptyList())
    val recentlyAdded: StateFlow<List<BookWithMetadata>> = _recentlyAdded.asStateFlow()
    
    private val _topRated = MutableStateFlow<List<BookWithMetadata>>(emptyList())
    val topRated: StateFlow<List<BookWithMetadata>> = _topRated.asStateFlow()
    
    private val _booksByTag = MutableStateFlow<Map<String, List<BookWithMetadata>>>(emptyMap())
    val booksByTag: StateFlow<Map<String, List<BookWithMetadata>>> = _booksByTag.asStateFlow()
    
    // Analytics
    private val _minutesReadThisWeek = MutableStateFlow(0)
    val minutesReadThisWeek: StateFlow<Int> = _minutesReadThisWeek.asStateFlow()
    
    // UI state
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    init {
        loadLibraryData()
        loadAnalytics()
    }
    
    /**
     * Load all library data and populate carousels
     */
    private fun loadLibraryData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load all books with metadata
                bookRepository.getAllBooksWithMetadata().collect { books ->
                    _allBooks.value = books
                    
                    // Populate carousels
                    populateCarousels(books)
                    
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Organize books into carousel categories
     */
    private fun populateCarousels(books: List<BookWithMetadata>) {
        // Continue Reading: books with progress > 0 and < 100
        _continueReading.value = books
            .filter { it.progress?.progressPercent ?: 0f in 0.1f..99.9f }
            .sortedByDescending { it.progress?.lastReadAt ?: 0 }
            .take(10)
        
        // Recently Added: sort by import timestamp
        _recentlyAdded.value = books
            .sortedByDescending { it.book.importTimestamp }
            .take(10)
        
        // Top Rated: books with rating >= 4 stars
        _topRated.value = books
            .filter { it.book.rating >= 4.0f }
            .sortedByDescending { it.book.rating }
            .take(10)
        
        // Group by tags
        val tagGroups = mutableMapOf<String, MutableList<BookWithMetadata>>()
        books.forEach { bookWithMeta ->
            bookWithMeta.tags.forEach { tag ->
                tagGroups.getOrPut(tag.name) { mutableListOf() }.add(bookWithMeta)
            }
        }
        _booksByTag.value = tagGroups
            .mapValues { it.value.take(10) }
            .filter { it.value.size >= 2 } // Only show tags with 2+ books
    }
    
    /**
     * Load reading analytics
     */
    private fun loadAnalytics() {
        viewModelScope.launch {
            val minutes = sessionRepository.getMinutesReadThisWeek()
            _minutesReadThisWeek.value = minutes
        }
    }
    
    /**
     * Refresh library data
     */
    fun refresh() {
        loadLibraryData()
        loadAnalytics()
    }
    
    /**
     * Show import dialog
     */
    fun showImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = true)
    }
    
    fun hideImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = false)
    }
    
    /**
     * Delete book
     */
    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            try {
                bookRepository.deleteBook(bookId)
                _uiState.value = _uiState.value.copy(
                    showDeleteConfirmation = false,
                    bookToDelete = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun showDeleteConfirmation(bookId: Long) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = true,
            bookToDelete = bookId
        )
    }
    
    fun hideDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = false,
            bookToDelete = null
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class LibraryUiState(
    val isLoading: Boolean = false,
    val showImportDialog: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val bookToDelete: Long? = null,
    val error: String? = null
)

// ============================================================================
// LIBRARY SCREEN - Main Netflix-style UI
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBookClick: (Long) -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val continueReading by viewModel.continueReading.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val topRated by viewModel.topRated.collectAsState()
    val booksByTag by viewModel.booksByTag.collectAsState()
    val minutesReadThisWeek by viewModel.minutesReadThisWeek.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val allBooks by viewModel.allBooks.collectAsState()
    
    Scaffold(
        topBar = {
            LibraryTopBar(
                minutesReadThisWeek = minutesReadThisWeek,
                onImportClick = onImportClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onImportClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import book")
            }
        },
        modifier = modifier
    ) { padding ->
        
        if (uiState.isLoading && allBooks.isEmpty()) {
            // Initial loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (allBooks.isEmpty()) {
            // Empty state
            EmptyLibraryState(
                onImportClick = onImportClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            // Main content: Carousels
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Continue Reading carousel
                if (continueReading.isNotEmpty()) {
                    item {
                        BookCarousel(
                            title = "Continue Reading",
                            books = continueReading,
                            onBookClick = onBookClick,
                            showProgress = true
                        )
                    }
                }
                
                // Recently Added carousel
                if (recentlyAdded.isNotEmpty()) {
                    item {
                        BookCarousel(
                            title = "Recently Added",
                            books = recentlyAdded,
                            onBookClick = onBookClick
                        )
                    }
                }
                
                // Top Rated carousel
                if (topRated.isNotEmpty()) {
                    item {
                        BookCarousel(
                            title = "Top Rated",
                            books = topRated,
                            onBookClick = onBookClick,
                            showRating = true
                        )
                    }
                }
                
                // Tag-based carousels
                booksByTag.forEach { (tagName, books) ->
                    item(key = "tag_$tagName") {
                        BookCarousel(
                            title = tagName,
                            books = books,
                            onBookClick = onBookClick
                        )
                    }
                }
                
                // Spacer for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
        
        // Delete confirmation dialog
        if (uiState.showDeleteConfirmation && uiState.bookToDelete != null) {
            DeleteBookDialog(
                onConfirm = { viewModel.deleteBook(uiState.bookToDelete!!) },
                onDismiss = viewModel::hideDeleteConfirmation
            )
        }
        
        // Error handling
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // Show snackbar
                viewModel.clearError()
            }
        }
    }
}

// ============================================================================
// TOP BAR WITH ANALYTICS
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(
    minutesReadThisWeek: Int,
    onImportClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Reading analytics badge
                if (minutesReadThisWeek > 0) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${minutesReadThisWeek}m this week",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onImportClick) {
                Icon(Icons.Default.FileUpload, contentDescription = "Import books")
            }
        }
    )
}

// ============================================================================
// BOOK CAROUSEL - Netflix-style horizontal scrolling
// ============================================================================

@Composable
fun BookCarousel(
    title: String,
    books: List<BookWithMetadata>,
    onBookClick: (Long) -> Unit,
    showProgress: Boolean = false,
    showRating: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Carousel title
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // Horizontal scrolling row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(
                items = books,
                key = { it.book.id }
            ) { bookWithMeta ->
                BookCard(
                    bookWithMetadata = bookWithMeta,
                    onClick = { onBookClick(bookWithMeta.book.id) },
                    showProgress = showProgress,
                    showRating = showRating
                )
            }
        }
    }
}

// ============================================================================
// BOOK CARD - Individual book display
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCard(
    bookWithMetadata: BookWithMetadata,
    onClick: () -> Unit,
    showProgress: Boolean = false,
    showRating: Boolean = false,
    modifier: Modifier = Modifier
) {
    val book = bookWithMetadata.book
    val progress = bookWithMetadata.progress
    
    Card(
        onClick = onClick,
        modifier = modifier.width(140.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            Column {
                // Book cover
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    if (book.coverPath != null) {
                        AsyncImage(
                            model = book.coverPath,
                            contentDescription = book.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Placeholder cover
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
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    }
                    
                    // Progress indicator overlay
                    if (showProgress && progress != null && progress.progressPercent > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        ) {
                            LinearProgressIndicator(
                                progress = progress.progressPercent / 100f,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
                
                // Book metadata
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Rating display
                    if (showRating && book.rating > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFB300)
                            )
                            Text(
                                text = String.format("%.1f", book.rating),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Progress percentage text
                    if (showProgress && progress != null && progress.progressPercent > 0) {
                        Text(
                            text = "${progress.progressPercent.toInt()}% complete",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Badges overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Highlight count badge
                if (bookWithMetadata.highlightCount > 0) {
                    Badge(
                        icon = Icons.Default.Bookmark,
                        count = bookWithMetadata.highlightCount
                    )
                }
                
                // Note count badge
                if (bookWithMetadata.noteCount > 0) {
                    Badge(
                        icon = Icons.Default.Note,
                        count = bookWithMetadata.noteCount
                    )
                }
            }
        }
    }
}

@Composable
fun Badge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================================================
// EMPTY STATE
// ============================================================================

@Composable
fun EmptyLibraryState(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            
            Text(
                text = "Your library is empty",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Import your first EPUB to start building your personal knowledge library",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(
                onClick = onImportClick,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Books")
            }
        }
    }
}

// ============================================================================
// DELETE CONFIRMATION DIALOG
// ============================================================================

@Composable
fun DeleteBookDialog(
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
        title = { Text("Delete Book?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This will permanently delete:")
                Text("• The book file")
                Text("• All highlights")
                Text("• All notes attached to highlights")
                Text("• Reading progress")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Standalone notes will not be deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
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
