package com.taiwanfreetv.app.data.repository

import com.taiwanfreetv.app.data.model.Channel
import com.taiwanfreetv.app.data.model.StreamType
import org.junit.Assert.assertEquals
import org.junit.Test

class UserPreferencesLogicTest {

    private val testChannels = listOf(
        Channel(id = "c1", name = "Channel 1", category = "新聞", type = StreamType.YOUTUBE_IFRAME, officialUrl = "https://example.com", number = 1),
        Channel(id = "c2", name = "Channel 2", category = "財經", type = StreamType.YOUTUBE_IFRAME, officialUrl = "https://example.com", number = 2),
        Channel(id = "c3", name = "Channel 3", category = "新聞", type = StreamType.HLS, streamUrl = "https://example.com/hls", officialUrl = "https://example.com", number = 3)
    )

    @Test
    fun testFavoriteFiltering() {
        val favoriteIds = setOf("c1", "c3")
        val favorites = testChannels.filter { favoriteIds.contains(it.id) }

        assertEquals(2, favorites.size)
        assertEquals("c1", favorites[0].id)
        assertEquals("c3", favorites[1].id)
    }

    @Test
    fun testCategoryAndSearchFiltering() {
        val categoryFilter = "新聞"
        val query = "Channel 3"

        val filteredByCategory = testChannels.filter { it.category == categoryFilter }
        assertEquals(2, filteredByCategory.size)

        val filteredByQuery = testChannels.filter { it.name.contains(query, ignoreCase = true) }
        assertEquals(1, filteredByQuery.size)
        assertEquals("c3", filteredByQuery[0].id)
    }

    @Test
    fun testCustomOrdering() {
        val customOrder = listOf("c3", "c1", "c2")
        val sortedChannels = testChannels.sortedBy { channel ->
            val index = customOrder.indexOf(channel.id)
            if (index == -1) Int.MAX_VALUE else index
        }

        assertEquals("c3", sortedChannels[0].id)
        assertEquals("c1", sortedChannels[1].id)
        assertEquals("c2", sortedChannels[2].id)
    }
}
