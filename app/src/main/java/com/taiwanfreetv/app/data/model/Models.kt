package com.taiwanfreetv.app.data.model

import com.google.gson.annotations.SerializedName

enum class StreamType {
    @SerializedName("youtube_iframe")
    YOUTUBE_IFRAME,

    @SerializedName("hls")
    HLS,

    @SerializedName("external_web")
    EXTERNAL_WEB
}

enum class PlaybackStatus {
    @SerializedName("playable")
    PLAYABLE,

    @SerializedName("embed_disabled")
    EMBED_DISABLED,

    @SerializedName("geo_restricted")
    GEO_RESTRICTED,

    @SerializedName("source_invalid")
    SOURCE_INVALID,

    @SerializedName("unverified")
    UNVERIFIED
}

enum class LiveStatus {
    @SerializedName("live")
    LIVE,

    @SerializedName("offline")
    OFFLINE,

    @SerializedName("unknown")
    UNKNOWN,

    @SerializedName("embed_disabled")
    EMBED_DISABLED,

    @SerializedName("vod")
    VOD
}

data class VodItem(
    val id: String,
    val title: String,
    @SerializedName("youtube_video_id")
    val youtubeVideoId: String,
    val duration: String? = null,
    val description: String? = null
)

data class Channel(
    val id: String,
    val name: String,
    val category: String,
    val type: StreamType,
    @SerializedName("youtube_video_id")
    val youtubeVideoId: String? = null,
    @SerializedName("youtube_channel_id")
    val youtubeChannelId: String? = null,
    @SerializedName("current_live_video_id")
    val currentLiveVideoId: String? = null,
    @SerializedName("stream_url")
    val streamUrl: String? = null,
    @SerializedName("official_url")
    val officialUrl: String,
    val embeddable: Boolean = true,
    @SerializedName("logo_url")
    val logoUrl: String? = null,
    val number: Int = 0,
    val description: String? = null,
    val status: PlaybackStatus = PlaybackStatus.PLAYABLE,
    @SerializedName("live_status")
    val liveStatus: LiveStatus = LiveStatus.UNKNOWN,
    @SerializedName("resolved_at")
    val resolvedAt: String? = null,
    @SerializedName("last_error")
    val lastError: String? = null,
    @SerializedName("status_message")
    val statusMessage: String? = null,
    @SerializedName("last_verified")
    val lastVerified: String? = null,
    @SerializedName("is_vod")
    val isVod: Boolean = false,
    @SerializedName("vod_items")
    val vodItems: List<VodItem>? = null
)

data class EPGProgram(
    val id: String,
    val channelId: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val description: String? = null
)

data class ChannelConfig(
    val version: Int,
    val updatedAt: String,
    val channels: List<Channel>,
    val categories: List<String>
)
