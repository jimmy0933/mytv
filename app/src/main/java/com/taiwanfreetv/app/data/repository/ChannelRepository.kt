package com.taiwanfreetv.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.taiwanfreetv.app.data.model.Channel
import com.taiwanfreetv.app.data.model.ChannelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ChannelRepository(
    private val context: Context,
    val liveResolver: YouTubeLiveResolver = YouTubeLiveResolver()
) {

    private val gson = Gson()

    suspend fun loadLocalConfig(): ChannelConfig = withContext(Dispatchers.IO) {
        val jsonString = context.assets.open("channels.json").bufferedReader().use { it.readText() }
        gson.fromJson(jsonString, ChannelConfig::class.java)
    }

    suspend fun fetchRemoteConfig(remoteUrl: String): ChannelConfig? = withContext(Dispatchers.IO) {
        try {
            val url = URL(remoteUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val jsonString = reader.use { it.readText() }
                return@withContext gson.fromJson(jsonString, ChannelConfig::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    fun parseConfigJson(jsonString: String): ChannelConfig {
        return gson.fromJson(jsonString, ChannelConfig::class.java)
    }

    suspend fun resolveChannelLive(channel: Channel, forceRefresh: Boolean = false): Channel {
        val res = liveResolver.resolveChannelLiveStatus(channel, forceRefresh)
        val activeVideoId = if (res.liveStatus == com.taiwanfreetv.app.data.model.LiveStatus.LIVE || res.liveStatus == com.taiwanfreetv.app.data.model.LiveStatus.VOD) {
            res.liveVideoId ?: channel.youtubeVideoId
        } else {
            null
        }
        return channel.copy(
            liveStatus = res.liveStatus,
            currentLiveVideoId = res.liveVideoId,
            youtubeVideoId = activeVideoId,
            resolvedAt = res.resolvedAt,
            lastError = res.lastError
        )
    }
}
