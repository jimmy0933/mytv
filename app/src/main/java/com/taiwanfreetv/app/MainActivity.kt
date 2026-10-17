package com.taiwanfreetv.app

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.taiwanfreetv.app.data.model.Channel
import com.taiwanfreetv.app.data.model.PlaybackStatus
import com.taiwanfreetv.app.data.model.StreamType
import com.taiwanfreetv.app.data.repository.ChannelRepository
import com.taiwanfreetv.app.data.repository.UserPreferencesRepository
import com.taiwanfreetv.app.ui.home.HomeScreen
import com.taiwanfreetv.app.ui.player.PlayerScreen
import com.taiwanfreetv.app.ui.player.launchOfficialUrl
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    data class Player(val channel: Channel) : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var channelRepository: ChannelRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    private var currentScreen by mutableStateOf<Screen>(Screen.Home)
    private var allChannels by mutableStateOf<List<Channel>>(emptyList())
    private var categories by mutableStateOf<List<String>>(listOf("全部"))
    private var selectedCategory by mutableStateOf("全部")
    private val playbackFilters = listOf("全部頻道", "App 內可播放", "官方平台觀看")
    private var selectedPlaybackFilter by mutableStateOf("全部頻道")
    private var searchQuery by mutableStateOf("")
    private var favoriteIds by mutableStateOf<Set<String>>(emptySet())

    private var resolveJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        channelRepository = ChannelRepository(this)
        userPreferencesRepository = UserPreferencesRepository(this)

        // Sequentially load channel configurations and then restore last watched channel
        lifecycleScope.launch {
            try {
                val config = channelRepository.loadLocalConfig()
                allChannels = config.channels
                categories = config.categories

                // Restore last watched channel after allChannels is populated
                val lastChannelId = userPreferencesRepository.lastWatchedChannelId.first()
                if (!lastChannelId.isNullOrEmpty() && allChannels.isNotEmpty()) {
                    val found = allChannels.find { it.id == lastChannelId }
                    if (found != null && found.status == PlaybackStatus.PLAYABLE && found.type != StreamType.EXTERNAL_WEB) {
                        val resolvedFound = channelRepository.resolveChannelLive(found)
                        currentScreen = Screen.Player(resolvedFound)
                    }
                }

                // Asynchronously resolve dynamic live stream status for channels in background
                resolveAllChannelsInBackground()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Collect favorite channels state
        lifecycleScope.launch {
            userPreferencesRepository.favoriteChannelIds.collect { favs ->
                favoriteIds = favs
            }
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    val filteredChannels = remember(allChannels, selectedCategory, selectedPlaybackFilter, searchQuery, favoriteIds) {
                        allChannels.filter { channel ->
                            val matchesCategory = when (selectedCategory) {
                                "全部" -> true
                                "收藏" -> favoriteIds.contains(channel.id)
                                else -> channel.category == selectedCategory
                            }
                            val matchesPlaybackFilter = when (selectedPlaybackFilter) {
                                "App 內可播放" -> channel.status == PlaybackStatus.PLAYABLE && channel.type != StreamType.EXTERNAL_WEB
                                "官方平台觀看" -> channel.status == PlaybackStatus.EMBED_DISABLED || channel.type == StreamType.EXTERNAL_WEB
                                else -> true
                            }
                            val matchesSearch = if (searchQuery.isBlank()) {
                                true
                            } else {
                                channel.name.contains(searchQuery, ignoreCase = true) ||
                                        (channel.number.toString() == searchQuery)
                            }
                            matchesCategory && matchesPlaybackFilter && matchesSearch
                        }
                    }

                    when (val screen = currentScreen) {
                        is Screen.Home -> {
                            HomeScreen(
                                channels = filteredChannels,
                                categories = categories,
                                selectedCategory = selectedCategory,
                                selectedPlaybackFilter = selectedPlaybackFilter,
                                playbackFilters = playbackFilters,
                                searchQuery = searchQuery,
                                favoriteIds = favoriteIds,
                                onCategorySelected = { selectedCategory = it },
                                onPlaybackFilterSelected = { selectedPlaybackFilter = it },
                                onSearchQueryChanged = { searchQuery = it },
                                onChannelClick = { channel ->
                                    if (channel.type == StreamType.EXTERNAL_WEB || channel.status == PlaybackStatus.EMBED_DISABLED) {
                                        launchOfficialUrl(this@MainActivity, channel.officialUrl)
                                    } else {
                                        playChannel(channel)
                                    }
                                },
                                onToggleFavorite = { channel ->
                                    lifecycleScope.launch {
                                        userPreferencesRepository.toggleFavorite(channel.id)
                                    }
                                }
                            )
                        }

                        is Screen.Player -> {
                            PlayerScreen(
                                currentChannel = screen.channel,
                                channels = allChannels,
                                favoriteIds = favoriteIds,
                                onBackClick = { currentScreen = Screen.Home },
                                onChannelSelect = { channel -> playChannel(channel) },
                                onNextChannel = { switchNextChannel(screen.channel) },
                                onPreviousChannel = { switchPreviousChannel(screen.channel) },
                                onToggleFavorite = { channel ->
                                    lifecycleScope.launch {
                                        userPreferencesRepository.toggleFavorite(channel.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun playChannel(channel: Channel) {
        resolveJob?.cancel()
        resolveJob = lifecycleScope.launch {
            userPreferencesRepository.setLastWatchedChannelId(channel.id)

            // Show loading or previous screen state while resolving to prevent UI overwrite issues
            if (currentScreen !is Screen.Player || (currentScreen as Screen.Player).channel.id != channel.id) {
                currentScreen = Screen.Player(channel) // Set immediately to channel to let UI show loading state
            }

            val resolvedChannel = if (channel.type == StreamType.YOUTUBE_IFRAME && !channel.isVod) {
                channelRepository.resolveChannelLive(channel)
            } else {
                channel
            }
            currentScreen = Screen.Player(resolvedChannel)
        }
    }

    private fun resolveAllChannelsInBackground() {
        lifecycleScope.launch {
            val updatedList = allChannels.map { ch ->
                if (ch.type == StreamType.YOUTUBE_IFRAME && !ch.isVod) {
                    channelRepository.resolveChannelLive(ch)
                } else {
                    ch
                }
            }
            allChannels = updatedList
        }
    }

    private fun switchNextChannel(current: Channel) {
        if (allChannels.isEmpty()) return
        val currentIndex = allChannels.indexOfFirst { it.id == current.id }
        val nextIndex = if (currentIndex == -1 || currentIndex == allChannels.size - 1) 0 else currentIndex + 1
        playChannel(allChannels[nextIndex])
    }

    private fun switchPreviousChannel(current: Channel) {
        if (allChannels.isEmpty()) return
        val currentIndex = allChannels.indexOfFirst { it.id == current.id }
        val prevIndex = if (currentIndex <= 0) allChannels.size - 1 else currentIndex - 1
        playChannel(allChannels[prevIndex])
    }

    // Android TV D-Pad Remote Hardware Key Handling
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (currentScreen is Screen.Player) {
            val playerScreen = currentScreen as Screen.Player
            when (keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_DPAD_UP -> {
                    switchPreviousChannel(playerScreen.channel)
                    return true
                }
                KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    switchNextChannel(playerScreen.channel)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (currentScreen is Screen.Player) {
            currentScreen = Screen.Home
        } else {
            super.onBackPressed()
        }
    }
}
