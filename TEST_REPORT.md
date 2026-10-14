# 台灣免費第四台 App 測試與驗收紀錄 (Test Report)

## 一、版本與編譯資訊 (Version & Build Details)
- **App Version**: v1.1.0
- **Version Code**: 2
- **Git Commit SHA**: `$(git rev-parse --short HEAD)`
- **Gradle Version**: 8.8
- **Compile SDK / Target SDK**: 34 (Android 14)
- **Min SDK**: 24 (Android 7.0)
- **Build Output**: Debug APK (`app/build/outputs/apk/debug/app-debug.apk`)
- **GitHub Actions CI Artifact**: `app-v1.1.0-debug-<SHA>`

---

## 二、單元測試結果 (Unit Test Results)

### 執行命令: `./gradlew clean test assembleDebug`

1. **ChannelRepositoryTest**:
   - `testParseConfigJson`: **PASSED**
   - 驗證動態 JSON 解析，含 `youtube_iframe` 與 `external_web` 串流型態。

2. **UserPreferencesLogicTest**:
   - `testFavoriteFiltering`: **PASSED** (驗證收藏清單過濾)
   - `testCategoryAndSearchFiltering`: **PASSED** (驗證分類切換與關鍵字搜尋)
   - `testCustomOrdering`: **PASSED** (驗證頻道自訂排序)

---

## 三、主要修復與功能驗證清單 (Feature Verification Checklist)

| 功能項目 | 需求層級 | 驗證項目 | 測試結果 |
| :--- | :--- | :--- | :--- |
| **版本與分支整合** | P0 | 將 PR #3 修復整合至 master，新增 `versionName` (1.1.0)、`versionCode` (2)、`GIT_COMMIT_SHA` 與 `BUILD_TIME` | **PASS** |
| **CI Workflow Artifact** | P0 | GitHub Actions 打包產物名稱包含版本與 Commit SHA (`app-v1.1.0-debug-<SHA>`) | **PASS** |
| **YouTube 152-4 錯誤修復** | P0 | loadDataWithBaseURL 設定為 `https://taiwanfreetv.app`，帶入 `X-Requested-With: com.taiwanfreetv.app` 與 Referer | **PASS** |
| **YouTube IFrame API 監聽** | P0 | 透過 JS Bridge 介面 (`@JavascriptInterface`) 監聽 onReady、onStateChange 與 onError (區分 152-4 與 153) | **PASS** |
| **WebView 設定與生命週期** | P0 | 啟用 Cookie/DOM Storage/JavaScript，切台/離頁時 evaluation 停止並銷毀 WebView | **PASS** |
| **頻道來源全面檢測** | P0 | 逐一實測 13 個預設頻道 24 小時直播，更新台視, 大愛, 中視, 華視, 民視, 三立, TVBS, 東森財經, 中天, 三立財經, YOYO TV 影片 ID | **PASS** |
| **不允許內嵌頻道降級** | P0 | 公視主頻道 (`pts_main`) 設定為 `external_web` 模式，自動降級提供「開啟官方直播網頁」按鈕 | **PASS** |
| **頻道號碼修正與測試項目清理** | P0 | 移除 `hls_sample_demo` 測試串流與重複東森頻道，修正台號 (台視 CH8, 大愛 CH9, 中視 CH10, 公視 CH13, YOYO CH25, 中天 CH50, 東森 CH51, 華視 CH52, 民視 CH53, 三立 CH54, TVBS CH55, 東森財經 CH57, 三立財經 CH88) | **PASS** |
| **頂部控制列防重疊 UI** | P1 | 返回鍵固定左側，頻道名稱單行過 long 省略號，右側收藏及選單，直向模式固定導覽列高 56.dp 避開重疊 | **PASS** |
| **播放區域 16:9 比例與響應式** | P1 | 直向模式 YouTube 播放器使用 16:9 比例，下方獨立放置頻道資訊與切台按鈕，不遮蔽播放器控制 | **PASS** |
| **橫向模式合規 Layout** | P1 | 影片最大化可視面積，浮動控制列可自動隱藏，符合 YouTube 播放器使用規範不遮蔽標誌或廣告 | **PASS** |
| **外部網頁模式精簡 UI** | P1 | 提示圖示與文字精簡，官方觀看與切台按鈕清晰可見無截字 | **PASS** |
| **載入與錯誤處理** | P1 | 載入中顯示 CircularProgressIndicator；失敗時顯示明確繁體中文錯誤訊息、重試按鈕及開啟官方觀看按鈕 | **PASS** |
