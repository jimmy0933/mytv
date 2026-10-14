# 台灣免費第四台 - 頻道來源驗證報告 (Channel Verification Report)

## 一、驗證原則 (Verification Principles)
1. **官方合法來源**：僅整合電視台官方 YouTube 直播頻道或官方 HLS 串流。
2. **免費且無版權風險**：不使用需要付費訂閱的頻道來源，不使用盜版 IPTV 或未經授權之串流。
3. **播放相容性與降級**：
   - 支援內嵌之 YouTube 官方直播：以官方 IFrame 播放器播放。
   - 不允許內嵌之來源：提供降級按鈕，開啟官方網站/網頁觀看。

---

## 二、預設頻道清單與驗證結果 (Verified Default Channels)

| 頻道號碼 | 頻道名稱 | 分類 | 串流類型 | YouTube Video ID / 官方網址 | 內嵌狀態 | 驗證結果 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **CH08** | **台視新聞台** | 新聞 | YouTube IFrame | `9iRAqBMakXY` | 允許內嵌 | 正常播放 (24hr Live) |
| **CH09** | **大愛電視台** | 綜合 | YouTube IFrame | `pM-1ytfQhos` | 允許內嵌 | 正常播放 (24hr Live) |
| **CH10** | **中視新聞台** | 新聞 | YouTube IFrame | `TCnaIE_SAtM` | 允許內嵌 | 正常播放 (24hr Live) |
| **CH13** | **公視主頻道** | 綜合 | External Web | `https://www.youtube.com/watch?v=C6gYqSHLRw4` | 官方限制內嵌 | 降級開啟官方頁面 |
| **CH25** | **YOYO TV 幼兒台** | 兒童 | YouTube IFrame | `BjY_jXI15ks` | 允許內嵌 | 正常播放 (24hr Live) |
| **CH50** | **中天新聞台** | 新聞 | YouTube IFrame | `vr3XyVCR4T0` | 允許內嵌 | 正常播放 (24hr Live) |
| **CH51** | **東森新聞台** | 新聞 | YouTube IFrame | `V1p33hqPrUk` | 允許內嵌 | 正常播放 (24hr Live) |
| **CH52** | **華視新聞資訊台** | 新聞 | YouTube IFrame | `wM0g8EoUZ_E` | 允許內嵌 | 正常播放 (24hr Live) |
| **CH53** | **民視新聞台** | 新聞 | YouTube IFrame | `ylYJSBUgaMA` | 允許內嵌 | 正常播放 (24hr Live) |
| **CH54** | **三立新聞台** | 新聞 | YouTube IFrame | `yeYC0mbSIOo` | 允許內嵌 | 正常播放 (24hr Live) |
| **CH55** | **TVBS新聞台** | 新聞 | YouTube IFrame | `2mCSYvcfhtc` | 允許內嵌 | 正常播放 (24hr Live) |
| **CH57** | **東森財經新聞台** | 財經 | YouTube IFrame | `AEBeWMM1atA` | 允許內嵌 | 正常播放 (24hr Live) |
| **CH88** | **三立財經iNEWS** | 財經 | YouTube IFrame | `pF507BLtbqU` | 允許內嵌 | 正常播放 (24hr Live) |

---

## 三、合規與修正紀錄 (Compliance Declaration & Audit Log)
- 移除舊版失效與過期之 Video ID (例如原台視 `xL0ch83pjK8`、三立 `41yP4L9PzM8`、東森 `R2aiS3fL56g`)。
- 將非 24 小時直播之一般短影片更換為電視台官方實時 24 小時直播串流。
- 移除正式頻道清單中之測試串流 (`hls_sample_demo`)。
- 修正頻道號碼重複與錯誤問題 (中視 CH10, 東森 CH51, 華視 CH52, 民視 CH53, 三立 CH54, TVBS CH55, 東森財經 CH57)。
- 當串流無法內嵌播放時 (如公視)，降級提供「開啟官方直播網頁」選項，完全遵循官方服務條款。
