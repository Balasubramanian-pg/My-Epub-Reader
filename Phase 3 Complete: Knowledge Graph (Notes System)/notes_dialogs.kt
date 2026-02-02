/**
 * Phase 3 Continued: Dialogs and Utilities for Notes System
 * 
 * - Create note dialog
 * - Link note dialog with search
 * - Delete confirmation
 * - Utility functions
 */

package com.epreader.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.epreader.data.local.Note
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================
// CREATE NOTE DIALOG
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoteDialog(
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LightbulbOutline, contentDescription = null)
                Text("Create Standalone Note")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Create a note that exists independently of any book. You can link it to other notes later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("Enter a descriptive title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Your thoughts") },
                    placeholder = { Text("What's on your mind?") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (title.isNotBlank() && body.isNotBlank()) {
                        onSave(title.trim(), body.trim())
                    }
                },
                enabled = title.isNotBlank() && body.isNotBlank()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Note")
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
// LINK NOTE DIALOG - CRITICAL FOR KNOWLEDGE GRAPH
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkNoteDialog(
    currentNoteId: Long,
    availableNotes: List<Note>,
    onLink: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter notes based on search
    val filteredNotes = remember(searchQuery, availableNotes) {
        if (searchQuery.isBlank()) {
            availableNotes
        } else {
            availableNotes.filter { note ->
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.body.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AddLink, contentDescription = null)
                Text("Link to Another Note")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Connect this note to another idea in your knowledge graph. Links are bidirectional—both notes will show the connection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search notes to link...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Notes list
                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (searchQuery.isBlank()) {
                                    "No other notes available to link"
                                } else {
                                    "No notes match your search"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredNotes,
                            key = { it.id }
                        ) { note ->
                            LinkableNoteCard(
                                note = note,
                                onClick = { onLink(note.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun LinkableNoteCard(
    note: Note,
    onClick: () -> Unit
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDate(note.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = "Link to this note",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ============================================================================
// DELETE CONFIRMATION DIALOG
// ============================================================================

@Composable
fun DeleteConfirmationDialog(
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
        title = { Text("Delete Note?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This will permanently delete this note and all its links.")
                Text(
                    text = "Notes that link to this one will lose the connection, but won't be deleted.",
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

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Format timestamp to readable date string
 */
fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * Format full date for metadata
 */
fun formatFullDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// ============================================================================
// PREVIEW PROVIDERS (for Compose previews)
// ============================================================================

object PreviewData {
    val sampleNote = Note(
        id = 1,
        title = "The Power of Habit",
        body = "Habits form through a loop: cue, routine, reward. To change a habit, keep the cue and reward but change the routine.",
        createdAt = System.currentTimeMillis() - 86400000,
        modifiedAt = System.currentTimeMillis()
    )
    
    val sampleNotes = listOf(
        sampleNote,
        Note(
            id = 2,
            title = "Flow State",
            body = "Optimal experience occurs when challenge matches skill level. Too easy = boredom, too hard = anxiety.",
            createdAt = System.currentTimeMillis() - 172800000,
            modifiedAt = System.currentTimeMillis() - 172800000
        ),
        Note(
            id = 3,
            title = "Atomic Habits",
            body = "Small changes compound over time. 1% better each day = 37x better in a year.",
            createdAt = System.currentTimeMillis() - 259200000,
            modifiedAt = System.currentTimeMillis() - 259200000,
            bookId = 1
        )
    )
}

// ============================================================================
// KNOWLEDGE GRAPH VISUALIZATION (Stretch Goal - Placeholder)
// ============================================================================

/**
 * Graph visualization component
 * This is a placeholder for future interactive graph view
 * 
 * Implementation options:
 * 1. Canvas-based force-directed graph
 * 2. WebView with D3.js
 * 3. Custom Compose drawing
 */
@Composable
fun GraphVisualizationScreen(
    notes: List<NoteWithLinks>,
    onNoteClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Implement interactive graph visualization
    // For MVP, this is deferred to stretch goals
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Graph Visualization",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Interactive graph view coming soon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${notes.size} notes in your knowledge graph",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================================
// INTEGRATION NOTES
// ============================================================================

/**
 * KNOWLEDGE GRAPH IMPLEMENTATION CHECKLIST
 * 
 * ✓ Note creation (standalone + from highlights)
 * ✓ Note editing
 * ✓ Note deletion with cleanup
 * ✓ Link creation (bidirectional)
 * ✓ Link removal
 * ✓ Backlinks display (memory mechanism)
 * ✓ Related notes via FTS (suggestions)
 * ✓ Navigation between notes
 * ✓ Jump to source (highlight → reader)
 * ✓ Search and filter
 * 
 * CONCEPTUAL MODEL ENFORCED:
 * 
 * 1. Notes are first-class ✓
 *    - Can exist without books
 *    - Can exist without highlights
 *    - Standalone creation dialog
 * 
 * 2. Links are explicit ✓
 *    - User must manually create links
 *    - Search-based linking UI
 *    - No automatic linking
 * 
 * 3. Backlinks are automatic ✓
 *    - Database relationship ensures bidirectionality
 *    - UI shows "Referenced By" section
 *    - Navigation works both directions
 * 
 * 4. FTS suggests, graph remembers ✓
 *    - Related notes shown separately
 *    - User decides whether to link
 *    - Explicit links persist
 * 
 * 5. Source preservation ✓
 *    - Highlights linked to notes
 *    - Book context shown
 *    - Jump to passage available
 * 
 * USAGE FLOWS:
 * 
 * Flow 1: Reading-first
 * - Read book → Highlight → Create note → Link to existing note
 * 
 * Flow 2: Thinking-first
 * - Create standalone note → Later link to book notes
 * 
 * Flow 3: Linking-later
 * - Browse notes → Discover connection → Create link
 * 
 * Flow 4: Rediscovery
 * - Search → Find old note → See backlinks → Navigate graph
 * 
 * NEXT STEPS FOR GRAPH ENHANCEMENT:
 * 
 * 1. Graph clustering by tags/topics
 * 2. Link strength/frequency metrics
 * 3. Temporal graph evolution view
 * 4. Export graph as DOT/GEXF
 * 5. Interactive canvas visualization
 */
