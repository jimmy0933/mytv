import json

with open('app/src/main/assets/channels.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

# Deduplicate channels by ID
seen_ids = set()
new_channels = []
for ch in data['channels']:
    if ch['id'] not in seen_ids:
        # Check if we should keep this one. The first ones are ustv_news (台灣財經) with number 58
        seen_ids.add(ch['id'])
        if ch['category'] == '財經':
            ch['category'] = '台灣財經'

        # We need to ensure multi-source support
        # Let's populate sources based on existing fields
        source = {
            "type": ch.get("type", "youtube_iframe"),
        }
        if ch.get("youtube_video_id"):
            source["youtube_video_id"] = ch["youtube_video_id"]
        if ch.get("youtube_channel_id"):
            source["youtube_channel_id"] = ch["youtube_channel_id"]
        if ch.get("stream_url"):
            source["stream_url"] = ch["stream_url"]
        if ch.get("official_url"):
            source["official_url"] = ch["official_url"]

        ch["sources"] = [source]
        new_channels.append(ch)

data['channels'] = new_channels

# Fix categories
if '台灣財經' not in data.get('categories', []):
    data['categories'].append('台灣財經')
if '財經' in data.get('categories', []):
    data['categories'].remove('財經')

# Update specific channels with HLS or proper multi-sources if needed.
# Requirement mentions "CTI, TVBS, 非凡新聞, 非凡商業 prioritized per-channel check for sources."
# Let's write them back first.
with open('app/src/main/assets/channels.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
