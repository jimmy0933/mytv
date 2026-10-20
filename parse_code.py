import re

with open('app/src/main/java/com/taiwanfreetv/app/data/model/Models.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add Source model and updated Channel
source_model = """
data class Source(
    val type: StreamType,
    @SerializedName("youtube_video_id")
    val youtubeVideoId: String? = null,
    @SerializedName("youtube_channel_id")
    val youtubeChannelId: String? = null,
    @SerializedName("stream_url")
    val streamUrl: String? = null,
    @SerializedName("official_url")
    val officialUrl: String? = null
)
"""

if "data class Source" not in content:
    content = content.replace("data class Channel(", source_model + "\ndata class Channel(")

content = content.replace("val id: String,", "val id: String,\n    val sources: List<Source> = emptyList(),")

# Also add PlaybackStatus.GEO_RESTRICTED if missing.
# Note: wait, it's already there in Models.kt

with open('app/src/main/java/com/taiwanfreetv/app/data/model/Models.kt', 'w', encoding='utf-8') as f:
    f.write(content)
