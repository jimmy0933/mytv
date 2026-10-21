# Channel Verification Report (Post-YouTube Removal)

## 驗證時間與環境
- **時間**: 2026-10-21
- **架構**: Android Media3 (ExoPlayer) 原生播放

## 驗證頻道 (連續 10 分鐘實播測試)

| 頻道名稱 | 替換/新增來源類型 | 測試結果 | 備註 |
| --- | --- | --- | --- |
| NHK World-Japan | `HLS` | 通過 | 官方直播源，畫面、聲音正常 |
| 公視主頻道 (改為公視3台) | `HLS` | 通過 | 由於原主頻來源失效，暫時以有效之 PTS3 來源備援，播放穩定 |
| TVBS Asia (替代TVBS新聞) | `HLS` | 通過 | 測試源可成功載入與播放 |
| GOOD TV 綜合台 (替代大愛) | `HLS` | 通過 | 測試源可成功載入 |

## 待解決 / 尚無可用來源 (轉為 WEB_ONLY 或 NO_SOURCE)
由於多數台灣新聞台 (例如：台視、中視、華視、東森、三立、中天、非凡) 先前完全依賴 YouTube 直播，在全面移除 YouTube 播放器及相關解析邏輯後，經查驗目前網路上無直接公開授權之純 HLS/DASH 直播源 (或者皆帶有時效性 Token/Geo-block)。
因此，這些頻道將被保留在收藏與清單中，但播放狀態標註為「僅官網」(WEB_ONLY) 或「尚無可用來源」(NO_SOURCE)，且點擊後會顯示提示並提供按鈕引導使用者前往外部官方網頁觀看。

- **快速切台 / 背景返回**: 確認在 PlayerScreen 載入新的 HLS 來源時，Media3 ExoPlayer 能正確重置並緩衝新來源，不再與舊的 YouTubeIFramePlayer 衝突。
- **旋轉與斷網恢復**: Media3 內建支援背景旋轉重建時恢復播放狀態，在連線中斷時會觸發 `PlaybackException` 並依現有架構觸發重試機制。
