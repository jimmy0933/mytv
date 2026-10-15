# 測試報告 (Test Report)

**專案名稱**：TaiwanFreeTV (mytv)
**測試日期**：2026-03-31
**測試人員**：Jules (Automated Agent)
**版本資訊**：v1.3.0 (Gradle version bump & channels.json v5)

---

## 一、測試執行摘要

| 測試項目 | 命令 / 工具 | 結果 | 備註 |
| :--- | :--- | :---: | :--- |
| **單元測試 (Unit Tests)** | `./gradlew test` | **PASS** | 包含 JSON 解析與 PlaybackStatus / StreamType 欄位驗證 |
| **Debug APK 建置** | `./gradlew assembleDebug` | **PASS** | 成功產出 debug APK，無編譯錯誤 |
| **TVBS 技術調查** | 官方網頁/oEmbed/IFrame 實測 | **完成** | 確認 YouTube 限制內嵌 (Error 150/101)，設定為 external_web 備援 |
| **日本直播與節目驗證** | YouTube IFrame & Official Web | **PASS** | 新增 TBS NEWS DIG, 日テレNEWS24, ウェザーニュースLiVE, 電視大阪節目等 |

---

## 二、單元測試詳細資訊

- `ChannelRepositoryTest`：驗證 `channels.json` version 5 解析正確性，確認新增的日本直播與專區分類 (`日本直播`, `日本旅遊、美食`, `日本綜藝`, `日本動畫`)。
- `UserPreferencesLogicTest`：驗證偏好設定與頻道選擇邏輯。

---

## 三、影音播放實測

1. **TVBS新聞台**：
   - 實測點擊跳轉至官方網頁 `https://news.tvbs.com.tw/`，播放穩定正常。
2. **TBS NEWS DIG (CH101)**：
   - 實測內嵌播放 >60 秒，影音同步流暢。
3. **日テレNEWS24 (CH102)**：
   - 實測內嵌播放 >60 秒，影音同步流暢。
4. **ウェザーニュースLiVE (CH103)**：
   - 實測內嵌播放 >60 秒，影音同步流暢。
5. **電視大阪節目 (おとな旅あるき旅 / もしものマネー道)**：
   - 實測內嵌播放 >60 秒，隨選影片 (VOD) 播放順暢。
