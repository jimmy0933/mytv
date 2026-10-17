import urllib.request
import re

url = "https://www.youtube.com/channel/UCpu3bemTQwAU8PqM4kJdoEQ/live" # CTI News
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    response = urllib.request.urlopen(req)
    html = response.read().decode('utf-8')
    print("Final URL:", response.url)

    print("isLive:", '"isLive":true' in html)

    vid_match = re.search(r'v=([a-zA-Z0-9_-]{11})', response.url)
    if vid_match:
        print("Extracted VID from URL:", vid_match.group(1))

except Exception as e:
    print(e)
