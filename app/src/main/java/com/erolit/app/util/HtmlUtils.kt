package com.erolit.app.util

/**
 * Utility functions for HTML processing and text formatting.
 */
object HtmlUtils {

    /** Strip HTML tags from a string, returning plain text. */
    fun stripHtml(html: String): String {
        return html
            .replace(Regex("<br\\s*/?>"), "\n")
            .replace(Regex("<p[^>]*>"), "")
            .replace(Regex("</p>"), "\n\n")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("&#039;"), "'")
            .replace(Regex("&nbsp;"), " ")
            .trim()
    }

    /** Truncate text to a given length with ellipsis. */
    fun truncate(text: String, maxLength: Int = 200): String {
        return if (text.length <= maxLength) text else "${text.take(maxLength)}…"
    }
}
