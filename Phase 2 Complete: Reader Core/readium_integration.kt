/**
 * Phase 2: Reader Core - Readium Integration
 * 
 * This file provides EPUB rendering using Readium Kotlin Toolkit
 * with support for:
 * - CFI-based location tracking
 * - Text selection and highlighting
 * - Persistent reading position
 * - DRM detection
 */

package com.epreader.reader

import android.content.Context
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.util.Try
import org.readium.r2.streamer.Streamer
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// ============================================================================
// READIUM CONFIGURATION & SETUP
// ============================================================================

/**
 * Central service for managing EPUB publications
 * Handles opening, closing, and tracking active publications
 */
@Singleton
class PublicationService @Inject constructor(
    private val context: Context
) {
    
    private val streamer = Streamer(context)
    
    // Currently open publications keyed by book ID
    private val openPublications = mutableMapOf<Long, Publication>()
    
    /**
     * Open an EPUB file and return Publication
     * Detects DRM and returns appropriate result
     */
    suspend fun openPublication(bookId: Long, filePath: String): PublicationResult {
        // Check if already open
        openPublications[bookId]?.let {
            return PublicationResult.Success(it, isDrmProtected = false)
        }
        
        val file = File(filePath)
        if (!file.exists()) {
            return PublicationResult.Error("File not found: $filePath")
        }
        
        // Open with Readium Streamer
        val asset = streamer.open(file, allowUserInteraction = false)
        
        return when (asset) {
            is Try.Success -> {
                val publication = asset.value.publication
                
                // Check for DRM restrictions
                val isDrmProtected = publication.isRestricted
                
                openPublications[bookId] = publication
                
                PublicationResult.Success(publication, isDrmProtected)
            }
            is Try.Failure -> {
                PublicationResult.Error("Failed to open: ${asset.value.message}")
            }
        }
    }
    
    /**
     * Close publication and clean up resources
     */
    fun closePublication(bookId: Long) {
        openPublications.remove(bookId)?.close()
    }
    
    /**
     * Get currently open publication
     */
    fun getPublication(bookId: Long): Publication? {
        return openPublications[bookId]
    }
}

sealed class PublicationResult {
    data class Success(val publication: Publication, val isDrmProtected: Boolean) : PublicationResult()
    data class Error(val message: String) : PublicationResult()
}

// ============================================================================
// READING STATE MANAGEMENT
// ============================================================================

/**
 * Manages reading state for a single book
 * Tracks current location, highlights, and selection
 */
data class ReadingState(
    val bookId: Long,
    val currentLocator: Locator? = null,
    val progressPercent: Float = 0f,
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeSelection: TextSelection? = null
)

data class TextSelection(
    val text: String,
    val locator: Locator,
    val rect: android.graphics.RectF? = null
)

/**
 * ViewModel-like controller for reading session
 * Coordinates between UI, Readium, and database
 */
class ReadingController @Inject constructor(
    private val publicationService: PublicationService,
    private val bookRepository: com.epreader.data.repository.BookRepository,
    private val highlightRepository: com.epreader.data.repository.HighlightRepository,
    private val sessionRepository: com.epreader.data.repository.ReadingSessionRepository
) {
    
    private val _state = MutableStateFlow<ReadingState?>(null)
    val state: StateFlow<ReadingState?> = _state.asStateFlow()
    
    private var currentSessionId: Long? = null
    
    /**
     * Start reading session for a book
     */
    suspend fun startReading(bookId: Long, filePath: String): Boolean {
        _state.value = ReadingState(bookId = bookId, isLoading = true)
        
        // Open publication
        when (val result = publicationService.openPublication(bookId, filePath)) {
            is PublicationResult.Success -> {
                // Start session tracking
                currentSessionId = sessionRepository.startSession(bookId)
                
                // Get last read position
                val progress = sessionRepository.getProgressByBook(bookId)
                val locator = progress?.lastCfi?.let { cfi ->
                    // Convert CFI string to Locator
                    // This requires parsing - simplified here
                    null // TODO: Parse CFI to Locator
                }
                
                _state.value = ReadingState(
                    bookId = bookId,
                    currentLocator = locator,
                    progressPercent = progress?.progressPercent ?: 0f,
                    isLoading = false
                )
                
                return true
            }
            is PublicationResult.Error -> {
                _state.value = ReadingState(
                    bookId = bookId,
                    isLoading = false,
                    error = result.message
                )
                return false
            }
        }
    }
    
    /**
     * Update current reading position
     * Called when user navigates to new location
     */
    fun updateLocation(locator: Locator, progressPercent: Float) {
        val currentState = _state.value ?: return
        
        _state.value = currentState.copy(
            currentLocator = locator,
            progressPercent = progressPercent
        )
    }
    
    /**
     * Handle text selection from reader
     */
    fun onTextSelected(selection: TextSelection) {
        val currentState = _state.value ?: return
        _state.value = currentState.copy(activeSelection = selection)
    }
    
    /**
     * Clear current selection
     */
    fun clearSelection() {
        val currentState = _state.value ?: return
        _state.value = currentState.copy(activeSelection = null)
    }
    
    /**
     * Create highlight from current selection
     */
    suspend fun createHighlightFromSelection(color: String = "#FFEB3B"): Long? {
        val currentState = _state.value ?: return null
        val selection = currentState.activeSelection ?: return null
        
        // Create highlight in database
        val highlightId = highlightRepository.createHighlight(
            bookId = currentState.bookId,
            chapterId = null, // TODO: Extract from locator
            cfiOrRange = selection.locator.locations.fragments.firstOrNull() ?: "",
            text = selection.text,
            color = color
        )
        
        // Clear selection after creating highlight
        clearSelection()
        
        return highlightId
    }
    
    /**
     * End reading session and persist progress
     */
    suspend fun endReading() {
        val currentState = _state.value ?: return
        val sessionId = currentSessionId ?: return
        val locator = currentState.currentLocator ?: return
        
        // Save session and progress
        sessionRepository.endSession(
            sessionId = sessionId,
            bookId = currentState.bookId,
            currentCfi = locator.locations.fragments.firstOrNull() ?: "",
            progressPercent = currentState.progressPercent
        )
        
        // Close publication
        publicationService.closePublication(currentState.bookId)
        
        _state.value = null
        currentSessionId = null
    }
}

// ============================================================================
// LOCATOR EXTENSIONS & UTILITIES
// ============================================================================

/**
 * Extension functions for working with Readium Locators
 */
object LocatorUtils {
    
    /**
     * Convert Locator to CFI string for database storage
     */
    fun Locator.toCfi(): String {
        return this.locations.fragments.firstOrNull() ?: ""
    }
    
    /**
     * Create Locator from CFI string
     * This is a simplified implementation - production would be more robust
     */
    fun fromCfi(
        cfi: String,
        publication: Publication,
        href: String
    ): Locator? {
        if (cfi.isEmpty()) return null
        
        return Locator(
            href = href,
            type = "application/epub+zip",
            locations = Locator.Locations(
                fragments = listOf(cfi),
                progression = null,
                position = null,
                totalProgression = null
            )
        )
    }
    
    /**
     * Calculate progress percentage from locator
     */
    fun calculateProgress(locator: Locator, publication: Publication): Float {
        // Use totalProgression if available
        locator.locations.totalProgression?.let {
            return (it * 100).toFloat()
        }
        
        // Fallback: estimate from position
        locator.locations.position?.let { pos ->
            val totalPositions = publication.positions.size
            if (totalPositions > 0) {
                return ((pos.toFloat() / totalPositions) * 100).coerceIn(0f, 100f)
            }
        }
        
        return 0f
    }
}

// ============================================================================
// HIGHLIGHT OVERLAY DATA
// ============================================================================

/**
 * Data structure for rendering highlights in the reader
 * Maps database highlights to visual decorations
 */
data class HighlightDecoration(
    val id: Long,
    val locator: Locator,
    val color: String,
    val text: String
)

/**
 * Service to convert database highlights to reader decorations
 */
class HighlightDecorationService @Inject constructor(
    private val highlightRepository: com.epreader.data.repository.HighlightRepository,
    private val publicationService: PublicationService
) {
    
    /**
     * Load all highlights for a book as decorations
     */
    suspend fun loadHighlightsForBook(
        bookId: Long
    ): List<HighlightDecoration> {
        val publication = publicationService.getPublication(bookId) ?: return emptyList()
        
        return highlightRepository.getHighlightsByBook(bookId)
            .collect { highlights ->
                highlights.mapNotNull { highlight ->
                    // Convert CFI to Locator
                    val locator = LocatorUtils.fromCfi(
                        cfi = highlight.cfiOrRange,
                        publication = publication,
                        href = "" // TODO: Extract from stored data
                    ) ?: return@mapNotNull null
                    
                    HighlightDecoration(
                        id = highlight.id,
                        locator = locator,
                        color = highlight.color,
                        text = highlight.text
                    )
                }
            }
        
        return emptyList()
    }
}

// ============================================================================
// TEXT EXTRACTION FOR INDEXING
// ============================================================================

/**
 * Utility to extract full text from EPUB for FTS indexing
 * This runs during import, not during reading
 */
class TextExtractionService @Inject constructor(
    private val context: Context
) {
    
    /**
     * Extract all readable text from EPUB
     * Returns list of (chapter, text, cfi) tuples
     */
    suspend fun extractFullText(
        filePath: String
    ): List<ChapterText> {
        val streamer = Streamer(context)
        val file = File(filePath)
        
        val asset = streamer.open(file, allowUserInteraction = false)
        
        return when (asset) {
            is Try.Success -> {
                val publication = asset.value.publication
                val chapters = mutableListOf<ChapterText>()
                
                // Iterate through reading order
                publication.readingOrder.forEachIndexed { index, link ->
                    try {
                        // Get resource content
                        val resource = publication.get(link)
                        val text = resource?.readAsString()?.getOrNull()
                        
                        if (text != null) {
                            chapters.add(
                                ChapterText(
                                    chapterIndex = index,
                                    href = link.href,
                                    text = cleanHtmlText(text),
                                    cfi = "" // Generate base CFI for chapter
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // Log error but continue
                        println("Error extracting chapter $index: ${e.message}")
                    }
                }
                
                publication.close()
                chapters
            }
            is Try.Failure -> {
                emptyList()
            }
        }
    }
    
    /**
     * Clean HTML tags and entities from text
     */
    private fun cleanHtmlText(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), " ") // Remove HTML tags
            .replace(Regex("&[a-z]+;"), " ") // Remove entities
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
    }
}

data class ChapterText(
    val chapterIndex: Int,
    val href: String,
    val text: String,
    val cfi: String
)

// ============================================================================
// DRM DETECTION & HANDLING
// ============================================================================

/**
 * DRM detection and user messaging
 */
object DrmHandler {
    
    /**
     * Check if publication has DRM restrictions
     */
    fun checkDrmStatus(publication: Publication): DrmStatus {
        return if (publication.isRestricted) {
            // Check which rights are restricted
            val hasTextAccess = !publication.rights.copy.isRestricted
            val hasPrint = !publication.rights.print.isRestricted
            
            if (!hasTextAccess) {
                DrmStatus.DRM_PROTECTED // Cannot extract text
            } else {
                DrmStatus.NONE // Has restrictions but text accessible
            }
        } else {
            DrmStatus.NONE
        }
    }
    
    /**
     * Get user-facing message for DRM status
     */
    fun getDrmMessage(status: DrmStatus): String? {
        return when (status) {
            DrmStatus.DRM_PROTECTED -> 
                "This book is DRM-protected. Some features like full-text search and note linking may be limited."
            DrmStatus.NONE -> null
            DrmStatus.UNKNOWN -> null
        }
    }
}

// Import DrmStatus from database schema
typealias DrmStatus = com.epreader.data.local.DrmStatus

// ============================================================================
// READING PREFERENCES
// ============================================================================

/**
 * User preferences for reading experience
 */
data class ReadingPreferences(
    val fontSize: Float = 16f,
    val fontFamily: String = "serif",
    val lineHeight: Float = 1.5f,
    val theme: ReaderTheme = ReaderTheme.DAY,
    val scrollMode: ScrollMode = ScrollMode.PAGINATED
)

enum class ReaderTheme {
    DAY,
    NIGHT,
    SEPIA
}

enum class ScrollMode {
    PAGINATED,
    CONTINUOUS
}

/**
 * Manager for reading preferences
 * In production: persist to SharedPreferences or DataStore
 */
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    
    private val _preferences = MutableStateFlow(ReadingPreferences())
    val preferences: StateFlow<ReadingPreferences> = _preferences.asStateFlow()
    
    fun updateFontSize(size: Float) {
        _preferences.value = _preferences.value.copy(fontSize = size.coerceIn(12f, 32f))
    }
    
    fun updateTheme(theme: ReaderTheme) {
        _preferences.value = _preferences.value.copy(theme = theme)
    }
    
    fun updateScrollMode(mode: ScrollMode) {
        _preferences.value = _preferences.value.copy(scrollMode = mode)
    }
    
    fun updateLineHeight(height: Float) {
        _preferences.value = _preferences.value.copy(lineHeight = height.coerceIn(1.0f, 2.0f))
    }
}

// ============================================================================
// INTEGRATION NOTES
// ============================================================================

/**
 * READIUM KOTLIN TOOLKIT DEPENDENCIES
 * 
 * Add to build.gradle.kts:
 * 
 * dependencies {
 *     // Readium core
 *     implementation("org.readium.kotlin-toolkit:readium-shared:2.4.0")
 *     implementation("org.readium.kotlin-toolkit:readium-streamer:2.4.0")
 *     implementation("org.readium.kotlin-toolkit:readium-navigator:2.4.0")
 *     
 *     // Optional: LCP DRM support
 *     implementation("org.readium.kotlin-toolkit:readium-lcp:2.4.0")
 *     
 *     // Coroutines
 *     implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
 * }
 * 
 * CRITICAL SETUP STEPS:
 * 
 * 1. Initialize Readium in Application class
 * 2. Configure storage paths for publications
 * 3. Handle Android scoped storage permissions
 * 4. Set up proper lifecycle management for publications
 * 
 * KNOWN LIMITATIONS:
 * 
 * - This implementation uses Readium's CFI system for locations
 * - DRM detection is basic - LCP requires additional setup
 * - Text extraction is synchronous - should use WorkManager for large books
 * - Locator serialization is simplified - production needs robust parsing
 */
