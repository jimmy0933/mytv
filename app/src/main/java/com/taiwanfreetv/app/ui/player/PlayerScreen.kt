package com.taiwanfreetv.app.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taiwanfreetv.app.data.model.Channel
import com.taiwanfreetv.app.data.model.PlaybackStatus
import com.taiwanfreetv.app.data.model.StreamType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    currentChannel: Channel,
    channels: List<Channel>,
    favoriteIds: Set<String>,
    onBackClick: () -> Unit,
    onChannelSelect: (Channel) -> Unit,
    onNextChannel: () -> Unit,
    onPreviousChannel: () -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier,
    onRetry: (Channel) -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isDrawerOpen by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorCode by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var retryTrigger by remember { mutableStateOf(0) }
    var sourceIndex by remember(currentChannel.id) { mutableIntStateOf(0) }
    val activeSource = currentChannel.sources.getOrNull(sourceIndex) ?: com.taiwanfreetv.app.data.model.Source(
        type = currentChannel.type,
        youtubeVideoId = currentChannel.youtubeVideoId,
        youtubeChannelId = currentChannel.youtubeChannelId,
        streamUrl = currentChannel.streamUrl,
        officialUrl = currentChannel.officialUrl
    )
    var areControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val latestChannelId by androidx.compose.runtime.rememberUpdatedState(currentChannel.id)
    var activeVodVideoId by remember(currentChannel.id) {
        mutableStateOf(currentChannel.youtubeVideoId)
    }

    val isPermanentEmbedRestriction = remember(currentChannel, activeSource, errorCode) {
        activeSource.type == StreamType.EXTERNAL_WEB ||
        activeSource.type == StreamType.YOUTUBE_IFRAME ||
        currentChannel.status == PlaybackStatus.EMBED_DISABLED ||
        currentChannel.status == PlaybackStatus.NO_SOURCE ||
        currentChannel.status == PlaybackStatus.WEB_ONLY
    }

    LaunchedEffect(currentChannel.id, currentChannel.liveStatus, retryTrigger) {
        isLoading = false
        errorMessage = null
        errorCode = 0
        areControlsVisible = true
        lastInteractionTime = System.currentTimeMillis()
    }

    LaunchedEffect(areControlsVisible, lastInteractionTime, isDrawerOpen, isLandscape) {
        if (isLandscape && areControlsVisible && !isDrawerOpen) {
            delay(3500)
            areControlsVisible = false
        }
    }

    val onUserInteract = {
        lastInteractionTime = System.currentTimeMillis()
        if (!areControlsVisible) {
            areControlsVisible = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (isLandscape) {
            // Landscape Mode
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        areControlsVisible = !areControlsVisible
                        if (areControlsVisible) lastInteractionTime = System.currentTimeMillis()
                    }
            ) {
                if (isPermanentEmbedRestriction) {
                    val customMsg = when {
                        errorCode == 101 || errorCode == 150 || errorCode == 152 || errorCode == 153 ->
                            "此頻道擁有者禁止在第三方 App 內嵌播放\n請點擊下方按鈕開啟官方直播網頁"
                        currentChannel.liveStatus == com.taiwanfreetv.app.data.model.LiveStatus.EMBED_DISABLED ->
                            "此頻道需透過官方平台觀看\n請點擊下方按鈕開啟官方直播網頁"
                        else -> null
                    }
                    ExternalFallbackComponent(
                        channel = currentChannel,
                        modifier = Modifier.fillMaxSize(),
                        customMessage = customMsg
                    )
                } else if (currentChannel.liveStatus == com.taiwanfreetv.app.data.model.LiveStatus.RESOLVING) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF1E88E5))
                    }
                } else {
                    val activeChannel = currentChannel.copy(youtubeVideoId = activeVodVideoId)
                                        PlayerCore(
                        currentChannel = activeChannel,
                        activeSource = activeSource,
                        retryTrigger = retryTrigger,
                        onLoadingChanged = { isLoading = it },
                        onError = { code, msg ->
                            if (currentChannel.sources.isNotEmpty() && sourceIndex < currentChannel.sources.size - 1) {
                                sourceIndex++
                            } else {
                                errorCode = code
                                errorMessage = msg
                                isLoading = false
                            }
                        },
                        onEnded = {
                            if (currentChannel.id == latestChannelId) {
                                val isPlayingVod = currentChannel.isVod || currentChannel.vodItems?.any { it.youtubeVideoId == activeVodVideoId } == true
                                if (!isPlayingVod) {
                                    onRetry(currentChannel)
                                }
                            }
                        }
                    )
                }

                AnimatedVisibility(
                    visible = areControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    PlayerControlsOverlayLandscape(
                        currentChannel = currentChannel,
                        favoriteIds = favoriteIds,
                        onBackClick = onBackClick,
                        onToggleFavorite = { onToggleFavorite(currentChannel) },
                        onToggleDrawer = {
                            isDrawerOpen = !isDrawerOpen
                            onUserInteract()
                        },
                        onPreviousChannel = {
                            onPreviousChannel()
                            onUserInteract()
                        },
                        onNextChannel = {
                            onNextChannel()
                            onUserInteract()
                        }
                    )
                }

                if (isLoading && errorMessage == null && !isPermanentEmbedRestriction) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF1E88E5))
                    }
                }
            }
        } else {
            // Portrait Layout
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF1A1A1A))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF2B2B2B), CircleShape)
                            .focusable()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回首頁",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = if (currentChannel.number > 0) "CH${currentChannel.number} ${currentChannel.name}" else currentChannel.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    val isFav = favoriteIds.contains(currentChannel.id)
                    IconButton(
                        onClick = { onToggleFavorite(currentChannel) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF2B2B2B), CircleShape)
                            .focusable()
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏頻道",
                            tint = if (isFav) Color.Red else Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            isDrawerOpen = !isDrawerOpen
                            onUserInteract()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF2B2B2B), CircleShape)
                            .focusable()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "頻道選單",
                            tint = Color.White
                        )
                    }
                }

                // Video Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isPermanentEmbedRestriction) Modifier.weight(1f)
                            else Modifier.aspectRatio(16f / 9f)
                        )
                        .background(Color.Black)
                ) {
                    if (isPermanentEmbedRestriction) {
                        val customMsg = when {
                            errorCode == 101 || errorCode == 150 || errorCode == 152 || errorCode == 153 ->
                                "此頻道擁有者禁止在第三方 App 內嵌播放\n請點擊下方按鈕開啟官方直播網頁"
                            currentChannel.liveStatus == com.taiwanfreetv.app.data.model.LiveStatus.EMBED_DISABLED ->
                                "此頻道需透過官方平台觀看\n請點擊下方按鈕開啟官方直播網頁"
                            else -> null
                        }
                        ExternalFallbackComponent(
                            channel = currentChannel,
                            modifier = Modifier.fillMaxSize(),
                            customMessage = customMsg
                        )
                    } else if (currentChannel.liveStatus == com.taiwanfreetv.app.data.model.LiveStatus.RESOLVING) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF1E88E5))
                        }
                    } else {
                        val activeChannel = currentChannel.copy(youtubeVideoId = activeVodVideoId)
                                            PlayerCore(
                        currentChannel = activeChannel,
                        activeSource = activeSource,
                        retryTrigger = retryTrigger,
                        onLoadingChanged = { isLoading = it },
                        onError = { code, msg ->
                            if (currentChannel.sources.isNotEmpty() && sourceIndex < currentChannel.sources.size - 1) {
                                sourceIndex++
                            } else {
                                errorCode = code
                                errorMessage = msg
                                isLoading = false
                            }
                        },
                        onEnded = {
                            if (currentChannel.id == latestChannelId) {
                                val isPlayingVod = currentChannel.isVod || currentChannel.vodItems?.any { it.youtubeVideoId == activeVodVideoId } == true
                                if (!isPlayingVod) {
                                    onRetry(currentChannel)
                                }
                            }
                        }
                    )
                    }

                    if (isLoading && errorMessage == null && !isPermanentEmbedRestriction) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x66000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF1E88E5))
                        }
                    }
                }

                // Channel Controls Section Below Video Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isPermanentEmbedRestriction) Modifier.wrapContentHeight() else Modifier.weight(1f))
                        .background(Color(0xFF121212))
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = Color(0xFF1E88E5),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (currentChannel.number > 0) "CH${currentChannel.number}" else currentChannel.category,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = currentChannel.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = currentChannel.description ?: "官方 24 小時實時線上直播",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )

                    if (currentChannel.isVod && !currentChannel.vodItems.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "節目清單 (隨選影片)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        currentChannel.vodItems.forEach { vod ->
                            val isSelectedVod = vod.youtubeVideoId == activeVodVideoId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { activeVodVideoId = vod.youtubeVideoId },
                                color = if (isSelectedVod) Color(0xFF1E88E5) else Color(0xFF222222)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = vod.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (!vod.description.isNullOrEmpty()) {
                                            Text(text = vod.description, color = Color.LightGray, fontSize = 12.sp)
                                        }
                                    }
                                    if (vod.duration != null) {
                                        Text(text = vod.duration, color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (!currentChannel.liveItems.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "頻道直播節目",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        currentChannel.liveItems.forEach { liveItem ->
                            val isSelected = liveItem.youtubeVideoId == activeVodVideoId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { activeVodVideoId = liveItem.youtubeVideoId },
                                color = if (isSelected) Color(0xFF1E88E5) else Color(0xFF222222)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = liveItem.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val tagColor = if (liveItem.isUpcoming) Color(0xFF1976D2) else Color(0xFFD32F2F)
                                            val tagText = if (liveItem.isUpcoming) "即將直播" else "直播中"
                                            Surface(
                                                color = tagColor,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = tagText,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            if (liveItem.isUpcoming && liveItem.scheduledStartTime != null) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = liveItem.scheduledStartTime.replace("預定發佈時間：", "").replace("Scheduled for ", ""),
                                                    color = Color.Gray,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "切換頻道",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onPreviousChannel,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("上一台")
                        }

                        Button(
                            onClick = onNextChannel,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
                        ) {
                            Text("下一台")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }

                    if (!isPermanentEmbedRestriction) {
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { launchOfficialUrl(context, currentChannel.officialUrl) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1E88E5)),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E88E5)))
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "開啟電視台官方觀看網頁", fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Temporary Network Error State Banner
        if (errorMessage != null && !isPermanentEmbedRestriction) {
            errorMessage?.let { errorText ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    color = Color(0xEE1E1E1E),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorText,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { launchOfficialUrl(context, currentChannel.officialUrl) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("開啟官方觀看", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = {
                                    retryTrigger++
                                    onRetry(currentChannel)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("重試", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Collapsible Side Channel Drawer
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp),
                color = Color(0xEE121212)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "頻道選單",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { isDrawerOpen = false }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "關閉選單", tint = Color.White)
                        }
                    }

                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(channels, key = { it.id }) { ch ->
                            val isSelected = ch.id == currentChannel.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onChannelSelect(ch)
                                        isDrawerOpen = false
                                    }
                                    .focusable(),
                                color = if (isSelected) Color(0xFF1E88E5) else Color(0xFF222222)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (ch.number > 0) "CH${ch.number} ${ch.name}" else ch.name,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = ch.category,
                                            color = Color.LightGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Text(
                                            text = "播放中",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerCore(
    currentChannel: Channel,
    activeSource: com.taiwanfreetv.app.data.model.Source,
    retryTrigger: Int,
    onLoadingChanged: (Boolean) -> Unit,
    onError: (Int, String) -> Unit,
    onEnded: () -> Unit = {}
) {
    key(currentChannel.id, activeSource, retryTrigger) {
        when (activeSource.type) {
            StreamType.HLS -> {
                if (!activeSource.streamUrl.isNullOrEmpty()) {
                    HlsPlayerComponent(
                        streamUrl = activeSource.streamUrl,
                        onError = { err -> onError(-1, err) },
                        modifier = Modifier.fillMaxSize()
                    )
                    onLoadingChanged(false)
                } else {
                    onError(-1, "頻道缺少有效的 HLS 直播網址")
                }
            }
            StreamType.EXTERNAL_WEB, StreamType.YOUTUBE_IFRAME -> {
                onLoadingChanged(false)
                val fallbackChannel = currentChannel.copy(officialUrl = activeSource.officialUrl ?: currentChannel.officialUrl)
                ExternalFallbackComponent(
                    channel = fallbackChannel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun PlayerControlsOverlayLandscape(
    currentChannel: Channel,
    favoriteIds: Set<String>,
    onBackClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleDrawer: () -> Unit,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x88000000))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(Color(0x99000000), CircleShape)
                    .focusable()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回首頁",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                modifier = Modifier.weight(1f),
                color = Color(0x99000000),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (currentChannel.number > 0) "CH${currentChannel.number} ${currentChannel.name}" else currentChannel.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            val isFav = favoriteIds.contains(currentChannel.id)
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .background(Color(0x99000000), CircleShape)
                    .focusable()
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "收藏頻道",
                    tint = if (isFav) Color.Red else Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onToggleDrawer,
                modifier = Modifier
                    .background(Color(0x99000000), CircleShape)
                    .focusable()
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "頻道選單",
                    tint = Color.White
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPreviousChannel,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0x88000000), CircleShape)
                    .focusable()
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "上一台",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = onNextChannel,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0x88000000), CircleShape)
                    .focusable()
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "下一台",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
