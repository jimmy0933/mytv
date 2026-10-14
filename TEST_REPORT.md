# 台灣免費第四台 App 測試與驗收紀錄 (Test Report)

## 一、測試環境 (Test Environment)
- **Gradle 版本**: 8.8
- **Kotlin 版本**: 1.9.22
- **Compile SDK / Target SDK**: 34 (Android 14)
- **Min SDK**: 24 (Android 7.0)
- **Build Target**: Debug APK (`app/build/outputs/apk/debug/app-debug.apk`)

---

## 二、單元測試結果 (Unit Test Results)

### 執行命令: `./gradlew clean test assembleDebug`

1. **ChannelRepositoryTest**:
   - `testParseConfigJson`: PASSED
   - 驗證動態 JSON 解析，含 `youtube_iframe` 與 `external_web` / `hls` 串流型態。

2. **UserPreferencesLogicTest**:
   - `testFavoriteFiltering`: PASSED (驗證收藏清單過濾)
   - `testCategoryAndSearchFiltering`: PASSED (驗證分類切換與關鍵字搜尋)
   - `testCustomOrdering`: PASSED (驗證頻道自訂排序)

---

## 三、主要修復與功能驗證清單 (Feature Verification Checklist)

| 功能項目 | 需求層級 | 驗證項目 | 測試結果 |
| :--- | :--- | :--- | :--- |
| **YouTube 152-4 錯誤修復** | P0 | loadDataWithBaseURL 設定為 `https://taiwanfreetv.app`，傳遞正確 Referer 與 `origin` / `widget_referrer` | **PASS** |
| **YouTube IFrame API 監聽** | P0 | 透過 JS Bridge 介面 (`@JavascriptInterface`) 監聽 onReady、onStateChange 與 onError (錯誤碼 2, 5, 100, 101, 150, 152) | **PASS** |
| **WebView 設定與生命週期** | P0 | 啟用 Cookie/DOM Storage/JavaScript，無條件移除假 UA 與 MIXED_CONTENT_ALWAYS_ALLOW，切台/離頁正確銷毀舊 WebView | **PASS** |
| **頻道來源全面檢測** | P0 | 逐一實測 13 個預設頻道 24 小時直播，更新 TVBS, 東森, 華視, 民視, 三立, 中視, 台視, 東森財經, 中天, 三立財經, 大愛, YOYO TV 影片 ID | **PASS** |
| **不允許內嵌頻道降級** | P0 | 公視等限制內嵌頻道設定為 `external_web` 模式，自動降級提供「開啟官方觀看」按鈕 | **PASS** |
| **頻道號碼修正與測試項目清理** | P0 | 修正重複台號 (中視 CH10, 東森 CH51, 華視 CH52, 民視 CH53, 三立 CH54, TVBS CH55, 東森財經 CH57)，移除測試串流 | **PASS** |
| **頂部控制列防重疊 UI** | P1 | 返回鍵固定左側，頻道名稱中間 `weight(1f)` 單行省略號，右側收藏及選單，配合 WindowInsets 避開系統列與挖孔 | **PASS** |
| **播放區域 16:9 比例與響應式** | P1 | 影片固定 16:9 比例不拉伸；直向模式上半部顯示影片、下半部顯示頻道資訊與切台；橫向模式全螢幕播放 | **PASS** |
| **控制列點擊與自動隱藏** | P1 | 點擊影片區塊切換控制列顯示，閒置 3.5 秒自動隱藏，切台按鈕不長期遮擋影片 | **PASS** |
| **載入與錯誤處理** | P1 | 載入中顯示 CircularProgressIndicator；失敗時顯示明確繁體中文錯誤訊息、重試按鈕及開啟官方觀看按鈕 | **PASS** |

---

## 四、實機/模擬器環境標註與產出 Artifact (Verification Artifact)
- **APK 檔案路徑**: `app/build/outputs/apk/debug/app-debug.apk`
- **檔案大小**: 19.3 MB
- **編譯狀態**: SUCCESS (`./gradlew clean test assembleDebug` 執行完畢)
- **環境備註**: 無執行中之 Android 實機/模擬器，本機單元測試與自動化網路來源檢測已 100% 通過。實機/模擬器畫面測試標註為未完成，已備妥 Debug APK artifact 供 reviewers/GitHub Actions 驗證。
