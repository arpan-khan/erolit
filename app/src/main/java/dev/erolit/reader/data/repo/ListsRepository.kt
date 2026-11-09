package dev.erolit.reader.data.repo

import dev.erolit.reader.core.net.ApiClient
import dev.erolit.reader.core.net.ApiError
import dev.erolit.reader.core.net.ApiRoutes
import dev.erolit.reader.core.net.AppJson
import dev.erolit.reader.core.net.Params
import dev.erolit.reader.data.categoryLabel
import dev.erolit.reader.data.dto.CreateListResponseDto
import dev.erolit.reader.data.dto.ListWorksEnvelopeDto
import dev.erolit.reader.data.dto.MutationResponseDto
import dev.erolit.reader.data.dto.MyListWorksDto
import dev.erolit.reader.data.dto.UserListDto
import dev.erolit.reader.data.toDomain
import dev.erolit.reader.data.toSummary
import dev.erolit.reader.domain.ListWorksPage
import dev.erolit.reader.domain.UserList
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ListsRepository(
    private val api: ApiClient,
    private val routes: ApiRoutes,
    private val categories: CategoryCache,
) {
    suspend fun myLists(): List<UserList> =
        api.getJson(routes.myLists(), authenticated = true, retryTransient404 = false) { raw ->
            AppJson.decodeFromString<List<UserListDto>>(raw)
        }.map { it.toDomain() }

    suspend fun listWorks(urlname: String, page: Int): ListWorksPage {
        val dto = api.getJson(
            routes.myList(urlname, Params.listWorks(page)),
            authenticated = true,
            retryTransient404 = false,
        ) { raw -> AppJson.decodeFromString<MyListWorksDto>(raw) }
        val listDto = dto.list ?: throw ApiError.NotFound
        val envelope: ListWorksEnvelopeDto = dto.works ?: ListWorksEnvelopeDto()
        val names = categories.names()
        return ListWorksPage(
            list = listDto.toDomain(),
            items = envelope.data.map { submission ->
                submission.toSummary(categoryLabel(submission.category, submission.categoryInfo?.pageUrl, names))
            },
            page = envelope.meta?.currentPage ?: page,
            lastPage = envelope.meta?.lastPage ?: 1,
            total = envelope.meta?.total ?: 0,
        )
    }

    suspend fun createList(userId: Long, title: String, isPrivate: Boolean): UserList {
        val body = buildJsonObject {
            put("title", title.trim())
            put("isPrivate", if (isPrivate) 1 else 0)
        }.toString()
        val dto = api.postJson(
            url = routes.createList(userId),
            jsonBody = body,
            authenticated = true,
        ) { raw -> AppJson.decodeFromString<CreateListResponseDto>(raw) }
        return dto.list?.toDomain() ?: throw ApiError.Parse(IllegalStateException("missing list"))
    }

    suspend fun deleteList(listId: Long) {
        api.delete(
            url = routes.deleteList(listId),
            authenticated = true,
        ) { raw -> decodeMutation(raw) }
    }

    suspend fun addStory(storyId: Long, listId: Long): UserList? {
        val dto = api.put(
            url = routes.addStoryToList(storyId, listId),
            authenticated = true,
        ) { raw -> AppJson.decodeFromString<MutationResponseDto>(raw) }
        return dto.list?.toDomain()
    }

    suspend fun removeStory(storyId: Long, listId: Long) {
        api.delete(
            url = routes.addStoryToList(storyId, listId),
            authenticated = true,
        ) { raw -> decodeMutation(raw) }
    }

    suspend fun listsContaining(storyId: Long): Set<Long> =
        api.getJson(
            routes.storiesLists(storyId),
            authenticated = true,
            retryTransient404 = false,
        ) { raw -> AppJson.decodeFromString<List<Long>>(raw) }.toSet()

    private fun decodeMutation(raw: String): MutationResponseDto {
        if (raw.isBlank()) return MutationResponseDto()
        return runCatching { AppJson.decodeFromString<MutationResponseDto>(raw) }
            .getOrDefault(MutationResponseDto())
    }
}
