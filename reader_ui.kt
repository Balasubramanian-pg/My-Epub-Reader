/**
 * Phase 2: Reader Screen UI (Jetpack Compose)
 * 
 * Complete reader interface with:
 * - EPUB rendering via Readium
 * - Text selection and highlighting
 * - Note creation modal
 * - Reading controls
 * - Progress tracking
 */

package com.epreader.ui.reader

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epreader.reader.*
import com.epreader.data.local.Highlight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================================
// READER VIEWMODEL
// ============================================================================

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val readingController: ReadingController,
    private val preferencesManager: PreferencesManager,
    private val highlightRepository: com.epreader.data.repository.HighlightRepository,
    private val noteRepository: com.epreader.data.repository.NoteRepository
) : ViewModel() {
    
    val readingState: StateFlow<ReadingState?> = readingController.state
    val preferences: StateFlow<ReadingPreferences> = preferencesManager.preferences
    
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState
    
    /**
     * Open book and start reading session
     */
    fun openBook(bookId: Long, filePath: String) {
        viewModelScope.launch {
            val success = readingController.startReading(bookId, filePath)
            if (!success) {
                _uiState.value = _uiState.value.copy(
                    showError = true,
                    errorMessage = "Failed to open book"
                )
            }
        }
    }
    
    /**
     * Handle location changes from navigator
     */
    fun onLocationChanged(locator: org.readium.r2.shared.publication.Locator, progress: Float) {
        readingController.updateLocation(locator, progress)
    }
    
    /**
     * Handle text selection
     */
    fun onTextSelected(selection: TextSelection) {
        readingController.onTextSelected(selection)
        _uiState.value = _uiState.value.copy(
            showSelectionMenu = true
        )
    }
    
    /**
     * Clear selection
     */
    fun clearSelection() {
        readingController.clearSelection()
        _uiState.value = _uiState.value.copy(
            showSelectionMenu = false
        )
    }
    
    /**
     * Create highlight with selected color
     */
    fun createHighlight(color: String) {
        viewModelScope.launch {
            val highlightId = readingController.createHighlightFromSelection(color)
            if (highlightId != null) {
                _uiState.value = _uiState.value.copy(
                    lastCreatedHighlightId = highlightId,
                    showNoteCreation = true
                )
            }
        }
    }
    
    /**
     * Create note from highlight
     */
    fun createNoteFromHighlight(title: String, body: String) {
        viewModelScope.launch {
            val highlightId = _uiState.value.lastCreatedHighlightId ?: return@launch
            
            noteRepository.createNoteFromHighlight(
                highlightId = highlightId,
                title = title,
                body = body
            )
            
            _uiState.value = _uiState.value.copy(
                showNoteCreation = false,
                lastCreatedHighlightId = null
            )
        }
    }
    
    /**
     * Skip note creation
     */
    fun skipNoteCreation() {
        _uiState.value = _uiState.value.copy(
            showNoteCreation = false,
            lastCreatedHighlightId = null
        )
    }
    
    /**
     * Toggle UI controls visibility
     */
    fun toggleControls() {
        _uiState.value = _uiState.value.copy(
            showControls = !_uiState.value.showControls
        )
    }
    
    /**
     * Font size adjustments
     */
    fun increaseFontSize() {
        val current = preferences.value.fontSize
        preferencesManager.updateFontSize(current + 2f)
    }
    
    fun decreaseFontSize() {
        val current = preferences.value.fontSize
        preferencesManager.updateFontSize(current - 2f)
    }
    
    /**
     * Theme toggle
     */
    fun toggleTheme() {
        val current = preferences.value.theme
        val next = when (current) {
            ReaderTheme.DAY -> ReaderTheme.NIGHT
            ReaderTheme.NIGHT -> ReaderTheme.SEPIA
            ReaderTheme.SEPIA -> ReaderTheme.DAY
        }
        preferencesManager.updateTheme(next)
    }
    
    /**
     * Close book and end session
     */
    fun closeBook() {
        viewModelScope.launch {
            readingController.endReading()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            readingController.endReading()
        }
    }
}

data class ReaderUiState(
    val showControls: Boolean = false,
    val showSelectionMenu: Boolean = false,
    val showNoteCreation: Boolean = false,
    val lastCreatedHighlightId: Long? = null,
    val showError: Boolean = false,
    val errorMessage: String? = null
)

// ============================================================================
// READER SCREEN COMPOSABLE
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: Long,
    filePath: String,
    viewModel: ReaderViewModel,
    onNavigateBack: () -> Unit
) {
    val readingState by viewModel.readingState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    
    // Open book on first composition
    LaunchedEffect(bookId) {
        viewModel.openBook(bookId, filePath)
    }
    
    // Cleanup on exit
    DisposableEffect(Unit) {
        onDispose {
            viewModel.closeBook()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Main reader content
        when {
            readingState?.isLoading == true -> {
                LoadingIndicator()
            }
            readingState?.error != null -> {
                ErrorDisplay(
                    message = readingState?.error ?: "Unknown error",
                    onRetry = { viewModel.openBook(bookId, filePath) }
                )
            }
            readingState != null -> {
                ReaderContent(
                    state = readingState!!,
                    preferences = preferences,
                    onLocationChanged = viewModel::onLocationChanged,
                    onTextSelected = viewModel::onTextSelected,
                    onTap = { viewModel.toggleControls() }
                )
            }
        }
        
        // Top overlay (progress bar and title)
        AnimatedVisibility(
            visible = uiState.showControls,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopControls(
                progress = readingState?.progressPercent ?: 0f,
                onClose = {
                    viewModel.closeBook()
                    onNavigateBack()
                }
            )
        }
        
        // Bottom overlay (reading controls)
        AnimatedVisibility(
            visible = uiState.showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomControls(
                fontSize = preferences.fontSize,
                theme = preferences.theme,
                onFontSizeIncrease = viewModel::increaseFontSize,
                onFontSizeDecrease = viewModel::decreaseFontSize,
                onThemeToggle = viewModel::toggleTheme
            )
        }
        
        // Selection menu (highlight colors)
        if (uiState.showSelectionMenu) {
            SelectionMenu(
                selection = readingState?.activeSelection,
                onHighlight = viewModel::createHighlight,
                onCancel = viewModel::clearSelection,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Note creation dialog
        if (uiState.showNoteCreation) {
            NoteCreationDialog(
                highlightText = readingState?.activeSelection?.text ?: "",
                onSave = viewModel::createNoteFromHighlight,
                onDismiss = viewModel::skipNoteCreation
            )
        }
    }
}

// ============================================================================
// READER CONTENT (Readium Integration)
// ============================================================================

@Composable
fun ReaderContent(
    state: ReadingState,
    preferences: ReadingPreferences,
    onLocationChanged: (org.readium.r2.shared.publication.Locator, Float) -> Unit,
    onTextSelected: (TextSelection) -> Unit,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    
    // Apply theme background
    val backgroundColor = when (preferences.theme) {
        ReaderTheme.DAY -> Color.White
        ReaderTheme.NIGHT -> Color(0xFF1A1A1A)
        ReaderTheme.SEPIA -> Color(0xFFF4ECD8)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Readium Navigator View
        // Note: This is a simplified integration placeholder
        // Production implementation requires actual EpubNavigatorFragment integration
        AndroidView(
            factory = { ctx ->
                // Create Readium navigator view
                // This would integrate with EpubNavigatorFragment
                android.widget.TextView(ctx).apply {
                    text = "EPUB Content Here\n\n(Readium Navigator Integration)"
                    textSize = preferences.fontSize
                    setPadding(32, 32, 32, 32)
                    setTextColor(
                        when (preferences.theme) {
                            ReaderTheme.DAY -> android.graphics.Color.BLACK
                            ReaderTheme.NIGHT -> android.graphics.Color.WHITE
                            ReaderTheme.SEPIA -> android.graphics.Color.parseColor("#5C4A3A")
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Tap detector for showing/hiding controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemGesturesPadding()
                // TODO: Add tap gesture detector
        )
    }
}

// ============================================================================
// UI COMPONENTS
// ============================================================================

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Opening book...")
        }
    }
}

@Composable
fun ErrorDisplay(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopControls(
    progress: Float,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 4.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Close book"
                    )
                }
                
                Text(
                    text = "${progress.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BottomControls(
    fontSize: Float,
    theme: ReaderTheme,
    onFontSizeIncrease: () -> Unit,
    onFontSizeDecrease: () -> Unit,
    onThemeToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Font size controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onFontSizeDecrease) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease font size"
                    )
                }
                Text(
                    text = "${fontSize.toInt()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(onClick = onFontSizeIncrease) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase font size"
                    )
                }
            }
            
            // Theme toggle
            IconButton(onClick = onThemeToggle) {
                Icon(
                    imageVector = when (theme) {
                        ReaderTheme.DAY -> Icons.Default.LightMode
                        ReaderTheme.NIGHT -> Icons.Default.DarkMode
                        ReaderTheme.SEPIA -> Icons.Default.Contrast
                    },
                    contentDescription = "Toggle theme"
                )
            }
        }
    }
}

@Composable
fun SelectionMenu(
    selection: TextSelection?,
    onHighlight: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Highlight",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (selection != null) {
                Text(
                    text = selection.text.take(100) + if (selection.text.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Color options
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HighlightColorButton(
                    color = Color(0xFFFFEB3B),
                    label = "Yellow",
                    onClick = { onHighlight("#FFEB3B") }
                )
                HighlightColorButton(
                    color = Color(0xFF4CAF50),
                    label = "Green",
                    onClick = { onHighlight("#4CAF50") }
                )
                HighlightColorButton(
                    color = Color(0xFF2196F3),
                    label = "Blue",
                    onClick = { onHighlight("#2196F3") }
                )
                HighlightColorButton(
                    color = Color(0xFFFF5722),
                    label = "Red",
                    onClick = { onHighlight("#FF5722") }
                )
            }
            
            TextButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun HighlightColorButton(
    color: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilledIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = color
            ),
            modifier = Modifier.size(48.dp)
        ) {
            // Empty - just colored button
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCreationDialog(
    highlightText: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Note") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show highlight context
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "\"${highlightText.take(150)}${if (highlightText.length > 150) "..." else ""}\"",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Your thoughts") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (title.isNotBlank() && body.isNotBlank()) {
                        onSave(title, body)
                    }
                },
                enabled = title.isNotBlank() && body.isNotBlank()
            ) {
                Text("Save Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        }
    )
}

// ============================================================================
// INTEGRATION NOTES FOR ACTUAL READIUM NAVIGATOR
// ============================================================================

/**
 * PRODUCTION READIUM INTEGRATION STEPS:
 * 
 * 1. Create Fragment Container:
 *    - Use AndroidView with FragmentContainerView
 *    - Manage EpubNavigatorFragment lifecycle
 * 
 * 2. Setup Navigator Listeners:
 *    ```kotlin
 *    navigator.addListener(object : Navigator.Listener {
 *        override fun onLocationChanged(locator: Locator) {
 *            onLocationChanged(locator, calculateProgress(locator))
 *        }
 *    })
 *    ```
 * 
 * 3. Handle Text Selection:
 *    - Implement SelectableNavigator.Listener
 *    - Extract selected text and locator
 *    - Show selection menu
 * 
 * 4. Apply Preferences:
 *    - Use navigator.settings to apply fontSize, theme, etc.
 *    - Update UserSettings object
 * 
 * 5. Render Highlights:
 *    - Use Decoration API to overlay highlights
 *    - Map database highlights to Decoration objects
 * 
 * 6. Handle CFI properly:
 *    - Use Readium's Locator.Text for precise positioning
 *    - Store full locator JSON in database
 */
