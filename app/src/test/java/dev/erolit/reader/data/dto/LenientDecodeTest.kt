package dev.erolit.reader.data.dto

import dev.erolit.reader.core.net.AppJson
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Test

class LenientDecodeTest {
    @Test
    fun numericTagValueDecodesAsText() {
        val raw = """
            {"meta":{"submissions_count":1},"submissions":[
              {"id":4589422,"title":"A title","tags":[{"id":213,"tag":69},{"id":903,"tag":"18+"}]}
            ]}
        """.trimIndent()

        val browse = AppJson.decodeFromString<BrowseDto>(raw)

        assertEquals("69", browse.submissions.single().tags?.get(0)?.tag)
        assertEquals("18+", browse.submissions.single().tags?.get(1)?.tag)
    }

    @Test
    fun mutationResponseWithStoryObjectDecodes() {
        val raw = """{"success":true,"story":{"id":123,"title":"A story"},"list":{"id":5,"urlname":"x","title":"L","stories_count":1}}"""

        val dto = AppJson.decodeFromString<MutationResponseDto>(raw)

        assertEquals(5L, dto.list?.id)
    }
}
