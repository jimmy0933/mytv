1. **Fix Duplicate Channels & Validation (P0)**
   - `channels.json` has duplicates for `ustv_news` and `ustv_business`. I will remove duplicates, ensuring only valid entries remain with `category` = `台灣財經`.
   - Update `ChannelRepository.kt` and `HomeScreen.kt`/`PlayerScreen.kt` to validate remote configuration (e.g. check for duplicate IDs, required fields) and fallback to local config if invalid.

2. **Multi-source Fallback Implementation (P1)**
   - Modify `Models.kt` to define a `Source` class (`type`, `youtubeVideoId`, `youtubeChannelId`, `streamUrl`, `officialUrl`) and add `val sources: List<Source>` to `Channel`.
   - Modify `channels.json` to include `sources` for all channels, and specifically investigate and add correct HLS/YouTube sources for CTI, TVBS, 非凡新聞, and 非凡商業. TVBS has `embed_disabled` so its default is `external_web`.
   - In `PlayerScreen.kt` and `MainActivity.kt`, add logic to attempt the next source in the list if the current one fails (e.g., when `YouTubeIFramePlayer` or `HlsPlayerComponent` signals an error). Stop if all sources are exhausted, showing `external_web` as the final fallback.
   - External Web playback does *not* mean successful app playback. If it's `external_web`, it should just provide the official link without trying to 'play' it.

3. **Improve YouTube Live Resolver Logic (P1)**
   - Update `YouTubeLiveResolver.kt` to use `ytInitialPlayerResponse` and `videoDetails.isLiveContent` to properly check live status.
   - Check `playabilityStatus` for `status` ("ERROR", "LOGIN_REQUIRED" (Bot Check -> UNKNOWN)), `reason`, and region restrictions. Map to `EMBED_DISABLED`, `GEO_RESTRICTED`, `UNKNOWN`, `LIVE`, `VOD`, `OFFLINE`, `UPCOMING`.
   - Stop using naive regex searches across the whole HTML file (e.g., `contains("isLiveNow")`) to avoid being misled by recommended videos.
   - Only return LIVE if `videoDetails.isLiveContent` is true, or similar robust structural checks succeed.
   - For `UNKNOWN`, do not permanently disable the channel.

4. **Fix Back Button Resolution Cancellation (P1)**
   - In `MainActivity.kt`, `onBackPressed()` cancels `resolveJob` and increments `requestGeneration`, but the UI back button `onBackClick` only sets `currentScreen = Screen.Home` without canceling the job.
   - Update `onBackClick` to call a unified back handler that cancels `resolveJob` and increments `requestGeneration`.

5. **TTL and Stream Expiration Updates (P1)**
   - Add TTL (e.g., 5 minutes for LIVE, 1 minute for RESOLVING) check in `MainActivity.kt`'s lifecycle (e.g., `onResume`) or periodically.
   - In `YouTubeIFramePlayer.kt`, on ENDED, trigger a callback to invalidate the cache and force `playChannel(channel, forceRefresh = true)`.

6. **HLS Player View Rebinding (P1)**
   - In `HlsPlayerComponent.kt`, `AndroidView`'s `update` block is missing. Use `update = { it.player = exoPlayer }` to rebind the updated player instance to the `PlayerView` when `streamUrl` changes.

7. **Fix '台灣財經' Category (P2)**
   - Already handled via JSON fix, adding '台灣財經' to `categories` and changing '財經' channels to '台灣財經'.

8. **Pre-commit and Tests**
   - Execute the pre-commit step to ensure formatting, CI, and test validations are successful.

9. **Submit**
   - Push to master branch and submit.
