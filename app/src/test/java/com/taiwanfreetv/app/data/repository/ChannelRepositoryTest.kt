package com.taiwanfreetv.app.data.repository

import com.google.gson.Gson
import com.taiwanfreetv.app.data.model.ChannelConfig
import com.taiwanfreetv.app.data.model.StreamType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelRepositoryTest {

    private val sampleJson = """
    {
      "version": 1,
      "updatedAt": "2026-03-31",
      "categories": ["全部", "新聞", "財經"],
      "channels": [
        {
          "id": "ctv_news",
          "name": "中視新聞台",
          "category": "新聞",
          "type": "youtube_iframe",
          "youtube_video_id": "TCnaIE_SAtM",
          "official_url": "https://www.ctv.com.tw/",
          "embeddable": true,
          "number": 51
        },
        {
          "id": "hls_sample",
          "name": "HLS Sample",
          "category": "綜合",
          "type": "hls",
          "stream_url": "https://example.com/live.m3u8",
          "official_url": "https://example.com",
          "embeddable": true,
          "number": 99
        }
      ]
    }
    """.trimIndent()

    @Test
    fun testParseConfigJson() {
        val gson = Gson()
        val config = gson.fromJson(sampleJson, ChannelConfig::class.java)

        assertNotNull(config)
        assertEquals(1, config.version)
        assertEquals(3, config.categories.size)
        assertEquals(2, config.channels.size)

        val ch1 = config.channels[0]
        assertEquals("ctv_news", ch1.id)
        assertEquals(StreamType.YOUTUBE_IFRAME, ch1.type)
        assertEquals("TCnaIE_SAtM", ch1.youtubeVideoId)
        assertTrue(ch1.embeddable)

        val ch2 = config.channels[1]
        assertEquals("hls_sample", ch2.id)
        assertEquals(StreamType.HLS, ch2.type)
        assertEquals("https://example.com/live.m3u8", ch2.streamUrl)
    }
}
