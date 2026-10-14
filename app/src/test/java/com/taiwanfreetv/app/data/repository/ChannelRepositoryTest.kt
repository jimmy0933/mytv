package com.taiwanfreetv.app.data.repository

import com.google.gson.Gson
import com.taiwanfreetv.app.data.model.ChannelConfig
import com.taiwanfreetv.app.data.model.PlaybackStatus
import com.taiwanfreetv.app.data.model.StreamType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelRepositoryTest {

    private val sampleJson = """
    {
      "version": 3,
      "updatedAt": "2026-03-31",
      "categories": ["全部", "新聞", "國際新聞", "財經"],
      "channels": [
        {
          "id": "ctv_news",
          "name": "中視新聞台",
          "category": "新聞",
          "type": "youtube_iframe",
          "youtube_video_id": "TCnaIE_SAtM",
          "official_url": "https://www.ctv.com.tw/",
          "embeddable": true,
          "status": "playable",
          "status_message": "官方 YouTube 24小時直播可成功內嵌播放",
          "last_verified": "2026-03-31",
          "number": 10
        },
        {
          "id": "nhk_world_japan",
          "name": "NHK WORLD-JAPAN",
          "category": "國際新聞",
          "type": "external_web",
          "official_url": "https://www3.nhk.or.jp/nhkworld/en/live_tv/",
          "embeddable": false,
          "status": "embed_disabled",
          "status_message": "無第三方 App 內嵌授權，引導至官方直播網頁觀看",
          "last_verified": "2026-03-31",
          "number": 14
        }
      ]
    }
    """.trimIndent()

    @Test
    fun testParseConfigJson() {
        val gson = Gson()
        val config = gson.fromJson(sampleJson, ChannelConfig::class.java)

        assertNotNull(config)
        assertEquals(3, config.version)
        assertEquals(4, config.categories.size)
        assertEquals(2, config.channels.size)

        val ch1 = config.channels[0]
        assertEquals("ctv_news", ch1.id)
        assertEquals(StreamType.YOUTUBE_IFRAME, ch1.type)
        assertEquals("TCnaIE_SAtM", ch1.youtubeVideoId)
        assertTrue(ch1.embeddable)
        assertEquals(PlaybackStatus.PLAYABLE, ch1.status)

        val ch2 = config.channels[1]
        assertEquals("nhk_world_japan", ch2.id)
        assertEquals(StreamType.EXTERNAL_WEB, ch2.type)
        assertEquals(PlaybackStatus.EMBED_DISABLED, ch2.status)
        assertEquals("2026-03-31", ch2.lastVerified)
    }

    @Test
    fun testPlaybackStatusParsing() {
        val gson = Gson()
        val config = gson.fromJson(sampleJson, ChannelConfig::class.java)
        val nhkChannel = config.channels.find { it.id == "nhk_world_japan" }
        assertNotNull(nhkChannel)
        assertEquals(PlaybackStatus.EMBED_DISABLED, nhkChannel?.status)
        assertEquals("無第三方 App 內嵌授權，引導至官方直播網頁觀看", nhkChannel?.statusMessage)
    }

    @Test
    fun testChannelListWithStatus() {
        val gson = Gson()
        val config = gson.fromJson(sampleJson, ChannelConfig::class.java)
        assertTrue(config.channels.all { it.status != null })
        assertEquals(1, config.channels.count { it.status == PlaybackStatus.PLAYABLE })
        assertEquals(1, config.channels.count { it.status == PlaybackStatus.EMBED_DISABLED })
    }
}
