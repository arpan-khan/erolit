package com.erolit.app.util

object Constants {
    const val BASE_URL = "https://www.literotica.com"
    const val TAGS_URL = "https://tags.literotica.com"
    const val SEARCH_URL = "https://search.literotica.com"
    const val UPLOADS_URL = "https://uploads.literotica.com"

    // URL paths
    const val PATH_STORIES = "/stories"
    const val PATH_NEW = "/new/stories"
    const val PATH_TOP_RATED = "/top/top-rated-erotic-stories/"
    const val PATH_MOST_READ = "/top/most-read-erotic-stories/"
    const val PATH_SERIES = "/series/"
    const val PATH_CATEGORY = "/c/"
    const val PATH_STORY = "/s/"
    const val PATH_AUTHOR = "/authors/"

    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/127.0.0.0 Mobile Safari/537.36"

    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L

    // Reading prefs keys
    const val PREF_FONT_SIZE = "pref_font_size"
    const val PREF_READER_THEME = "pref_reader_theme"
    const val PREF_LINE_SPACING = "pref_line_spacing"

    const val DEFAULT_FONT_SIZE = 16f
    const val MIN_FONT_SIZE = 12f
    const val MAX_FONT_SIZE = 28f

    // Pagination
    const val PAGE_SIZE = 20
}
