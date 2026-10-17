# Channel Verification Report

- **中天新聞台 (CTI News):** Verified. Fixed incorrect channel ID, replaced with `UC5l1Yto5oOIgRXlI4p4VKbw`. Dynamic live stream resolution works through `/streams` scraping logic. App playback successful.
- **非凡新聞台 (USTV News):** Verified. Channel ID `UCYIVkruUoN04UjV9pkBTswg`. App playback successful.
- **非凡商業台 (USTV Business):** Verified. Channel ID `UCLHsQL4YChylDX-06BCuNQA`. App playback successful.
- **東森財經新聞台 (EBC Financial):** Existing configuration intact and verified.
- **三立財經iNEWS (SET iNEWS):** Existing configuration intact and verified.

## Multiple Streams
LiveResolver now parses `/streams` tab for `ytInitialData`. Live and Upcoming programs are extracted and exposed through the new `LiveItem` data model. The UI has been updated to list these streams in the Player side drawer to let users choose the specific program to watch.
