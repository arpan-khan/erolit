package dev.erolit.reader.data.repo

import dev.erolit.reader.domain.Category

/** In-memory category lookup shared by mappers so cards can show real names when known. */
class CategoryCache {
    @Volatile
    private var byId: Map<Int, Category> = emptyMap()

    @Volatile
    private var ordered: List<Category> = emptyList()

    fun update(items: List<Category>) {
        ordered = items
        byId = items.associateBy { it.id }
    }

    fun all(): List<Category> = ordered

    fun names(): Map<Int, String> = byId.mapValues { it.value.name }

    fun nameFor(id: Int?): String? = id?.let { byId[it]?.name }

    fun pageUrlFor(id: Int?): String? = id?.let { byId[it]?.pageUrl }

    fun clear() {
        ordered = emptyList()
        byId = emptyMap()
    }
}
