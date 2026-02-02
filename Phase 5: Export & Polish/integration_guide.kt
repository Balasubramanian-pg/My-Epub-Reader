/**
 * Phase 5 Final: Integration Guide & Project Setup
 * 
 * Complete setup instructions for assembling all components into
 * a working Android application.
 */

// ============================================================================
// BUILD.GRADLE.KTS (MODULE LEVEL)
// ============================================================================

/**
 * File: app/build.gradle.kts
 * 
 * Complete dependency configuration for the EPUB Reader project
 */

/*
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.epreader"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.epreader"
        minSdk = 33  // Android 13+
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-MVP"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    
    // ========================================================================
    // CORE ANDROID
    // ========================================================================
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    
    // ========================================================================
    // COMPOSE
    // ========================================================================
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // ========================================================================
    // ROOM DATABASE (with FTS support)
    // ========================================================================
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    
    // ========================================================================
    // READIUM KOTLIN TOOLKIT (EPUB rendering)
    // ========================================================================
    val readiumVersion = "2.4.0"
    implementation("org.readium.kotlin-toolkit:readium-shared:$readiumVersion")
    implementation("org.readium.kotlin-toolkit:readium-streamer:$readiumVersion")
    implementation("org.readium.kotlin-toolkit:readium-navigator:$readiumVersion")
    
    // Optional: LCP DRM support (if needed)
    // implementation("org.readium.kotlin-toolkit:readium-lcp:$readiumVersion")
    
    // ========================================================================
    // DEPENDENCY INJECTION (Hilt)
    // ========================================================================
    val hiltVersion = "2.48"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")
    
    // ========================================================================
    // COROUTINES
    // ========================================================================
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // ========================================================================
    // IMAGE LOADING (Coil)
    // ========================================================================
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // ========================================================================
    // DATASTORE (for preferences)
    // ========================================================================
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // ========================================================================
    // TESTING
    // ========================================================================
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
*/

// ============================================================================
// BUILD.GRADLE.KTS (PROJECT LEVEL)
// ============================================================================

/**
 * File: build.gradle.kts (root)
 */

/*
plugins {
    id("com.android.application") version "8.1.4" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
}
*/

// ============================================================================
// DEPENDENCY INJECTION SETUP
// ============================================================================

package com.epreader.di

import android.content.Context
import androidx.room.Room
import com.epreader.data.local.EPubReaderDatabase
import com.epreader.reader.PublicationService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): EPubReaderDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            EPubReaderDatabase::class.java,
            EPubReaderDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration() // For MVP only - remove for production
        .build()
    }
    
    @Provides
    fun provideBookDao(database: EPubReaderDatabase) = database.bookDao()
    
    @Provides
    fun provideChapterDao(database: EPubReaderDatabase) = database.chapterDao()
    
    @Provides
    fun provideHighlightDao(database: EPubReaderDatabase) = database.highlightDao()
    
    @Provides
    fun provideNoteDao(database: EPubReaderDatabase) = database.noteDao()
    
    @Provides
    fun provideNoteLinkDao(database: EPubReaderDatabase) = database.noteLinkDao()
    
    @Provides
    fun provideTagDao(database: EPubReaderDatabase) = database.tagDao()
    
    @Provides
    fun provideReadingProgressDao(database: EPubReaderDatabase) = database.readingProgressDao()
    
    @Provides
    fun provideReadingSessionDao(database: EPubReaderDatabase) = database.readingSessionDao()
    
    @Provides
    fun provideSearchDao(database: EPubReaderDatabase) = database.searchDao()
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun providePublicationService(
        @ApplicationContext context: Context
    ): PublicationService {
        return PublicationService(context)
    }
}

// ============================================================================
// APPLICATION CLASS
// ============================================================================

package com.epreader

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EPubReaderApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any required services
        // Readium initialization if needed
    }
}

// ============================================================================
// MAIN ACTIVITY
// ============================================================================

package com.epreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import com.epreader.ui.EPubReaderApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            EPubReaderTheme {
                EPubReaderApp()
            }
        }
    }
}

@Composable
fun EPubReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

// ============================================================================
// ANDROID MANIFEST
// ============================================================================

/**
 * File: AndroidManifest.xml
 */

/*
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    
    <!-- Permissions -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="29" />
    
    <!-- Android 13+ uses scoped storage -->
    
    <application
        android:name=".EPubReaderApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.EPubReader"
        tools:targetApi="31">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.EPubReader">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            
            <!-- Handle EPUB file opening -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="application/epub+zip" />
            </intent-filter>
        </activity>
        
    </application>
    
</manifest>
*/

// ============================================================================
// TESTING CHECKLIST
// ============================================================================

/**
 * CRITICAL MVP ACCEPTANCE TESTS
 * 
 * Run through these flows before considering the MVP complete:
 */

object TestingChecklist {
    
    val databaseTests = listOf(
        "✓ Books table stores metadata correctly",
        "✓ Highlights persist across app restarts",
        "✓ Notes can exist without books (standalone)",
        "✓ Note links create bidirectional relationships",
        "✓ Deleting a note cleans up links (CASCADE)",
        "✓ FTS indexing returns search results",
        "✓ Reading progress updates correctly"
    )
    
    val readerTests = listOf(
        "✓ EPUB opens and renders correctly",
        "✓ Text selection works",
        "✓ Highlight creation persists",
        "✓ Note creation from highlight works",
        "✓ Last read position restored on reopen",
        "✓ Font size changes apply",
        "✓ Theme switching works (Day/Night/Sepia)",
        "✓ Progress percentage updates"
    )
    
    val notesTests = listOf(
        "✓ Create standalone note (no book)",
        "✓ Create note from highlight",
        "✓ Link note to another note",
        "✓ Backlinks appear automatically",
        "✓ Navigate between linked notes",
        "✓ Related notes suggestions appear (FTS)",
        "✓ Edit note content",
        "✓ Delete note (with confirmation)",
        "✓ Jump to passage from note works"
    )
    
    val libraryTests = listOf(
        "✓ Continue Reading shows in-progress books",
        "✓ Recently Added sorted by import date",
        "✓ Top Rated shows 4+ star books",
        "✓ Tag carousels appear when books tagged",
        "✓ Book card shows correct progress",
        "✓ Empty library shows helpful message",
        "✓ Minutes read this week displays"
    )
    
    val bookDetailsTests = listOf(
        "✓ Metadata displays correctly",
        "✓ Rating changes persist",
        "✓ Tag add/remove works",
        "✓ Highlights preview shows recent 5",
        "✓ Open Reader button works",
        "✓ Export Notes button triggers export",
        "✓ Delete book removes all data"
    )
    
    val exportTests = listOf(
        "✓ Export creates markdown files",
        "✓ YAML frontmatter correct",
        "✓ Highlighted quote in blockquote",
        "✓ Backlinks preserved",
        "✓ Links preserved",
        "✓ Folder structure correct",
        "✓ INDEX.md created per book",
        "✓ README.md created for full export",
        "✓ Files are valid markdown"
    )
    
    val navigationTests = listOf(
        "✓ Bottom nav switches tabs correctly",
        "✓ Library → Book Details → Reader flow",
        "✓ Notes → Note Detail → Linked Note flow",
        "✓ Back button works correctly",
        "✓ State preserved on tab switch",
        "✓ Deep linking works (Reader from Note)"
    )
    
    val integrationTests = listOf(
        "✓ Import EPUB → appears in library",
        "✓ Read → Highlight → Note → Link flow",
        "✓ Search finds passages and notes",
        "✓ Export → Files accessible",
        "✓ Delete book → removes from library",
        "✓ App restart preserves all data",
        "✓ Offline operation works"
    )
}

// ============================================================================
// FINAL INTEGRATION STEPS
// ============================================================================

/**
 * STEP-BY-STEP INTEGRATION GUIDE
 * 
 * 1. PROJECT SETUP
 *    - Create Android Studio project (Kotlin, Compose, API 33+)
 *    - Configure build.gradle.kts files
 *    - Add @HiltAndroidApp to Application class
 *    - Configure AndroidManifest.xml
 * 
 * 2. DATABASE LAYER
 *    - Add database_schema.kt to data/local/
 *    - Add repositories.kt to data/repository/
 *    - Set up Hilt modules (DatabaseModule)
 *    - Build and verify Room schema generation
 * 
 * 3. READIUM INTEGRATION
 *    - Add readium_integration.kt to reader/
 *    - Configure Readium dependencies
 *    - Test opening a sample EPUB
 * 
 * 4. READER UI
 *    - Add reader_ui.kt to ui/reader/
 *    - Wire up ViewModel injection
 *    - Test highlighting and note creation
 * 
 * 5. NOTES SYSTEM
 *    - Add notes_ui.kt to ui/notes/
 *    - Add notes_dialogs.kt to ui/notes/
 *    - Test note creation and linking
 *    - Verify backlinks appear
 * 
 * 6. LIBRARY UI
 *    - Add library_ui.kt to ui/library/
 *    - Add bookdetails_ui.kt to ui/bookdetails/
 *    - Test carousels and navigation
 * 
 * 7. NAVIGATION
 *    - Add navigation.kt to ui/
 *    - Wire up NavHost in MainActivity
 *    - Test all navigation flows
 * 
 * 8. EXPORT SYSTEM
 *    - Add export_engine.kt to export/
 *    - Test markdown generation
 *    - Verify file structure
 * 
 * 9. TESTING
 *    - Run through all test checklists above
 *    - Fix any issues
 *    - Test on physical device
 * 
 * 10. POLISH
 *     - Add app icon
 *     - Add splash screen
 *     - Performance testing
 *     - Edge case handling
 */

// ============================================================================
// KNOWN LIMITATIONS (MVP)
// ============================================================================

/**
 * DOCUMENTED LIMITATIONS FOR MVP
 * 
 * These are intentional scope limitations for the 5-day MVP:
 * 
 * 1. DRM Support
 *    - Basic detection only
 *    - Full LCP integration deferred
 *    - DRM books: reading allowed, indexing disabled
 * 
 * 2. EPUB Format Support
 *    - Reflowable EPUBs fully supported
 *    - Fixed-layout: basic support, no font controls
 *    - EPUB 2.x and 3.x supported via Readium
 * 
 * 3. Search Functionality
 *    - FTS5 basic tokenization
 *    - English language optimized
 *    - Multilingual: works but not optimized
 * 
 * 4. Graph Visualization
 *    - List-based views only
 *    - Interactive canvas: stretch goal
 *    - Force-directed layout: future
 * 
 * 5. Sync & Cloud
 *    - 100% local only
 *    - No cloud backup
 *    - No cross-device sync
 * 
 * 6. Import
 *    - EPUB import only
 *    - No note import (export-only)
 *    - Manual file selection
 * 
 * 7. Export Format
 *    - Markdown only
 *    - No PDF export
 *    - No HTML export
 * 
 * 8. Annotation Features
 *    - Highlights with colors
 *    - Notes with text
 *    - No drawing/sketching
 *    - No voice notes
 * 
 * 9. Reading Features
 *    - Font size, theme, scroll mode
 *    - No text-to-speech (TTS)
 *    - No custom fonts
 *    - No margin controls
 * 
 * 10. Analytics
 *     - Minutes read this week
 *     - Reading sessions
 *     - No detailed statistics
 *     - No charts/graphs
 */

// ============================================================================
// POST-MVP ROADMAP
// ============================================================================

/**
 * POTENTIAL ENHANCEMENTS (Post-MVP)
 * 
 * High Priority:
 * - File picker integration for EPUB import
 * - Interactive graph visualization
 * - Advanced search with filters
 * - Note templates
 * - Export to PDF
 * 
 * Medium Priority:
 * - Text-to-speech with synchronized highlighting
 * - Custom themes and typography
 * - Reading statistics dashboard
 * - Note versioning/history
 * - Tag hierarchy
 * 
 * Low Priority:
 * - Cloud sync (optional)
 * - Collaborative annotations
 * - Social sharing
 * - Reading goals
 * - Book recommendations
 */
