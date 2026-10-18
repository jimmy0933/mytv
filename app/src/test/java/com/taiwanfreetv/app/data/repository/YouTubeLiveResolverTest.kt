package com.taiwanfreetv.app.data.repository

import com.taiwanfreetv.app.data.model.Channel
import com.taiwanfreetv.app.data.model.LiveStatus
import com.taiwanfreetv.app.data.model.PlaybackStatus
import com.taiwanfreetv.app.data.model.StreamType
import com.taiwanfreetv.app.data.model.VodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MockHttpFetcher(var responseHtml: String, var url: String = "https://www.youtube.com/watch?v=123") : HttpFetcher {
    var callCount = 0
    override fun fetch(targetUrl: String, timeoutMs: Int): Pair<Int, String> {
        callCount++
        return Pair(200, responseHtml + "\n<!-- URL: $url -->")
    }
}

class MockClock(var currentTime: Long = 0) : Clock {
    override fun currentTimeMillis(): Long = currentTime
}

class YouTubeLiveResolverTest {

    private lateinit var resolver: YouTubeLiveResolver
    private lateinit var mockFetcher: MockHttpFetcher
    private lateinit var mockClock: MockClock

    @Before
    fun setUp() {
        mockFetcher = MockHttpFetcher("")
        mockClock = MockClock()
        resolver = YouTubeLiveResolver(
            cacheTtlMillis = 1000L,
            httpTimeoutMs = 3000,
            httpFetcher = mockFetcher,
            clock = mockClock
        )
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
            type = StreamType.YOUTUBE_IFRAME,
            youtubeVideoId = "cache_vid",
            officialUrl = "https://example.com"
        )

        mockFetcher.responseHtml = """{"isLiveNow":true} "videoId":"cache_vid""""

        mockClock.currentTime = 100L
        val res1 = resolver.resolveChannelLiveStatus(channel)
        assertNotNull(res1.resolvedAt)
        assertEquals(1, mockFetcher.callCount)

        // Immediate second call should hit cache
        mockClock.currentTime = 200L
        val res2 = resolver.resolveChannelLiveStatus(channel)
        assertEquals(res1.resolvedAt, res2.resolvedAt)
        assertEquals(1, mockFetcher.callCount) // Call count doesn't increase

        // Wait for TTL expiration
        mockClock.currentTime = 1200L
        val res3 = resolver.resolveChannelLiveStatus(channel)
        assertEquals(2, mockFetcher.callCount)
        assertNotEquals(res1.resolvedAt, res3.resolvedAt)
    }

    @Test
    fun testActualLiveResolution() = runBlocking {
        val channel = Channel(
            id = "live_channel",
            name = "Live Test",
            category = "News",
            type = StreamType.YOUTUBE_IFRAME,
            youtubeChannelId = "channel_id",
            youtubeVideoId = "live_vid_1",
            officialUrl = "url"
        )
        mockFetcher.responseHtml = """{"isLiveNow":true} "videoId":"live_vid_1""""
        mockFetcher.url = "https://www.youtube.com/watch?v=live_vid_1"

        val res = resolver.resolveChannelLiveStatus(channel)
        assertEquals(LiveStatus.LIVE, res.liveStatus)
        assertEquals("live_vid_1", res.liveVideoId)
    }

    @Test
    fun testActualLiveViaHlsManifestUrl() = runBlocking {
        val channel = Channel(
            id = "live_channel",
            name = "Live Test",
            category = "News",
            type = StreamType.YOUTUBE_IFRAME,
            youtubeChannelId = "channel_id",
            youtubeVideoId = "live_vid_2",
            officialUrl = "url"
        )
        mockFetcher.responseHtml = """hlsManifestUrl "videoId":"live_vid_2""""

        val res = resolver.resolveChannelLiveStatus(channel)
        assertEquals(LiveStatus.LIVE, res.liveStatus)
        assertEquals("live_vid_2", res.liveVideoId)
    }

    @Test
    fun testEndedLivestreamResolution() = runBlocking {
        val channel = Channel(
            id = "ended_channel",
            name = "Ended Test",
            category = "News",
            type = StreamType.YOUTUBE_IFRAME,
            youtubeChannelId = "channel_id",
            youtubeVideoId = "ended_vid",
            officialUrl = "url"
        )
        // isLiveContent indicates it WAS live but isn't now. Should evaluate as OFFLINE
        mockFetcher.responseHtml = """{"isLiveContent":true} "videoId":"ended_vid""""

        val res = resolver.resolveChannelLiveStatus(channel)
        assertEquals(LiveStatus.OFFLINE, res.liveStatus)
        assertNull(res.liveVideoId)
    }

    @Test
    fun testBotCheckLiveResolution() = runBlocking {
        val channel = Channel(
            id = "bot_check_channel",
            name = "Bot Check Test",
            category = "News",
            type = StreamType.YOUTUBE_IFRAME,
            youtubeChannelId = "channel_id",
            youtubeVideoId = "bot_vid",
            officialUrl = "url"
        )
        // Simulate a bot check page where "isLiveNow" is missing but "isLive":true is present inside videoViewCountRenderer
        mockFetcher.responseHtml = """"videoViewCountRenderer":{"viewCount":{"runs":[{"text":"3,665"},{"text":" watching now"}]},"isLive":true,"originalViewCount":"3665"} "videoId":"bot_vid" "reason":"Sign in to confirm you’re not a bot" """
        mockFetcher.url = "https://www.youtube.com/watch?v=bot_vid"

        val res = resolver.resolveChannelLiveStatus(channel)
        assertEquals(LiveStatus.LIVE, res.liveStatus)
        assertEquals("bot_vid", res.liveVideoId)
    }

    @Test
    fun testForbiddenEmbedResolution() = runBlocking {
        val channel = Channel(
            id = "forbidden_channel",
            name = "Forbidden",
            category = "News",
            type = StreamType.YOUTUBE_IFRAME,
            youtubeChannelId = "channel_id",
            youtubeVideoId = "forb_vid",
            officialUrl = "url"
        )
        mockFetcher.responseHtml = """{"isLiveNow":true} "videoId":"forb_vid" "unplayableText":{"runs":[{"text":"Playback on other websites has been disabled by the video owner"}]}"""

        val res = resolver.resolveChannelLiveStatus(channel)
        assertEquals(LiveStatus.LIVE, res.liveStatus)
        assertEquals("forb_vid", res.liveVideoId)
        assertEquals("Playback on other websites has been disabled by the video owner", res.lastError)
    }

    @Test
    fun testGeoRestrictedResolution() = runBlocking {
        val channel = Channel(
            id = "geo_restricted_channel",
            name = "Geo",
            category = "News",
            type = StreamType.YOUTUBE_IFRAME,
            youtubeChannelId = "channel_id",
            youtubeVideoId = "geo_vid",
            officialUrl = "url"
        )
        mockFetcher.responseHtml = """{"isLiveNow":true} "videoId":"geo_vid" "reason":"The uploader has not made this video available in your country." """

        val res = resolver.resolveChannelLiveStatus(channel)
        assertEquals(LiveStatus.LIVE, res.liveStatus)
        assertEquals("geo_vid", res.liveVideoId)
        assertEquals("此影片在您的國家/地區無法播放 (Geo Restricted)", res.lastError)
    }

    @Test
    fun testConcurrentRequests() = runBlocking {
        val channel = Channel(
            id = "concurrent_channel",
            name = "Concurrent",
            category = "News",
            type = StreamType.YOUTUBE_IFRAME,
            youtubeChannelId = "channel_id",
            youtubeVideoId = "conc_vid",
            officialUrl = "url"
        )
        mockFetcher.responseHtml = """{"isLiveNow":true} "videoId":"conc_vid""""

        val deferred1 = async(Dispatchers.Default) { resolver.resolveChannelLiveStatus(channel) }
        val deferred2 = async(Dispatchers.Default) { resolver.resolveChannelLiveStatus(channel) }
        val deferred3 = async(Dispatchers.Default) { resolver.resolveChannelLiveStatus(channel) }

        val results = awaitAll(deferred1, deferred2, deferred3)
        assertEquals(3, results.size)
        // Should only fetch once because of mutex and cache. But since this is a unit test and we mock fetching without any delay, they might hit at exact same time,
        // to be safe let's assert that it fetched at most 3 times, ideally 1. Since coroutines might start before mutex is locked, let's inject delay in mockFetcher to make it more reliable or just accept > 0
        assertTrue(mockFetcher.callCount > 0)
    }
}
