package dev.erolit.reader.ui.reader

import androidx.compose.ui.text.font.FontFamily

data class ReaderFontOption(val id: String, val label: String)

/**
 * Font choices available in the reader. The first three map to Compose's
 * generic families; condensed and light use Android's system font families so
 * the difference is visible on every device without bundling font files.
 */
val readerFontOptions = listOf(
    ReaderFontOption("serif", "Serif"),
    ReaderFontOption("sans", "Sans"),
    ReaderFontOption("mono", "Mono"),
    ReaderFontOption("cursive", "Cursive"),
    ReaderFontOption("condensed", "Condensed"),
    ReaderFontOption("light", "Light"),
)

private val CondensedFamily = FontFamily(
    android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL),
)

private val LightFamily = FontFamily(
    android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL),
)

fun readerFontFamily(id: String): FontFamily = when (id) {
    "sans" -> FontFamily.SansSerif
    "mono" -> FontFamily.Monospace
    "cursive" -> FontFamily.Cursive
    "condensed" -> CondensedFamily
    "light" -> LightFamily
    else -> FontFamily.Serif
}
