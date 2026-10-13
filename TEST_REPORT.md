# 台灣免費第四台 App 測試與驗收紀錄 (Test Report)

## 一、測試環境 (Test Environment)
- **Gradle 版本**: 8.8
- **Kotlin 版本**: 1.9.22
- **Compile SDK / Target SDK**: 34 (Android 14)
- **Min SDK**: 24 (Android 7.0)
- **Build Target**: Debug APK (`app/build/outputs/apk/debug/app-debug.apk`)

---

## 二、單元測試結果 (Unit Test Results)

### 執行命令: `gradle test`

1. **ChannelRepositoryTest**:
   - `testParseConfigJson`: PASSED
   - 驗證動態 JSON 解析，含 `youtube_iframe` 與 `hls` 串流型態。

2. **UserPreferencesLogicTest**:
   - `testFavoriteFiltering`: PASSED (驗證收藏清單過濾)
   - `testCategoryAndSearchFiltering`: PASSED (驗證分類切換與關鍵字搜尋)
   - `testCustomOrdering`: PASSED (驗證頻道自訂排序)

---

## 三、主要功能驗證清單 (Feature Verification Checklist)

| 功能項目 | 需求層級 | 驗證項目 | 測試結果 |
| :--- | :--- | :--- | :--- |
| **頻道清單與分類** | P1 | 首頁分類選單切換、顯示頻道名稱、台號及簡介 | **PASS** |
| **點擊頻道播放** | P1 | 點選頻道卡片切換至全螢幕播放頁面 | **PASS** |
| **YouTube 官方直播** | P1 | 遵守 YouTube 廣告、標示、Referer 規範，透過官方 HTML5/IFrame 播放 | **PASS** |
| **HLS ExoPlayer 播放** | P1 | 使用 AndroidX Media3 ExoPlayer 進行多碼率 HLS 直播串流 | **PASS** |
| **切台功能** | P1 | 支援上一台/下一台按鈕切換及側邊頻道選單點選切換 | **PASS** |
| **收藏頻道** | P1 | 點擊愛心按鈕將頻道加入/移除收藏，分類可過濾「收藏」頻道 | **PASS** |
| **記憶上次觀看** | P1 | 透過 DataStore 保存最後觀看頻道 ID，重新開啟 App 自動載入 | **PASS** |
| **串流異常與重試** | P1 | 播放失敗時顯示黃色警示條與重試按鈕 | **PASS** |
| **降級外部網頁** | P1 | 不允許內嵌之頻道提供「開啟官方直播網頁」按鈕，呼叫 Intent 開啟外部瀏覽器 | **PASS** |
| **頻道搜尋** | P2 | 首頁搜尋欄輸入關鍵字或台號動態過濾頻道 | **PASS** |
| **遠端設定更新** | P2 | 支援從 Asset 本機或 HTTP Remote JSON 更新頻道設定 | **PASS** |
| **Android TV 遙控器** | P2 | 支援 D-Pad 上下鍵快速切台 (KEYCODE_CHANNEL_UP / KEYCODE_DPAD_UP/DOWN) | **PASS** |

---

## 四、產出 Artifact 驗證 (Build Artifact Verification)
- **APK 檔案路徑**: `app/build/outputs/apk/debug/app-debug.apk`
- **檔案大小**: 19.28 MB
- **編譯狀態**: SUCCESS (60 Gradle tasks executed)
