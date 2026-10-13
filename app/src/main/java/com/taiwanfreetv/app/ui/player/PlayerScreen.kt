package com.taiwanfreetv.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taiwanfreetv.app.data.model.Channel
import com.taiwanfreetv.app.data.model.StreamType

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
    modifier: Modifier = Modifier
) {
    var isDrawerOpen by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }

    // Reset error when channel changes or retry is triggered
    LaunchedEffect(currentChannel.id, retryTrigger) {
        errorMessage = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Player Dispatcher based on StreamType
        key(currentChannel.id, retryTrigger) {
            when (currentChannel.type) {
                StreamType.YOUTUBE_IFRAME -> {
                    if (!currentChannel.youtubeVideoId.isNullOrEmpty()) {
                        YouTubeIFramePlayer(
                            videoId = currentChannel.youtubeVideoId,
                            onError = { err -> errorMessage = err },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        errorMessage = "頻道缺少有效的 YouTube 影片 ID"
                    }
                }
                StreamType.HLS -> {
                    if (!currentChannel.streamUrl.isNullOrEmpty()) {
                        HlsPlayerComponent(
                            streamUrl = currentChannel.streamUrl,
                            onError = { err -> errorMessage = err },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        errorMessage = "頻道缺少有效的 HLS 直播網址"
                    }
                }
                StreamType.EXTERNAL_WEB -> {
                    ExternalFallbackComponent(
                        channel = currentChannel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Overlay Controller Bar (Top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .background(Color(0x99000000), CircleShape)
                        .focusable()
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回首頁",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    color = Color(0x99000000),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (currentChannel.number > 0) "CH${currentChannel.number} ${currentChannel.name}" else currentChannel.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Favorite Button
                val isFav = favoriteIds.contains(currentChannel.id)
                IconButton(
                    onClick = { onToggleFavorite(currentChannel) },
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

                Spacer(modifier = Modifier.width(10.dp))

                // Toggle Channel List Drawer
                IconButton(
                    onClick = { isDrawerOpen = !isDrawerOpen },
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
        }

        // Quick Channel Switcher Overlay (Left/Right Arrows)
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
                    .size(48.dp)
                    .background(Color(0x88000000), CircleShape)
                    .focusable()
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "上一台",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = onNextChannel,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0x88000000), CircleShape)
                    .focusable()
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "下一台",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Error State & Retry Banner
        errorMessage?.let { errorText ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp, start = 20.dp, end = 20.dp),
                color = Color(0xDD000000),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFB300)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorText,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { retryTrigger++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("重試", fontSize = 12.sp)
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
                    Text(
                        text = "頻道選單",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    HorizontalDivider(color = Color.DarkGray)

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 12.dp)
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
                                            text = ch.name,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
