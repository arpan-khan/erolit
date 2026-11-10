package dev.erolit.reader.domain

data class StorySummary(
    val id: Long,
    val slug: String?,
    val title: String,
    val authorId: Long?,
    val authorName: String?,
    val authorPic: String?,
    val categoryId: Int?,
    val categoryName: String?,
    val description: String?,
    val words: Int?,
    val readingMinutes: Int?,
    val rating: Double?,
    val ratingCount: Int?,
    val publishedAt: String?,
    val type: String?,
    val isHot: Boolean,
    val isNew: Boolean,
    val writersPick: Boolean,
    val seriesId: Long?,
    val seriesTitle: String?,
    val tags: List<String> = emptyList(),
)

data class PagedStories(
    val items: List<StorySummary>,
    val page: Int,
    val total: Int,
    val pageSize: Int = 20,
)

data class StoryDetail(
    val summary: StorySummary,
    val pagesCount: Int,
    val page: Int,
    val text: String,
    val fetchedAt: Long,
)

data class CachedStory(
    val summary: StorySummary,
    val pagesCount: Int,
    val page: Int,
    val text: String?,
    val fetchedAt: Long?,
)

data class SeriesSummary(
    val id: Long,
    val url: String?,
    val title: String,
    val authorName: String?,
    val authorId: Long?,
    val description: String?,
    val state: String?,
    val workCount: Int?,
)

data class SeriesDetail(
    val id: Long,
    val url: String?,
    val title: String,
    val authorName: String?,
    val authorId: Long?,
    val description: String?,
    val introduction: String?,
    val workCount: Int,
    val state: String?,
    val works: List<StorySummary>,
)

data class AuthorProfile(
    val id: Long,
    val username: String,
    val userpic: String?,
    val bio: String?,
    val followers: Int?,
    val storiesCount: Int?,
    val seriesCount: Int?,
)

data class MemberSummary(
    val id: Long,
    val username: String,
    val userpic: String?,
    val bio: String?,
)

data class SearchResult(
    val stories: List<StorySummary> = emptyList(),
    val members: List<MemberSummary> = emptyList(),
    val total: Int,
    val page: Int,
    val pageSize: Int = 50,
)

data class UserList(
    val id: Long,
    val urlname: String,
    val title: String,
    val description: String?,
    val isPrivate: Boolean,
    val isDeletable: Boolean,
    val storiesCount: Int,
    val submissionType: String?,
    val updatedAt: String?,
)

data class ListWorksPage(
    val list: UserList,
    val items: List<StorySummary>,
    val page: Int,
    val lastPage: Int,
    val total: Int,
)

data class ProgressInfo(
    val storyId: Long,
    val page: Int,
    val scrollFraction: Float,
    val percent: Float,
    val updatedAt: Long,
)

data class RecentReading(
    val storyId: Long,
    val page: Int,
    val scrollFraction: Float,
    val percent: Float,
    val updatedAt: Long,
    val title: String,
    val authorName: String?,
    val slug: String?,
    val type: String?,
)

data class Category(
    val id: Int,
    val name: String,
    val shortName: String?,
    val description: String?,
    val pageUrl: String?,
    val type: String?,
    val submissionCount: Int?,
)

enum class HomeTab {
    New, Top, Series
}
