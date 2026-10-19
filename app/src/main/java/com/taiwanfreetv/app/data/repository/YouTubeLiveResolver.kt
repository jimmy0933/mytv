package com.taiwanfreetv.app.data.repository

import com.taiwanfreetv.app.data.model.Channel
import com.taiwanfreetv.app.data.model.LiveStatus
import com.taiwanfreetv.app.data.model.PlaybackStatus
import com.taiwanfreetv.app.data.model.StreamType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

data class LiveResolutionResult(
    val liveStatus: LiveStatus,
    val liveVideoId: String?,
    val resolvedAt: String,
    val resolvedTimestamp: Long,
    val lastError: String?,
    val liveItems: List<com.taiwanfreetv.app.data.model.LiveItem>? = null
)

interface HttpFetcher {
    fun fetch(url: String, timeoutMs: Int): Pair<Int, String>
}

class DefaultHttpFetcher : HttpFetcher {
    override fun fetch(targetUrl: String, timeoutMs: Int): Pair<Int, String> {
        val url = URL(targetUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = timeoutMs
        conn.readTimeout = timeoutMs
        conn.requestMethod = "GET"
        conn.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        )
        conn.setRequestProperty("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7")

        val responseCode = conn.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            return Pair(responseCode, "")
        }
        val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
        val html = reader.use { it.readText() }
        val finalUrl = conn.url.toString()
        return Pair(responseCode, html + "\n<!-- URL: $finalUrl -->")
    }
}

interface Clock {
    fun currentTimeMillis(): Long
}

class DefaultClock : Clock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

class YouTubeLiveResolver(
    private val cacheTtlMillis: Long = 5 * 60 * 1000L, // 5 minutes default TTL
    private val httpTimeoutMs: Int = 6000,
    private val httpFetcher: HttpFetcher = DefaultHttpFetcher(),
    private val clock: Clock = DefaultClock()
) {
    private val cache = ConcurrentHashMap<String, LiveResolutionResult>()
    private val channelMutexes = ConcurrentHashMap<String, Mutex>()

    private fun getMutexForChannel(channelId: String): Mutex {
        return channelMutexes.computeIfAbsent(channelId) { Mutex() }
    }

    private fun isCacheValid(cached: LiveResolutionResult, now: Long): Boolean {
        val isFailed = cached.liveStatus == LiveStatus.UNKNOWN || cached.liveStatus == LiveStatus.OFFLINE
        val dynamicTtl = if (isFailed) 30 * 1000L else cacheTtlMillis
        return now - cached.resolvedTimestamp < dynamicTtl
    }

    suspend fun resolveChannelLiveStatus(
        channel: Channel,
        forceRefresh: Boolean = false
    ): LiveResolutionResult = withContext(Dispatchers.IO) {
        val now = clock.currentTimeMillis()
        val channelId = channel.id

        // VOD / EXTERNAL_WEB / EMBED_DISABLED edge cases
        if (channel.isVod) {
            return@withContext LiveResolutionResult(
                liveStatus = LiveStatus.VOD,
                liveVideoId = channel.youtubeVideoId,
                resolvedAt = getCurrentIsoTimestamp(),
                resolvedTimestamp = now,
                lastError = null
            )
        }

        if (channel.type == StreamType.EXTERNAL_WEB || channel.status == PlaybackStatus.EMBED_DISABLED) {
            return@withContext LiveResolutionResult(
                liveStatus = LiveStatus.EMBED_DISABLED,
                liveVideoId = channel.youtubeVideoId,
                resolvedAt = getCurrentIsoTimestamp(),
                resolvedTimestamp = now,
                lastError = channel.statusMessage
            )
        }

        if (channel.type != StreamType.YOUTUBE_IFRAME) {
            return@withContext LiveResolutionResult(
                liveStatus = LiveStatus.UNKNOWN,
                liveVideoId = null,
                resolvedAt = getCurrentIsoTimestamp(),
                resolvedTimestamp = now,
                lastError = null
            )
        }

        // Check Cache first
        if (!forceRefresh) {
            val cached = cache[channelId]
            if (cached != null && isCacheValid(cached, now)) {
                return@withContext cached
            }
        }

        // Concurrency Lock per channel to prevent duplicate parallel requests
        val mutex = getMutexForChannel(channelId)
        mutex.withLock {
            if (!forceRefresh) {
                val cached = cache[channelId]
                if (cached != null && isCacheValid(cached, now)) {
                    return@withLock cached
                }
            }

            val result = performLiveResolution(channel, now)
            cache[channelId] = result
            result
        }
    }

    private fun performLiveResolution(channel: Channel, now: Long): LiveResolutionResult {
        val timestamp = getCurrentIsoTimestamp()

        // Strategy 1: Resolve via youtubeChannelId streams tab for multiple programs
        if (!channel.youtubeChannelId.isNullOrEmpty()) {
            val channelStreamsUrl = "https://www.youtube.com/channel/${channel.youtubeChannelId}/streams"
            val streamsResult = fetchAndParseStreamsData(channelStreamsUrl)
            if (streamsResult.isNotEmpty()) {
                val liveItem = streamsResult.firstOrNull { !it.isUpcoming } ?: streamsResult.first()
                val status = if (liveItem.isUpcoming) LiveStatus.UPCOMING else LiveStatus.LIVE
                return LiveResolutionResult(
                    liveStatus = status,
                    liveVideoId = liveItem.youtubeVideoId,
                    resolvedAt = timestamp,
                    resolvedTimestamp = now,
                    lastError = null,
                    liveItems = streamsResult
                )
            }

            // Fallback to /live endpoint if streams parse failed or empty
            val channelLiveUrl = "https://www.youtube.com/channel/${channel.youtubeChannelId}/live"
            val chResult = fetchAndParseYouTubeHtml(channelLiveUrl)
            if (chResult.isSuccess && chResult.videoId != null) {
                val status = if (chResult.unplayableReason != null) LiveStatus.EMBED_DISABLED else if (chResult.isLive) LiveStatus.LIVE else LiveStatus.OFFLINE
                return LiveResolutionResult(
                    liveStatus = status,
                    liveVideoId = if (status == LiveStatus.LIVE || status == LiveStatus.EMBED_DISABLED) chResult.videoId else null,
                    resolvedAt = timestamp,
                    resolvedTimestamp = now,
                    lastError = chResult.unplayableReason
                )
            }
        }

        // Strategy 2: Fallback to checking youtubeVideoId page directly
        if (!channel.youtubeVideoId.isNullOrEmpty()) {
            val watchUrl = "https://www.youtube.com/watch?v=${channel.youtubeVideoId}"
            val watchResult = fetchAndParseYouTubeHtml(watchUrl)
            if (watchResult.isSuccess) {
                val status = if (watchResult.unplayableReason != null) LiveStatus.EMBED_DISABLED else if (watchResult.isLive) LiveStatus.LIVE else LiveStatus.OFFLINE
                return LiveResolutionResult(
                    liveStatus = status,
                    liveVideoId = if (status == LiveStatus.LIVE || status == LiveStatus.EMBED_DISABLED) channel.youtubeVideoId else null,
                    resolvedAt = timestamp,
                    resolvedTimestamp = now,
                    lastError = watchResult.unplayableReason
                )
            } else {
                return LiveResolutionResult(
                    liveStatus = LiveStatus.UNKNOWN,
                    liveVideoId = null,
                    resolvedAt = timestamp,
                    resolvedTimestamp = now,
                    lastError = watchResult.errorMsg ?: "無法連線至 YouTube 頁面"
                )
            }
        }

        return LiveResolutionResult(
            liveStatus = LiveStatus.UNKNOWN,
            liveVideoId = null,
            resolvedAt = timestamp,
            resolvedTimestamp = now,
            lastError = "頻道未配置有效的 YouTube Channel ID 或 Video ID"
        )
    }

    private data class ParsedPage(
        val isSuccess: Boolean,
        val videoId: String?,
        val isLive: Boolean,
        val unplayableReason: String?,
        val errorMsg: String?
    )

    private fun fetchAndParseYouTubeHtml(targetUrl: String): ParsedPage {
        try {
            val (responseCode, rawHtml) = httpFetcher.fetch(targetUrl, httpTimeoutMs)

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return ParsedPage(false, null, false, null, "HTTP Error $responseCode")
            }

            val finalUrlLine = rawHtml.lines().lastOrNull { it.startsWith("<!-- URL: ") } ?: ""
            val finalUrl = finalUrlLine.removePrefix("<!-- URL: ").removeSuffix(" -->").trim()
            val html = rawHtml

            // Extract video ID
            val linkPattern = Pattern.compile("<link rel=\"canonical\" href=\"https://www.youtube.com/watch\\?v=([a-zA-Z0-9_-]{11})\"")
            val mLink = linkPattern.matcher(html)
            var extractedVid: String? = if (mLink.find()) mLink.group(1) else null

            if (extractedVid == null) {
                val vidPattern1 = Pattern.compile("\"videoId\":\"([a-zA-Z0-9_-]{11})\"")
                val m1 = vidPattern1.matcher(html)
                if (m1.find()) extractedVid = m1.group(1)
            }

            if (extractedVid == null) {
                val vidPattern2 = Pattern.compile("v=([a-zA-Z0-9_-]{11})")
                val m2 = vidPattern2.matcher(finalUrl)
                if (m2.find()) extractedVid = m2.group(1)
            }

            // Check if page is currently LIVE
            // Under bot-check ("LOGIN_REQUIRED"), "isLiveNow":true might be missing from microformat,
            // but "isLive":true is usually still present in ytInitialData/ytInitialPlayerResponse for true live streams.
            // We use a regex to ensure it only matches within the main video context and not sidebar recommendations.
            val isLivePattern = Pattern.compile("\"videoViewCountRenderer\":\\{[\\s\\S]*?\"isLive\":true")
            var isLive = html.contains("\"isLiveNow\":true") ||
                    isLivePattern.matcher(html).find() ||
                    (html.contains("hlsManifestUrl") && !html.contains("\"isUpcoming\":true"))

            var unplayableMsg: String? = null
            var isGeoRestricted: Boolean

            val pattern = Pattern.compile("var ytInitialData = (.*?);</script>")
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val jsonString = matcher.group(1)
                val unplayablePattern = Pattern.compile("\"unplayableText\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"\\}")
                val unm = unplayablePattern.matcher(jsonString ?: "") // Unplayable usually appears in videoDetails or playabilityStatus
                if (unm.find()) unplayableMsg = unm.group(1)

                val geoRestrictedPattern = Pattern.compile("\"reason\":\"[^\"]*This video is not available in your country[^\"]*\"")
                val geoRestrictedPattern2 = Pattern.compile("\"reason\":\"[^\"]*The uploader has not made this video available in your country[^\"]*\"")
                isGeoRestricted = geoRestrictedPattern.matcher(jsonString ?: "").find() || geoRestrictedPattern2.matcher(jsonString ?: "").find()
            } else {
                // Fallback to naive global search if ytInitialData isn't structured as expected
                isLive = html.contains("\"isLiveNow\":true") || html.contains("\"isLive\":true") ||
                        (html.contains("hlsManifestUrl") && !html.contains("\"isUpcoming\":true"))

                val unplayablePattern = Pattern.compile("\"unplayableText\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"\\}")
                val unm = unplayablePattern.matcher(html)
                if (unm.find()) unplayableMsg = unm.group(1)

                val geoRestrictedPattern = Pattern.compile("\"reason\":\"[^\"]*This video is not available in your country[^\"]*\"")
                val geoRestrictedPattern2 = Pattern.compile("\"reason\":\"[^\"]*The uploader has not made this video available in your country[^\"]*\"")
                isGeoRestricted = geoRestrictedPattern.matcher(html).find() || geoRestrictedPattern2.matcher(html).find()
            }

            val finalUnplayableMsg = if (isGeoRestricted) "此影片在您的國家/地區無法播放 (Geo Restricted)" else unplayableMsg

            return ParsedPage(
                isSuccess = true,
                videoId = extractedVid,
                isLive = isLive,
                unplayableReason = finalUnplayableMsg,
                errorMsg = null
            )
        } catch (e: Exception) {
            return ParsedPage(
                isSuccess = false,
                videoId = null,
                isLive = false,
                unplayableReason = null,
                errorMsg = e.message ?: "連線失敗"
            )
        }
    }

    private fun fetchAndParseStreamsData(targetUrl: String): List<com.taiwanfreetv.app.data.model.LiveItem> {
        try {
            val (responseCode, rawHtml) = httpFetcher.fetch(targetUrl, httpTimeoutMs)
            if (responseCode != HttpURLConnection.HTTP_OK) return emptyList()

            val pattern = Pattern.compile("var ytInitialData = (.*?);</script>")
            val matcher = pattern.matcher(rawHtml)
            if (!matcher.find()) return emptyList()

            val jsonString = matcher.group(1) ?: return emptyList()
            val result = mutableListOf<com.taiwanfreetv.app.data.model.LiveItem>()

            // Traverse JSON
            val vidPattern = Pattern.compile("\"videoId\":\"([a-zA-Z0-9_-]{11})\"")
            val vids = mutableSetOf<String>()
            val vidMatcher = vidPattern.matcher(jsonString)
            while(vidMatcher.find()) {
                vidMatcher.group(1)?.let { vids.add(it) }
            }

            // We use simple regex extraction over the json block to avoid massive GSON trees.
            // Match blocks roughly around videoId
            for (vid in vids) {
                val blockPattern = Pattern.compile("\\{\"videoId\":\"$vid\".*?\"title\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"\\}.*?\"thumbnailOverlays\":\\[(.*?)\\]", Pattern.DOTALL)
                val blockMatcher = blockPattern.matcher(jsonString)
                if (blockMatcher.find()) {
                    val title = blockMatcher.group(1) ?: continue
                    val overlays = blockMatcher.group(2) ?: ""

                    var isLive = false
                    var isUpcoming = false
                    var scheduledTime: String? = null

                    if (overlays.contains("\"style\":\"LIVE\"")) {
                        isLive = true
                    } else if (overlays.contains("\"style\":\"UPCOMING\"")) {
                        isUpcoming = true

                        // Extract scheduled time text
                        val timePattern = Pattern.compile("\"style\":\"UPCOMING\".*?\"text\":\"([^\"]+)\"")
                        val timeMatcher = timePattern.matcher(overlays)
                        if (timeMatcher.find()) {
                            scheduledTime = timeMatcher.group(1)
                        }
                    }

                    if (isLive || isUpcoming) {
                        result.add(
                            com.taiwanfreetv.app.data.model.LiveItem(
                                id = vid,
                                title = title,
                                youtubeVideoId = vid,
                                isUpcoming = isUpcoming,
                                scheduledStartTime = scheduledTime
                            )
                        )
                    }
                }
            }
            return result
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun getCurrentIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(clock.currentTimeMillis()))
    }

    private fun parseIsoTimestamp(timestamp: String?): Long {
        if (timestamp.isNullOrEmpty()) return 0L
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(timestamp)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun clearCache() {
        cache.clear()
    }
}
