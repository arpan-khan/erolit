package dev.erolit.reader.data.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * The upstream API is inconsistent about scalar types: for example a tag's
 * `tag` field is usually a string but occasionally a bare number. Decode any
 * JSON primitive as text instead of failing the whole response.
 */
object LenientTextSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientText", PrimitiveKind.STRING).nullable

    override fun deserialize(decoder: Decoder): String? {
        val input = decoder as? JsonDecoder ?: return decoder.decodeString()
        return when (val element = input.decodeJsonElement()) {
            is JsonNull -> null
            is JsonPrimitive -> element.content
            else -> element.toString()
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value)
    }
}

@Serializable
data class CategoryDto(
    val id: Int,
    val language: Int? = null,
    val ldesc: String? = null,
    val name: String = "",
    @SerialName("pageUrl") val pageUrl: String? = null,
    val sdesc: String? = null,
    @SerialName("topUrl") val topUrl: String? = null,
    val type: String? = null,
    @SerialName("submission_count") val submissionCount: Int? = null,
)

@Serializable
data class TagDto(
    val id: Int? = null,
    @Serializable(with = LenientTextSerializer::class) val tag: String? = null,
)

@Serializable
data class AuthorRefDto(
    val userid: Long? = null,
    val username: String? = null,
    val userpic: String? = null,
)

@Serializable
data class CategoryRefDto(
    val id: Int? = null,
    @SerialName("pageUrl") val pageUrl: String? = null,
    val name: String? = null,
    val type: String? = null,
)

@Serializable
data class SeriesItemDto(
    val id: Long? = null,
    val title: String? = null,
    val url: String? = null,
    val type: String? = null,
)

@Serializable
data class SeriesRefMetaDto(
    val id: Long? = null,
    val title: String? = null,
    val url: String? = null,
)

/**
 * The upstream API sends `series` as an object when a story belongs to a series,
 * but as an empty array (`[]`) when it does not. Accept both shapes; an array
 * simply means "no series".
 */
object LenientSeriesRefSerializer : KSerializer<SeriesRefDto?> {
    private val delegate = SeriesRefDto.serializer()
    override val descriptor: SerialDescriptor = delegate.descriptor.nullable

    override fun deserialize(decoder: Decoder): SeriesRefDto? {
        val input = decoder as? JsonDecoder ?: return decoder.decodeNullableSerializableValue(delegate)
        return when (val element = input.decodeJsonElement()) {
            is JsonObject -> input.json.decodeFromJsonElement(delegate, element)
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: SeriesRefDto?) {
        encoder.encodeNullableSerializableValue(delegate, value)
    }
}

@Serializable
data class SeriesRefDto(
    val meta: SeriesRefMetaDto? = null,
    val items: List<SeriesItemDto>? = null,
)

@Serializable
data class SubmissionDto(
    val id: Long,
    val title: String = "",
    val url: String? = null,
    val type: String? = null,
    val author: AuthorRefDto? = null,
    val authorname: String? = null,
    val category: Int? = null,
    @SerialName("category_info") val categoryInfo: CategoryRefDto? = null,
    val tags: List<TagDto>? = null,
    val description: String? = null,
    @SerialName("date_approve") val dateApprove: String? = null,
    @SerialName("words_count") val wordsCount: Int? = null,
    @SerialName("reading_time") val readingTime: Int? = null,
    @SerialName("view_count") val viewCount: Int? = null,
    @SerialName("rate_all") val rateAll: Double? = null,
    @SerialName("rate_count") val rateCount: Int? = null,
    @SerialName("favorite_count") val favoriteCount: Int? = null,
    @SerialName("comment_count") val commentCount: Int? = null,
    @SerialName("is_hot") val isHot: Boolean? = null,
    @SerialName("is_new") val isNew: Boolean? = null,
    @SerialName("writers_pick") val writersPick: Boolean? = null,
    val status: String? = null,
    @Serializable(with = LenientSeriesRefSerializer::class)
    val series: SeriesRefDto? = null,
    val language: Int? = null,
)

@Serializable
data class BrowseMetaDto(
    @SerialName("submissions_count") val submissionsCount: Int? = null,
)

@Serializable
data class BrowseDto(
    val meta: BrowseMetaDto? = null,
    val submissions: List<SubmissionDto> = emptyList(),
)

/** Top lists return a Laravel paginator instead of the `{meta,submissions}` shape. */
@Serializable
data class PaginatedSubmissionsDto(
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
    val total: Int = 0,
    @SerialName("per_page") val perPage: Int = 20,
    val data: List<SubmissionDto> = emptyList(),
)

@Serializable
data class StoryMetaDto(
    @SerialName("pages_count") val pagesCount: Int? = null,
)

@Serializable
data class StoryDetailDto(
    val meta: StoryMetaDto? = null,
    val submission: SubmissionDto? = null,
    @SerialName("pageText") val pageText: String? = null,
)

@Serializable
data class SeriesCategoryDto(
    val id: Int? = null,
    val url: String? = null,
)

@Serializable
data class SeriesDto(
    val id: Long = 0,
    @SerialName("user_id") val userId: Long? = null,
    val url: String? = null,
    val title: String = "",
    val language: Int? = null,
    val state: String? = null,
    val description: String? = null,
    val introduction: String? = null,
    @SerialName("work_count") val workCount: Int? = null,
    @SerialName("view_count") val viewCount: Int? = null,
    @SerialName("author_name") val authorName: String? = null,
    val categories: List<SeriesCategoryDto>? = null,
    val works: List<SeriesItemDto>? = null,
    val user: UserDto? = null,
)

@Serializable
data class SeriesDetailDto(
    val success: Boolean? = null,
    val data: SeriesDto? = null,
)

@Serializable
data class SeriesCardDto(
    val id: Long = 0,
    @SerialName("user_id") val userId: Long? = null,
    val url: String? = null,
    val title: String = "",
    val language: Int? = null,
    val state: String? = null,
    val description: String? = null,
    @SerialName("work_count") val workCount: Int? = null,
    @SerialName("author_name") val authorName: String? = null,
    val categories: List<SeriesCategoryDto>? = null,
    val works: List<SeriesItemDto>? = null,
)

@Serializable
data class SeriesFeedDto(
    val data: List<SeriesCardDto> = emptyList(),
)

@Serializable
data class UserDto(
    val userid: Long = 0,
    val username: String = "",
    val userpic: String? = null,
    val bio: String? = null,
    val biography: String? = null,
    @SerialName("followers_count") val followersCount: Int? = null,
    @SerialName("stories_count") val storiesCount: Int? = null,
    @SerialName("submissions_count") val submissionsCount: Int? = null,
    @SerialName("series_count") val seriesCount: Int? = null,
    val lists: List<UserListDto>? = null,
)

@Serializable
data class UserDetailDto(
    val success: Boolean? = null,
    val user: UserDto? = null,
)

@Serializable
data class UserListDto(
    val id: Long = 0,
    val description: String? = null,
    @SerialName("stories_count") val storiesCount: Int = 0,
    @SerialName("submission_type") val submissionType: String? = null,
    val title: String = "",
    val urlname: String = "",
    @SerialName("user_id") val userId: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_private") val isPrivate: Int = 0,
    @SerialName("is_deletable") val isDeletable: Int = 0,
)

@Serializable
data class LinkDto(
    val url: String? = null,
    val label: String? = null,
    val active: Boolean? = null,
)

@Serializable
data class PageMetaDto(
    val total: Int = 0,
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
    val from: Int? = null,
    val to: Int? = null,
    @SerialName("per_page") val perPage: Int = 25,
)

@Serializable
data class ListWorksEnvelopeDto(
    val data: List<SubmissionDto> = emptyList(),
    val links: List<LinkDto>? = null,
    val meta: PageMetaDto? = null,
)

@Serializable
data class MyListWorksDto(
    val list: UserListDto? = null,
    val works: ListWorksEnvelopeDto? = null,
)

@Serializable
data class AuthorWorksDto(
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
    val total: Int = 0,
    @SerialName("per_page") val perPage: Int = 50,
    val data: List<SubmissionDto> = emptyList(),
)

@Serializable
data class SearchMetaDto(
    @SerialName("pageSize") val pageSize: Int = 50,
    val total: Int = 0,
)

@Serializable
data class SearchStoriesDto(
    val data: List<SubmissionDto> = emptyList(),
    val meta: SearchMetaDto? = null,
)

@Serializable
data class MemberDto(
    val userid: Long = 0,
    val username: String = "",
    val userpic: String? = null,
    val bio: String? = null,
    val biography: String? = null,
    @SerialName("stories_count") val storiesCount: Int? = null,
    @SerialName("followers_count") val followersCount: Int? = null,
)

@Serializable
data class SearchMembersDto(
    val data: List<MemberDto> = emptyList(),
    val meta: SearchMetaDto? = null,
)

@Serializable
data class CreateListResponseDto(
    val success: JsonElement? = null,
    val list: UserListDto? = null,
)

/**
 * Mutation responses differ between operations (`story` is sometimes the id,
 * sometimes the full submission, and `success` is sometimes a flag). Keep the
 * uninteresting parts untyped so a successful write is never reported as a
 * parse failure.
 */
@Serializable
data class MutationResponseDto(
    val success: JsonElement? = null,
    val story: JsonElement? = null,
    val list: UserListDto? = null,
)
