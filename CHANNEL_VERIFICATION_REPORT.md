# 頻道播放驗證報告 (Channel Verification Report)

**驗證日期**：2026-03-31
**專案名稱**：TaiwanFreeTV (mytv)
**測試環境**：Android 13 / 14 實體測試與沙盒單元測試環境
**版本**：v1.3.0

---

## 一、TVBS 直播專案優先調查報告 (P0)

### 1. 官方網路直播來源確認
- **官方直播網頁**：`https://news.tvbs.com.tw/playlists`
- **YouTube 24小時直播 ID**：`m_dhMSvUCIc` (`🔴LIVE：TVBS NEWS網路獨家新聞24小時直播`)
- **其他官方介面**：TVBS 新聞網官方主頁 (`https://news.tvbs.com.tw/`) 及 TVBS 新聞 App。

### 2. Android WebView 完整瀏覽實測
- 在 Android WebView 中載入 `https://news.tvbs.com.tw/` 頁面正常，排版及新聞影音均可順利瀏覽。

### 3. 第三方嵌入與權限調查
- TVBS 官方頻道將 YouTube 直播開啟了 **「禁止第三方嵌入」** 及 **「限制特定網域 (tvbs.com.tw)」** 權限。
- 在第三方 Android App 或一般行動 WebView 中載入 YouTube IFrame Player 時，會觸發 YouTube JavaScript API `onError` 錯誤碼 **101 / 150** (Embed restricted by video owner)。

### 4. 授權 HLS 與合規聲明
- TVBS 官方未公開發布無須認證的 HLS 串流 URL (.m3u8)。
- 遵守本專案「禁止破解播放限制或抽取未授權串流」之原則，不使用未授權之第三方抓包串流。

### 5. 處理結果與備援機制
- TVBS 新聞台標記為 `EMBED_DISABLED` (`external_web` 模式)。
- 使用者點選後提供官方網頁連結直接瀏覽，兼顧播放穩定度與合規性。

---

## 二、日本直播頻道與節目專區驗證矩陣 (P1)

### 1. 日本直播頻道

| 頻道號碼 | 頻道名稱 | 分類 | 官方來源網址 | 免費 | 直播 | 台灣可看 | 允許內嵌 | 狀態 | 實測與處理說明 |
| :--- | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **CH101** | **TBS NEWS DIG** | 日本直播 | https://newsdig.tbs.co.jp/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`Anr15FA9OCI`) 內嵌實測可播放超過 60 秒 |
| **CH102** | **日テレNEWS24** | 日本直播 | https://news.ntv.co.jp/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`t9kwjZBLI-A`) 內嵌實測可播放超過 60 秒 |
| **CH103** | **ウェザーニュースLiVE** | 日本直播 | https://weathernews.jp/s/live/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`-PAdw9eF_6Q`) 內嵌實測可播放超過 60 秒 |
| **CH14** | **NHK WORLD-JAPAN** | 國際新聞 | https://www3.nhk.or.jp/nhkworld/en/live_tv/ | 是 | 是 | 是 | 否 | `EMBED_DISABLED` | 無第三方 App 內嵌授權，降級為「官方平台觀看」模式 |

### 2. 日本節目與專區 (VOD 隨選影片)

| 頻道號碼 | 節目/頻道名稱 | 分類 | 類型 | 官方來源 | 免費 | 允許內嵌 | 狀態 | 說明 |
| :--- | :--- | :--- | :--- | :--- | :---: | :---: | :---: | :--- |
| **CH201** | **おとな旅あるき旅** | 日本旅遊、美食 | 隨選影片 | 電視大阪 YouTube | 是 | 是 | `PLAYABLE` | 電視大阪官方節目 (`v0XWJjGSqhk`)，標註為 VOD 隨選影片 |
| **CH202** | **NHK WORLD 旅遊文化** | 日本旅遊、美食 | 隨選影片 | NHK WORLD 官網 | 是 | 否 | `EMBED_DISABLED` | 引導至 NHK 官方隨選專區觀看 |
| **CH301** | **もしものマネー道** | 日本綜藝 | 隨選影片 | 電視大阪 YouTube | 是 | 是 | `PLAYABLE` | 電視大阪官方節目 (`Ruf3mplz2J4`)，標註為 VOD 隨選影片 |
| **CH401** | **木棉花正版動畫專區** | 日本動畫 | 隨選影片 | 木棉花 YouTube | 是 | 是 | `PLAYABLE` | 木棉花官方免費正版動畫專區 |
| **CH402** | **Ani-One 正版動畫專區** | 日本動畫 | 隨選影片 | Ani-One YouTube | 是 | 是 | `PLAYABLE` | Ani-One 官方免費正版動畫專區 |

---

## 三、播放來源與管理機制

1. **可擴充 JSON 結構 (`channels.json` version 5)**：
   - 每個頻道均包含：`id`, `name`, `category`, `type` (`youtube_iframe` / `hls` / `external_web`), `official_url`, `embeddable`, `status` (`PLAYABLE` / `EMBED_DISABLED`), `status_message`, `last_verified`, `description`。
2. **直播與隨選嚴格區分**：
   - 所有錄影節目均在描述與標題註明【隨選影片】，絕不誤導為直播。
