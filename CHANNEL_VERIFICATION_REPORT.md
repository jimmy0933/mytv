# 頻道播放驗證報告 (Channel Verification Report)

**驗證日期**：2026-03-31
**專案名稱**：TaiwanFreeTV (mytv)
**測試環境**：Android 13 / 14 實體測試與沙盒單元測試環境
**版本**：v1.2.0 (code 3)

---

## 一、預設頻道驗證矩陣

| 頻道號碼 | 頻道名稱 | 分類 | 官方來源網址 | 是否免費 | 即時直播 | 台灣可看 | 允許第三方內嵌 | 播放狀態 | 實測結果與處理方式 |
| :--- | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **CH08** | **台視新聞台** | 新聞 | https://news.ttv.com.tw/ | 是 | 是 | 是 | 否 | `EMBED_DISABLED` | YouTube 內嵌受限，降級為「官方平台觀看」模式 |
| **CH09** | **大愛電視台** | 綜合 | https://www.daai.tv/ | 是 | 是 | 是 | 否 | `EMBED_DISABLED` | YouTube 內嵌受限，降級為「官方平台觀看」模式 |
| **CH10** | **中視新聞台** | 新聞 | https://www.ctv.com.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`TCnaIE_SAtM`) 內嵌實測可播放 |
| **CH13** | **公視主頻道** | 綜合 | https://www.pts.org.tw/ | 是 | 是 | 是 | 否 | `EMBED_DISABLED` | 公視限制內嵌播放，降級為「官方平台觀看」模式 |
| **CH14** | **NHK WORLD-JAPAN** | 國際新聞 | https://www3.nhk.or.jp/nhkworld/en/live_tv/ | 是 | 是 | 是 | 否 | `EMBED_DISABLED` | 無第三方 App 內嵌授權，降級為「官方平台觀看」模式 |
| **CH25** | **YOYO TV 幼兒台** | 兒童 | https://yoyo.ebc.net.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`BjY_jXI15ks`) 內嵌實測可播放 |
| **CH50** | **中天新聞台** | 新聞 | https://gotv.ctitv.com.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`vr3XyVCR4T0`) 內嵌實測可播放 |
| **CH51** | **東森新聞台** | 新聞 | https://news.ebc.net.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`V1p33hqPrUk`) 內嵌實測可播放 |
| **CH52** | **華視新聞資訊台** | 新聞 | https://news.cts.com.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`wM0g8EoUZ_E`) 內嵌實測可播放 |
| **CH53** | **民視新聞台** | 新聞 | https://www.ftvnews.com.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`ylYJSBUgaMA`) 內嵌實測可播放 |
| **CH54** | **三立新聞台** | 新聞 | https://www.setn.com/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`yeYC0mbSIOo`) 內嵌實測可播放 |
| **CH55** | **TVBS新聞台** | 新聞 | https://news.tvbs.com.tw/ | 是 | 是 | 是 | 否 | `EMBED_DISABLED` | YouTube 內嵌受限，降級為「官方平台觀看」模式 |
| **CH57** | **東森財經新聞台** | 財經 | https://fnc.ebc.net.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`AEBeWMM1atA`) 內嵌實測可播放 |
| **CH88** | **三立財經iNEWS** | 財經 | https://www.setn.com/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`pF507BLtbqU`) 內嵌實測可播放 |

---

## 二、優先五大頻道專案調查報告

1. **NHK WORLD-JAPAN** (CH14)：官方免費英語國際新聞。無公開免認證 HLS 或第三方 App 內嵌授權。確定標記為 `EMBED_DISABLED`，引導至官方網站/應用程式觀看。
2. **公視主頻道** (CH13)：公視官方直播限制第三方 App 內嵌（YouTube 傳回 101/150 錯誤）。確定標記為 `EMBED_DISABLED`。
3. **大愛電視台** (CH09)：大愛電視 YouTube 直播在行動裝置 WebView 內嵌遭遇權限限制。確定標記為 `EMBED_DISABLED`。
4. **台視新聞台** (CH08)：台視新聞台 YouTube 直播受內嵌權限限制。確定標記為 `EMBED_DISABLED`。
5. **TVBS新聞台** (CH55)：TVBS 新聞台 YouTube 直播禁止第三方 App 內嵌。確定標記為 `EMBED_DISABLED`。

---

## 三、播放狀態與 UI 體驗改善說明

1. **狀態真實分類**：
   - `PLAYABLE`（App 內可播放）：可在 App 內透過內嵌播放器直接觀看影音。
   - `EMBED_DISABLED`（官方平台觀看）：官方限制第三方內嵌，清楚標示不誤導使用者，點擊直接開啟官方頁面。
2. **連續測試紀錄**：
   - 對所有標示 `PLAYABLE` 之頻道進行至少 60 秒持續播放與連續切台 10 次測試，確定無崩潰或卡死現象。
   - 對標示 `EMBED_DISABLED` 之頻道確定正確引導降級至官方平台，無重複按鈕或偽裝內嵌直播問題。
