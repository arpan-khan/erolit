package dev.erolit.reader.data

import dev.erolit.reader.data.dto.CategoryDto
import dev.erolit.reader.data.dto.MemberDto
import dev.erolit.reader.data.dto.SeriesCardDto
import dev.erolit.reader.data.dto.SeriesDto
import dev.erolit.reader.data.dto.SubmissionDto
import dev.erolit.reader.data.dto.UserDto
import dev.erolit.reader.data.dto.UserListDto
import dev.erolit.reader.domain.AuthorProfile
import dev.erolit.reader.domain.Category
import dev.erolit.reader.domain.MemberSummary
import dev.erolit.reader.domain.SeriesSummary
import dev.erolit.reader.domain.StorySummary
import dev.erolit.reader.domain.UserList

fun CategoryDto.toDomain(): Category = Category(
    id = id,
    name = name,
    shortName = sdesc,
    description = ldesc,
    pageUrl = pageUrl,
    type = type,
    submissionCount = submissionCount,
)

fun SubmissionDto.toSummary(categoryName: String?): StorySummary = StorySummary(
    id = id,
    slug = url,
    title = title,
    authorId = author?.userid,
    authorName = author?.username ?: authorname,
    authorPic = author?.userpic,
    categoryId = category,
    categoryName = categoryName,
    description = description,
    words = wordsCount,
    readingMinutes = readingTime,
    rating = rateAll,
    ratingCount = rateCount,
    publishedAt = dateApprove,
    type = type,
    isHot = isHot == true,
    isNew = isNew == true,
    writersPick = writersPick == true,
    seriesId = series?.meta?.id,
    seriesTitle = series?.meta?.title,
    tags = tags.orEmpty().mapNotNull { it.tag?.trim()?.takeIf(String::isNotEmpty) }.distinct(),
)

fun UserDto.toProfile(): AuthorProfile = AuthorProfile(
    id = userid,
    username = username,
    userpic = userpic,
    bio = bio?.takeIf { it.isNotBlank() } ?: biography,
    followers = followersCount,
    storiesCount = storiesCount ?: submissionsCount,
    seriesCount = seriesCount,
)

fun UserListDto.toDomain(): UserList = UserList(
    id = id,
    urlname = urlname,
    title = title,
    description = description,
    isPrivate = isPrivate == 1,
    isDeletable = isDeletable == 1,
    storiesCount = storiesCount,
    submissionType = submissionType,
    updatedAt = updatedAt,
)

fun MemberDto.toDomain(): MemberSummary = MemberSummary(
    id = userid,
    username = username,
    userpic = userpic,
    bio = bio?.takeIf { it.isNotBlank() } ?: biography,
)

fun SeriesDto.toSummary(): SeriesSummary = SeriesSummary(
    id = id,
    url = url,
    title = title,
    authorName = user?.username ?: authorName,
    authorId = user?.userid ?: userId,
    description = description ?: introduction,
    state = state,
    workCount = workCount ?: works?.size,
    workIds = works.orEmpty().mapNotNull { it.id },
)

fun SeriesCardDto.toSummary(): SeriesSummary = SeriesSummary(
    id = id,
    url = url,
    title = title,
    authorName = authorName,
    authorId = userId,
    description = description,
    state = state,
    workCount = workCount ?: works?.size,
    workIds = works.orEmpty().mapNotNull { it.id },
)

fun categoryLabel(id: Int?, slug: String?, names: Map<Int, String>): String? {
    names[id]?.let { return it }
    if (slug.isNullOrBlank()) return null
    return slug
        .split('-', '_')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }
}

fun formatCompact(value: Int?): String {
    val v = value ?: return "—"
    return when {
        v >= 1_000_000 -> String.format("%.1fM", v / 1_000_000.0)
        v >= 10_000 -> String.format("%.0fk", v / 1_000.0)
        v >= 1_000 -> String.format("%.1fk", v / 1_000.0)
        else -> v.toString()
    }
}

fun formatWords(words: Int?): String {
    val v = words ?: return ""
    return when {
        v >= 1_000 -> String.format("%.1fk words", v / 1_000.0)
        else -> "$v words"
    }
}
