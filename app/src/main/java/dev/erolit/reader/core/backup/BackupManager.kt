package dev.erolit.reader.core.backup

import androidx.room.withTransaction
import dev.erolit.reader.core.db.AppDatabase
import dev.erolit.reader.core.db.HistoryEntity
import dev.erolit.reader.core.db.LocalAuthorEntity
import dev.erolit.reader.core.db.LocalSavedEntity
import dev.erolit.reader.core.db.LocalSeriesEntity
import dev.erolit.reader.core.db.LocalSeriesWorkEntity
import dev.erolit.reader.core.db.ProgressEntity
import dev.erolit.reader.core.settings.AppSettings
import dev.erolit.reader.core.settings.SettingsRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Raised when a selected file is not a backup this app can restore. */
class BackupException(message: String) : Exception(message)

data class BackupStats(
    val stories: Int,
    val series: Int,
    val authors: Int,
    val historyEntries: Int,
)

data class BackupExport(
    val json: String,
    val stats: BackupStats,
)

private const val BACKUP_FORMAT = "erolit-backup"
private const val BACKUP_VERSION = 1

private val BackupJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

/**
 * Reads and writes the local user state as a versioned JSON document.
 *
 * Credentials deliberately never enter a backup: the session secret lives in
 * the Android Keystore and is not exported. Missing fields in older (or newer)
 * files fall back to the defaults below, so version 1 backups keep importing
 * after later updates add more state.
 */
class BackupManager(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
) {
    suspend fun export(): BackupExport {
        val stories = db.localSavedDao().all()
        val seriesRows = db.localSeriesDao().allSeries()
        val worksBySeries = db.localSeriesDao().allWorks().groupBy { it.seriesId }
        val authors = db.localAuthorDao().all()
        val progress = db.progressDao().all()
        val history = db.historyDao().all()

        val file = BackupFile(
            exportedAt = System.currentTimeMillis(),
            settings = settings.current().toBackup(),
            savedStories = stories.map { SavedStoryBackup(story = it.toSnapshot(), savedAt = it.savedAt) },
            savedSeries = seriesRows.map { row ->
                SavedSeriesBackup(
                    seriesId = row.seriesId,
                    title = row.title,
                    authorId = row.authorId,
                    authorName = row.authorName,
                    description = row.description,
                    state = row.state,
                    workCount = row.workCount,
                    savedAt = row.savedAt,
                    works = worksBySeries[row.seriesId].orEmpty().map { work ->
                        SavedSeriesWorkBackup(
                            position = work.position,
                            story = work.toSnapshot(),
                        )
                    },
                )
            },
            savedAuthors = authors.map { it.toBackup() },
            progress = progress.map { it.toBackup() },
            history = history.map {
                HistoryBackup(openedAt = it.openedAt, story = it.toSnapshot())
            },
        )

        return BackupExport(
            json = BackupJson.encodeToString(file),
            stats = BackupStats(
                stories = stories.size,
                series = seriesRows.size,
                authors = authors.size,
                historyEntries = history.size,
            ),
        )
    }

    suspend fun import(raw: String): BackupStats {
        val file = try {
            BackupJson.decodeFromString<BackupFile>(raw.trim())
        } catch (e: Exception) {
            throw BackupException("This file is not a valid Erolit backup.")
        }
        if (file.format != BACKUP_FORMAT) {
            throw BackupException("This file is not an Erolit backup.")
        }
        if (file.version > BACKUP_VERSION) {
            throw BackupException(
                "This backup was made by a newer version of Erolit. Update the app first.",
            )
        }
        if (file.version < 1) {
            throw BackupException("This backup file version is not supported.")
        }

        val savedStories = file.savedStories.map { it.story.toEntity(savedAt = it.savedAt) }
        val series = file.savedSeries.map { it.toEntity() }
        val seriesIds = series.map { it.seriesId }.toSet()
        val seriesWorks = file.savedSeries.flatMap { saved ->
            saved.works.mapNotNull { work ->
                if (saved.seriesId !in seriesIds) return@mapNotNull null
                work.story.toWorkEntity(seriesId = saved.seriesId, position = work.position)
            }
        }
        val authors = file.savedAuthors.map { it.toEntity() }
        val progress = file.progress.map { it.toEntity() }
        val history = file.history.map { it.story.toHistoryEntity(openedAt = it.openedAt) }

        db.withTransaction {
            db.localSavedDao().clear()
            db.localSavedDao().insertAll(savedStories)

            db.localSeriesDao().clearAll()
            db.localSeriesDao().upsertSeriesList(series)
            db.localSeriesDao().insertWorks(seriesWorks)

            db.localAuthorDao().clear()
            db.localAuthorDao().insertAll(authors)

            db.progressDao().clear()
            db.progressDao().upsertAll(progress)

            db.historyDao().clear()
            db.historyDao().insertAll(history)
        }

        // Apply settings last, so a database failure cannot leave the reader
        // configured for a backup that was not restored.
        settings.restore(file.settings.toSettings())

        return BackupStats(
            stories = savedStories.size,
            series = series.size,
            authors = authors.size,
            historyEntries = history.size,
        )
    }
}

@Serializable
private data class BackupFile(
    val format: String = BACKUP_FORMAT,
    val version: Int = BACKUP_VERSION,
    val exportedAt: Long = 0L,
    val settings: SettingsBackup = SettingsBackup(),
    val savedStories: List<SavedStoryBackup> = emptyList(),
    val savedSeries: List<SavedSeriesBackup> = emptyList(),
    val savedAuthors: List<SavedAuthorBackup> = emptyList(),
    val progress: List<ProgressBackup> = emptyList(),
    val history: List<HistoryBackup> = emptyList(),
)

/** Story metadata shared by saved stories, series chapters, and history. */
@Serializable
private data class StorySnapshot(
    val storyId: Long = 0L,
    val slug: String? = null,
    val title: String = "",
    val authorId: Long? = null,
    val authorName: String? = null,
    val authorPic: String? = null,
    val categoryId: Int? = null,
    val categoryName: String? = null,
    val description: String? = null,
    val words: Int? = null,
    val readingMinutes: Int? = null,
    val rating: Double? = null,
    val ratingCount: Int? = null,
    val publishedAt: String? = null,
    val type: String? = null,
    val seriesId: Long? = null,
    val seriesTitle: String? = null,
    val tags: String? = null,
)

@Serializable
private data class SavedStoryBackup(
    val story: StorySnapshot = StorySnapshot(),
    val savedAt: Long = 0L,
)

@Serializable
private data class SavedSeriesWorkBackup(
    val story: StorySnapshot = StorySnapshot(),
    val position: Int = 0,
)

@Serializable
private data class SavedSeriesBackup(
    val seriesId: Long = 0L,
    val title: String = "",
    val authorId: Long? = null,
    val authorName: String? = null,
    val description: String? = null,
    val state: String? = null,
    val workCount: Int = 0,
    val savedAt: Long = 0L,
    val works: List<SavedSeriesWorkBackup> = emptyList(),
)

@Serializable
private data class SavedAuthorBackup(
    val authorId: Long = 0L,
    val username: String = "",
    val userpic: String? = null,
    val bio: String? = null,
    val followers: Int? = null,
    val storiesCount: Int? = null,
    val seriesCount: Int? = null,
    val savedAt: Long = 0L,
)

@Serializable
private data class ProgressBackup(
    val storyId: Long = 0L,
    val page: Int = 1,
    val scrollFraction: Float = 0f,
    val percent: Float = 0f,
    val updatedAt: Long = 0L,
)

@Serializable
private data class HistoryBackup(
    val story: StorySnapshot = StorySnapshot(),
    val openedAt: Long = 0L,
)

@Serializable
private data class SettingsBackup(
    val theme: String = "system",
    val dynamicColor: Boolean = true,
    val fontFamily: String = "serif",
    val fontSizeSp: Int = 18,
    val lineHeightMultiplier: Float = 1.5f,
    val justifyText: Boolean = false,
    val readerWidth: String = "normal",
    val keepScreenOn: Boolean = true,
    val defaultHomeTab: String = "new",
    val defaultTopSort: String = "rating",
    val defaultTopPeriod: String = "week",
    val defaultSearchType: String = "stories",
    val ageConfirmed: Boolean = true,
    val imageLoading: String = "wifiOnly",
    val slowMode: Boolean = false,
    val ignoredUpdateVersion: String? = null,
)

private fun AppSettings.toBackup(): SettingsBackup = SettingsBackup(
    theme = theme,
    dynamicColor = dynamicColor,
    fontFamily = fontFamily,
    fontSizeSp = fontSizeSp,
    lineHeightMultiplier = lineHeightMultiplier,
    justifyText = justifyText,
    readerWidth = readerWidth,
    keepScreenOn = keepScreenOn,
    defaultHomeTab = defaultHomeTab,
    defaultTopSort = defaultTopSort,
    defaultTopPeriod = defaultTopPeriod,
    defaultSearchType = defaultSearchType,
    ageConfirmed = ageConfirmed,
    imageLoading = imageLoading,
    slowMode = slowMode,
    ignoredUpdateVersion = ignoredUpdateVersion,
)

private fun SettingsBackup.toSettings(): AppSettings = AppSettings(
    theme = theme,
    dynamicColor = dynamicColor,
    fontFamily = fontFamily,
    fontSizeSp = fontSizeSp.coerceIn(14, 26),
    lineHeightMultiplier = lineHeightMultiplier.coerceIn(1.2f, 2.0f),
    justifyText = justifyText,
    readerWidth = readerWidth,
    keepScreenOn = keepScreenOn,
    defaultHomeTab = defaultHomeTab,
    defaultTopSort = defaultTopSort,
    defaultTopPeriod = defaultTopPeriod,
    defaultSearchType = defaultSearchType,
    ageConfirmed = ageConfirmed,
    imageLoading = imageLoading,
    slowMode = slowMode,
    ignoredUpdateVersion = ignoredUpdateVersion,
)

private fun LocalSavedEntity.toSnapshot(): StorySnapshot = StorySnapshot(
    storyId = storyId,
    slug = slug,
    title = title,
    authorId = authorId,
    authorName = authorName,
    categoryId = categoryId,
    categoryName = categoryName,
    description = description,
    words = words,
    readingMinutes = readingMinutes,
    rating = rating,
    ratingCount = ratingCount,
    publishedAt = publishedAt,
    type = type,
    seriesId = seriesId,
    seriesTitle = seriesTitle,
    tags = tags,
)

private fun StorySnapshot.toEntity(savedAt: Long): LocalSavedEntity = LocalSavedEntity(
    storyId = storyId,
    slug = slug,
    title = title,
    authorId = authorId,
    authorName = authorName,
    categoryId = categoryId,
    categoryName = categoryName,
    description = description,
    words = words,
    readingMinutes = readingMinutes,
    rating = rating,
    ratingCount = ratingCount,
    publishedAt = publishedAt,
    type = type,
    seriesId = seriesId,
    seriesTitle = seriesTitle,
    tags = tags,
    savedAt = savedAt,
)

private fun LocalSeriesWorkEntity.toSnapshot(): StorySnapshot = StorySnapshot(
    storyId = storyId,
    slug = slug,
    title = title,
    authorId = authorId,
    authorName = authorName,
    authorPic = authorPic,
    categoryId = categoryId,
    categoryName = categoryName,
    description = description,
    words = words,
    readingMinutes = readingMinutes,
    rating = rating,
    ratingCount = ratingCount,
    publishedAt = publishedAt,
    type = type,
    tags = tags,
)

private fun StorySnapshot.toWorkEntity(seriesId: Long, position: Int): LocalSeriesWorkEntity =
    LocalSeriesWorkEntity(
        seriesId = seriesId,
        storyId = storyId,
        position = position,
        slug = slug,
        title = title,
        authorId = authorId,
        authorName = authorName,
        authorPic = authorPic,
        categoryId = categoryId,
        categoryName = categoryName,
        description = description,
        words = words,
        readingMinutes = readingMinutes,
        rating = rating,
        ratingCount = ratingCount,
        publishedAt = publishedAt,
        type = type,
        tags = tags,
    )

private fun HistoryEntity.toSnapshot(): StorySnapshot = StorySnapshot(
    storyId = storyId,
    slug = slug,
    title = title,
    authorId = authorId,
    authorName = authorName,
    authorPic = authorPic,
    categoryId = categoryId,
    categoryName = categoryName,
    description = description,
    words = words,
    readingMinutes = readingMinutes,
    rating = rating,
    ratingCount = ratingCount,
    publishedAt = publishedAt,
    type = type,
    seriesId = seriesId,
    seriesTitle = seriesTitle,
    tags = tags,
)

private fun StorySnapshot.toHistoryEntity(openedAt: Long): HistoryEntity = HistoryEntity(
    storyId = storyId,
    slug = slug,
    title = title,
    authorId = authorId,
    authorName = authorName,
    authorPic = authorPic,
    categoryId = categoryId,
    categoryName = categoryName,
    description = description,
    words = words,
    readingMinutes = readingMinutes,
    rating = rating,
    ratingCount = ratingCount,
    publishedAt = publishedAt,
    type = type,
    seriesId = seriesId,
    seriesTitle = seriesTitle,
    tags = tags,
    openedAt = openedAt,
)

private fun SavedSeriesBackup.toEntity(): LocalSeriesEntity = LocalSeriesEntity(
    seriesId = seriesId,
    title = title,
    authorId = authorId,
    authorName = authorName,
    description = description,
    state = state,
    workCount = workCount,
    savedAt = savedAt,
)

private fun LocalAuthorEntity.toBackup(): SavedAuthorBackup = SavedAuthorBackup(
    authorId = authorId,
    username = username,
    userpic = userpic,
    bio = bio,
    followers = followers,
    storiesCount = storiesCount,
    seriesCount = seriesCount,
    savedAt = savedAt,
)

private fun SavedAuthorBackup.toEntity(): LocalAuthorEntity = LocalAuthorEntity(
    authorId = authorId,
    username = username,
    userpic = userpic,
    bio = bio,
    followers = followers,
    storiesCount = storiesCount,
    seriesCount = seriesCount,
    savedAt = savedAt,
)

private fun ProgressEntity.toBackup(): ProgressBackup = ProgressBackup(
    storyId = storyId,
    page = page,
    scrollFraction = scrollFraction,
    percent = percent,
    updatedAt = updatedAt,
)

private fun ProgressBackup.toEntity(): ProgressEntity = ProgressEntity(
    storyId = storyId,
    page = page,
    scrollFraction = scrollFraction,
    percent = percent,
    updatedAt = updatedAt,
)
