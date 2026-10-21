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
    private val context: Context
) {

    private val gson = Gson()

    suspend fun loadLocalConfig(): ChannelConfig = withContext(Dispatchers.IO) {
        val jsonString = context.assets.open("channels.json").bufferedReader().use { it.readText() }
        val config = gson.fromJson(jsonString, ChannelConfig::class.java)
        validateConfig(config)
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
                val config = gson.fromJson(jsonString, ChannelConfig::class.java)
                return@withContext validateConfig(config)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    fun parseConfigJson(jsonString: String): ChannelConfig {
        val config = gson.fromJson(jsonString, ChannelConfig::class.java)
        return validateConfig(config)
    }

    private fun validateConfig(config: ChannelConfig?): ChannelConfig {
        if (config == null) return ChannelConfig(0, "", emptyList(), emptyList())
        val validChannels = mutableListOf<Channel>()
        val seenIds = mutableSetOf<String>()

        for (channel in config.channels) {
            if (channel.id.isBlank()) continue
            if (seenIds.contains(channel.id)) continue // Skip duplicates

            seenIds.add(channel.id)

            val validatedSources = channel.sources.map { source ->
                if (source.type == com.taiwanfreetv.app.data.model.StreamType.HLS && source.streamUrl.isNullOrBlank()) {
                    source
                } else {
                    source
                }
            }

            var newStatus = channel.status

            val hasValidHls = validatedSources.any { it.type == com.taiwanfreetv.app.data.model.StreamType.HLS && !it.streamUrl.isNullOrBlank() }
            val hasWebOnly = validatedSources.any { it.type == com.taiwanfreetv.app.data.model.StreamType.EXTERNAL_WEB }
            val hasYouTube = validatedSources.any { it.type == com.taiwanfreetv.app.data.model.StreamType.YOUTUBE_IFRAME }

            if (!hasValidHls) {
                if (hasYouTube) {
                    newStatus = com.taiwanfreetv.app.data.model.PlaybackStatus.NO_SOURCE
                } else if (hasWebOnly) {
                    newStatus = com.taiwanfreetv.app.data.model.PlaybackStatus.WEB_ONLY
                } else {
                    newStatus = com.taiwanfreetv.app.data.model.PlaybackStatus.NO_SOURCE
                }
            }

            validChannels.add(channel.copy(sources = validatedSources, status = newStatus))
        }

        return config.copy(channels = validChannels)
    }

    suspend fun resolveChannelLive(channel: Channel, forceRefresh: Boolean = false): Channel {
        return channel
    }
}