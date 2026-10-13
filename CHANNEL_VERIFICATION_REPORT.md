# 台灣免費第四台 - 頻道來源驗證報告 (Channel Verification Report)

## 一、驗證原則 (Verification Principles)
1. **官方合法來源**：僅整合電視台官方 YouTube 直播頻道或官方 HLS 串流。
2. **免費且無版權風險**：不使用需要付費訂閱的頻道來源，不使用盜版 IPTV 或未經授權之串流。
3. **播放相容性與降級**：
   - 支援內嵌之 YouTube 官方直播：以官方 IFrame 播放器播放。
   - 官方 HLS 串流：以 AndroidX Media3 ExoPlayer 播放。
   - 不允許內嵌之來源：提供降級按鈕，開啟官方網站觀看。

---

## 二、預設頻道清單與驗證結果 (Verified Default Channels)

| 頻道名稱 | 分類 | 串流類型 | 官方/來源網址 | 內嵌狀態 | 驗證結果 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **中視新聞台** | 新聞 | YouTube IFrame | `https://www.ctv.com.tw/` | 允許內嵌 | 正常播放 (24hr Live) |
| **TVBS新聞台** | 新聞 | YouTube IFrame | `https://news.tvbs.com.tw/` | 允許內嵌 | 正常播放 (24hr Live) |
| **華視新聞資訊台** | 新聞 | YouTube IFrame | `https://news.cts.com.tw/` | 允許內嵌 | 正常播放 (24hr Live) |
| **民視新聞台** | 新聞 | YouTube IFrame | `https://www.ftvnews.com.tw/` | 允許內嵌 | 正常播放 (24hr Live) |
| **台視新聞台** | 新聞 | YouTube IFrame | `https://news.ttv.com.tw/` | 允許內嵌 | 正常播放 (24hr Live) |
| **三立新聞台** | 新聞 | YouTube IFrame | `https://www.setn.com/` | 允許內嵌 | 正常播放 (24hr Live) |
| **東森新聞台** | 新聞 | YouTube IFrame | `https://news.ebc.net.tw/` | 允許內嵌 | 正常播放 (24hr Live) |
| **東森財經新聞台** | 財經 | YouTube IFrame | `https://fnc.ebc.net.tw/` | 允許內嵌 | 正常播放 |
| **公視主頻道** | 綜合 | YouTube IFrame | `https://www.pts.org.tw/` | 允許內嵌 | 正常播放 |
| **大愛電視台** | 綜合 | YouTube IFrame | `https://www.daai.tv/` | 允許內嵌 | 正常播放 (24hr Live) |
| **YOYO TV 幼兒台** | 兒童 | YouTube IFrame | `https://yoyo.ebc.net.tw/` | 允許內嵌 | 正常播放 |
| **官方 HLS 測試頻道** | 綜合 | HLS (Media3) | `https://mux.com/` | 允許內嵌 | 正常播放 (HLS Multi-bitrate) |
| **東森新聞 (網頁觀看)** | 新聞 | External Web | `https://news.ebc.net.tw/live` | 外部網頁 | 降級開啟官方頁面 |

---

## 三、合規聲明 (Compliance Declaration)
- 本應用程式未包含任何抓取 YouTube 底層 `.m3u8`/`.mpd` 之行為。
- YouTube 直播播放完全經由官方 API / HTML5 IFrame 進行，廣告呈現與原作者版權均獲完全保留。
- 當串流無法內嵌播放時，App 提供「開啟官方直播網頁」選項，遵循各電視台服務條款。
