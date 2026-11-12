package dev.erolit.reader.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
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
    val ageConfirmed: Boolean = false,
    val imageLoading: String = "wifiOnly",
    val slowMode: Boolean = false,
    /** Release tag the user chose not to be reminded about again. */
    val ignoredUpdateVersion: String? = null,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val fontFamily = stringPreferencesKey("font_family")
        val fontSize = intPreferencesKey("font_size_sp")
        val lineHeight = floatPreferencesKey("line_height_multiplier")
        val justify = booleanPreferencesKey("justify_text")
        val readerWidth = stringPreferencesKey("reader_width")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val defaultHomeTab = stringPreferencesKey("default_home_tab")
        val defaultTopSort = stringPreferencesKey("default_top_sort")
        val defaultTopPeriod = stringPreferencesKey("default_top_period")
        val defaultSearchType = stringPreferencesKey("default_search_type")
        val ageConfirmed = booleanPreferencesKey("age_confirmed")
        val imageLoading = stringPreferencesKey("image_loading")
        val slowMode = booleanPreferencesKey("slow_mode")
        val ignoredUpdateVersion = stringPreferencesKey("ignored_update_version")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { cause ->
            if (cause is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw cause
        }
        .map { prefs ->
            val defaults = AppSettings()
            AppSettings(
                theme = prefs[Keys.theme] ?: defaults.theme,
                dynamicColor = prefs[Keys.dynamicColor] ?: defaults.dynamicColor,
                fontFamily = prefs[Keys.fontFamily] ?: defaults.fontFamily,
                fontSizeSp = (prefs[Keys.fontSize] ?: defaults.fontSizeSp).coerceIn(14, 26),
                lineHeightMultiplier = (prefs[Keys.lineHeight] ?: defaults.lineHeightMultiplier).coerceIn(1.2f, 2.0f),
                justifyText = prefs[Keys.justify] ?: defaults.justifyText,
                readerWidth = prefs[Keys.readerWidth] ?: defaults.readerWidth,
                keepScreenOn = prefs[Keys.keepScreenOn] ?: defaults.keepScreenOn,
                defaultHomeTab = prefs[Keys.defaultHomeTab] ?: defaults.defaultHomeTab,
                defaultTopSort = prefs[Keys.defaultTopSort] ?: defaults.defaultTopSort,
                defaultTopPeriod = prefs[Keys.defaultTopPeriod] ?: defaults.defaultTopPeriod,
                defaultSearchType = prefs[Keys.defaultSearchType] ?: defaults.defaultSearchType,
                ageConfirmed = prefs[Keys.ageConfirmed] ?: defaults.ageConfirmed,
                imageLoading = prefs[Keys.imageLoading] ?: defaults.imageLoading,
                slowMode = prefs[Keys.slowMode] ?: defaults.slowMode,
                ignoredUpdateVersion = prefs[Keys.ignoredUpdateVersion],
            )
        }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setTheme(value: String) = edit { it[Keys.theme] = value }
    suspend fun setDynamicColor(value: Boolean) = edit { it[Keys.dynamicColor] = value }
    suspend fun setFontFamily(value: String) = edit { it[Keys.fontFamily] = value }
    suspend fun setFontSize(value: Int) = edit { it[Keys.fontSize] = value.coerceIn(14, 26) }
    suspend fun setLineHeight(value: Float) = edit { it[Keys.lineHeight] = value.coerceIn(1.2f, 2.0f) }
    suspend fun setJustify(value: Boolean) = edit { it[Keys.justify] = value }
    suspend fun setReaderWidth(value: String) = edit { it[Keys.readerWidth] = value }
    suspend fun setKeepScreenOn(value: Boolean) = edit { it[Keys.keepScreenOn] = value }
    suspend fun setDefaultHomeTab(value: String) = edit { it[Keys.defaultHomeTab] = value }
    suspend fun setDefaultTopSort(value: String) = edit { it[Keys.defaultTopSort] = value }
    suspend fun setDefaultTopPeriod(value: String) = edit { it[Keys.defaultTopPeriod] = value }
    suspend fun setDefaultSearchType(value: String) = edit { it[Keys.defaultSearchType] = value }
    suspend fun setAgeConfirmed(value: Boolean) = edit { it[Keys.ageConfirmed] = value }
    suspend fun setImageLoading(value: String) = edit { it[Keys.imageLoading] = value }
    suspend fun setSlowMode(value: Boolean) = edit { it[Keys.slowMode] = value }
    suspend fun setIgnoredUpdateVersion(value: String?) = edit { prefs ->
        if (value.isNullOrBlank()) prefs.remove(Keys.ignoredUpdateVersion)
        else prefs[Keys.ignoredUpdateVersion] = value
    }

    /** Applies a backup in one write, so the UI never sees a half-restored state. */
    suspend fun restore(settings: AppSettings) {
        edit { prefs ->
            prefs[Keys.theme] = settings.theme
            prefs[Keys.dynamicColor] = settings.dynamicColor
            prefs[Keys.fontFamily] = settings.fontFamily
            prefs[Keys.fontSize] = settings.fontSizeSp.coerceIn(14, 26)
            prefs[Keys.lineHeight] = settings.lineHeightMultiplier.coerceIn(1.2f, 2.0f)
            prefs[Keys.justify] = settings.justifyText
            prefs[Keys.readerWidth] = settings.readerWidth
            prefs[Keys.keepScreenOn] = settings.keepScreenOn
            prefs[Keys.defaultHomeTab] = settings.defaultHomeTab
            prefs[Keys.defaultTopSort] = settings.defaultTopSort
            prefs[Keys.defaultTopPeriod] = settings.defaultTopPeriod
            prefs[Keys.defaultSearchType] = settings.defaultSearchType
            prefs[Keys.ageConfirmed] = settings.ageConfirmed
            prefs[Keys.imageLoading] = settings.imageLoading
            prefs[Keys.slowMode] = settings.slowMode
            val ignored = settings.ignoredUpdateVersion
            if (ignored.isNullOrBlank()) prefs.remove(Keys.ignoredUpdateVersion)
            else prefs[Keys.ignoredUpdateVersion] = ignored
        }
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit { prefs -> block(prefs) }
    }
}
