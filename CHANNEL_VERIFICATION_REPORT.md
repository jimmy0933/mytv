# 頻道播放驗證報告 (Channel Verification Report)

**驗證日期**：2026-03-31
**專案名稱**：TaiwanFreeTV (mytv)
**狀態**：全面完成 (P0, P1, P2)

---

## 一、預設頻道驗證矩陣

| 頻道號碼 | 頻道名稱 | 分類 | 官方來源網址 | 是否免費 | 即時直播 | 台灣可看 | 允許第三方內嵌 | 播放狀態 | 實實測試結果與處理方式 |
| :--- | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **CH08** | **台視新聞台** | 新聞 | https://news.ttv.com.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`9iRAqBMakXY`) 內嵌測試成功 |
| **CH09** | **大愛電視台** | 綜合 | https://www.daai.tv/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`pM-1ytfQhos`) 內嵌測試成功 |
| **CH10** | **中視新聞台** | 新聞 | https://www.ctv.com.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`TCnaIE_SAtM`) 內嵌測試成功 |
| **CH13** | **公視主頻道** | 綜合 | https://www.youtube.com/watch?v=C6gYqSHLRw4 | 是 | 是 | 是 | 否 | `EMBED_DISABLED` | 官方限制內嵌，UI 簡潔提示並提供直達官方頁面按鈕 |
| **CH14** | **NHK WORLD-JAPAN** | 國際新聞 | https://www3.nhk.or.jp/nhkworld/en/live_tv/ | 是 | 是 | 是 | 否 (無授權) | `EMBED_DISABLED` | 新增頻道。官方英語國際頻道，無 App 內嵌授權，引導至官方直播頁 |
| **CH25** | **YOYO TV 幼兒台** | 兒童 | https://yoyo.ebc.net.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`BjY_jXI15ks`) 內嵌測試成功 |
| **CH50** | **中天新聞台** | 新聞 | https://gotv.ctitv.com.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`vr3XyVCR4T0`) 內嵌測試成功 |
| **CH51** | **東森新聞台** | 新聞 | https://news.ebc.net.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`V1p33hqPrUk`) 內嵌測試成功 |
| **CH52** | **華視新聞資訊台** | 新聞 | https://news.cts.com.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`wM0g8EoUZ_E`) 內嵌測試成功 |
| **CH53** | **民視新聞台** | 新聞 | https://www.ftvnews.com.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`ylYJSBUgaMA`) 內嵌測試成功 |
| **CH54** | **三立新聞台** | 新聞 | https://www.setn.com/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`yeYC0mbSIOo`) 內嵌測試成功 |
| **CH55** | **TVBS新聞台** | 新聞 | https://news.tvbs.com.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`2mCSYvcfhtc`) 內嵌測試成功 |
| **CH57** | **東森財經新聞台** | 財經 | https://fnc.ebc.net.tw/ | 是 | 是 | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`AEBeWMM1atA`) 內嵌測試成功 |
| **CH88** | **三立財經iNEWS** | 財經 | https://www.setn.com/ | 是 | Es | 是 | 是 | `PLAYABLE` | YouTube 官方直播 (`pF507BLtbqU`) 內嵌測試成功 |

---

## 二、CNN 新聞來源調查報告

1. **調查目標**：CNN Headlines, CNN FAST / 免費直播來源。
2. **台灣地區可用性與授權評估**：
   - CNN International 在全球均屬付費 / 訂閱頻道（電視台系統或授權 OTT）。
   - CNN Headlines 及部分短播專區雖然會在 YouTube 或 FAST 平台出現，但無開放第三方 App 內嵌授權，且串流連結網址變動頻繁或受地理權限與認證鎖定。
3. **合規決策**：
   - 為嚴格遵守合規性與「不得將 CNN Headlines 標示為 CNN International」之要求，且目前無合法免費可內嵌直播串流，因此列為**候選頻道**，不放入正式免費直播清單中，確保不使用未授權來源。

---

## 三、播放狀態管理與 UI 體驗改善

1. **頻道狀態模型**：
   - 新增 `PlaybackStatus` 屬性：`PLAYABLE` (可播放)、`EMBED_DISABLED` (禁止內嵌)、`GEO_RESTRICTED` (地區限制)、`SOURCE_INVALID` (來源失效)、`UNVERIFIED` (尚未驗證)。
   - 包含 `last_verified` (最近驗證時間) 與 `status_message` 說明。
2. **UI 優化**：
   - 頻道列表卡片上標示狀態顏色 Tag，使用者可一目瞭然。
   - 當播放器遇到禁止內嵌 (101/150 錯誤碼或 `EMBED_DISABLED` 類型) 時，不再顯示黑色原生錯誤頁面，改以簡潔質感的提示「此頻道需透過官方平台觀看」，並提供高亮「開啟官方直播網頁」按鈕。
   - 永久限制不進行無限重試，暫時網路異常則提供重試機制。
