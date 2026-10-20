from urllib import request

def check_hls(url):
    try:
        req = request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        res = request.urlopen(req)
        return res.status == 200
    except:
        return False

print("Running...")
