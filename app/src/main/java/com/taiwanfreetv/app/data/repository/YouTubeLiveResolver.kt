package com.taiwanfreetv.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object YouTubeLiveResolver {
    private val cache = mutableMapOf<String, Pair<String, Long>>()
    private val CACHE_EXPIRATION_MS = 10 * 60 * 1000L // 10 minutes
    private val mutex = Mutex()
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

    suspend fun resolveLiveVideoId(channelName: String): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cacheKey = channelName

        mutex.withLock {
            val cached = cache[cacheKey]
            if (cached != null && (now - cached.second) < CACHE_EXPIRATION_MS) {
                return@withContext cached.first
            }
        }

        // Try searching YouTube for live videos of the channel
        try {
            val encodedQuery = java.net.URLEncoder.encode("${channelName}", "UTF-8")
            val url = URL("https://www.youtube.com/results?search_query=$encodedQuery&sp=EgJAAQ%253D%253D")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }

                val videoIdPattern = Pattern.compile("\"videoId\":\"([^\"]{11})\"")
                val matcher = videoIdPattern.matcher(html)

                val videoIds = mutableListOf<String>()
                while (matcher.find()) {
                    videoIds.add(matcher.group(1)!!)
                }

                // Verify the video is actually live
                for (vid in videoIds.distinct()) {
                    if (isLiveVideo(vid)) {
                        mutex.withLock {
                            cache[cacheKey] = Pair(vid, now)
                        }
                        return@withContext vid
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        null
    }

    private fun isLiveVideo(videoId: String): Boolean {
        try {
            val url = URL("https://www.youtube.com/watch?v=$videoId")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                return html.contains("\"isLive\":true")
            }
        } catch (e: Exception) {
             e.printStackTrace()
        }
        return false
    }
}
