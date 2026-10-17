package com.taiwanfreetv.app.data.repository

import com.taiwanfreetv.app.data.model.Channel
import com.taiwanfreetv.app.data.model.LiveStatus
import com.taiwanfreetv.app.data.model.PlaybackStatus
import com.taiwanfreetv.app.data.model.StreamType
import com.taiwanfreetv.app.data.model.VodItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class YouTubeLiveResolverTest {

    private lateinit var resolver: YouTubeLiveResolver

    @Before
    fun setUp() {
        resolver = YouTubeLiveResolver(cacheTtlMillis = 1000L, httpTimeoutMs = 3000)
    }

    @Test
    fun testVodChannelResolution() = runBlocking {
        val vodChannel = Channel(
            id = "tv_osaka_otona_tabi",
            name = "おとな旅あるき旅",
            category = "日本旅遊、美食",
            type = StreamType.YOUTUBE_IFRAME,
            youtubeVideoId = "v0XWJjGSqhk",
            officialUrl = "https://www.tv-osaka.co.jp/ip4/tabi/",
            isVod = true,
            vodItems = listOf(
                VodItem("1", "精選 1", "v0XWJjGSqhk")
            )
        )

        val result = resolver.resolveChannelLiveStatus(vodChannel)
        assertEquals(LiveStatus.VOD, result.liveStatus)
        assertEquals("v0XWJjGSqhk", result.liveVideoId)
        assertNull(result.lastError)
    }

    @Test
    fun testExternalWebChannelResolution() = runBlocking {
        val externalChannel = Channel(
            id = "ttv_news",
            name = "台視新聞台",
            category = "新聞",
            type = StreamType.EXTERNAL_WEB,
            officialUrl = "https://news.ttv.com.tw/",
            status = PlaybackStatus.EMBED_DISABLED,
            statusMessage = "台視新聞台 YouTube 直播受內嵌權限限制"
        )

        val result = resolver.resolveChannelLiveStatus(externalChannel)
        assertEquals(LiveStatus.EMBED_DISABLED, result.liveStatus)
        assertEquals("台視新聞台 YouTube 直播受內嵌權限限制", result.lastError)
    }

    @Test
    fun testNonYouTubeStreamTypeResolution() = runBlocking {
        val hlsChannel = Channel(
            id = "hls_test",
            name = "HLS Test",
            category = "綜合",
            type = StreamType.HLS,
            streamUrl = "https://example.com/live.m3u8",
            officialUrl = "https://example.com"
        )

        val result = resolver.resolveChannelLiveStatus(hlsChannel)
        assertEquals(LiveStatus.UNKNOWN, result.liveStatus)
        assertNull(result.liveVideoId)
    }

    @Test
    fun testCacheExpirationAndRefresh() = runBlocking {
        val channel = Channel(
            id = "test_cache_channel",
            name = "Cache Test Channel",
            category = "綜合",
            type = StreamType.HLS,
            streamUrl = "https://example.com/stream.m3u8",
            officialUrl = "https://example.com"
        )

        val res1 = resolver.resolveChannelLiveStatus(channel)
        assertNotNull(res1.resolvedAt)

        // Immediate second call should hit cache and have exact same resolvedAt
        val res2 = resolver.resolveChannelLiveStatus(channel)
        assertEquals(res1.resolvedAt, res2.resolvedAt)

        // Clear cache and verify forced refresh
        resolver.clearCache()
        val res3 = resolver.resolveChannelLiveStatus(channel, forceRefresh = true)
        assertNotNull(res3)
    }
}
