package dev.erolit.reader.ui.reader

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dev.erolit.reader.R

data class ReaderFontOption(val id: String, val label: String)

/**
 * Font choices available in the reader. Every option is backed by a real font
 * bundled with the app so the preview and the reader look identical on every
 * device, regardless of which system fonts are installed.
 */
val readerFontOptions = listOf(
    ReaderFontOption("serif", "Serif"),
    ReaderFontOption("sans", "Sans"),
    ReaderFontOption("mono", "Mono"),
    ReaderFontOption("cursive", "Cursive"),
    ReaderFontOption("condensed", "Condensed"),
    ReaderFontOption("light", "Light"),
)

private val SerifFamily = FontFamily(Font(R.font.reader_serif))
private val SansFamily = FontFamily(Font(R.font.reader_sans))
private val MonoFamily = FontFamily(Font(R.font.reader_mono))
private val CursiveFamily = FontFamily(Font(R.font.reader_cursive))
private val CondensedFamily = FontFamily(Font(R.font.reader_condensed))
private val LightFamily = FontFamily(Font(R.font.reader_light))

fun readerFontFamily(id: String): FontFamily = when (id) {
    "sans" -> SansFamily
    "mono" -> MonoFamily
    "cursive" -> CursiveFamily
    "condensed" -> CondensedFamily
    "light" -> LightFamily
    else -> SerifFamily
}
