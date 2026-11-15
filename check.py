import asyncio, json, os, time, urllib.request, websockets
H = os.path.expanduser("~/robochat-app/")

async def one(name, url, key):
    try:
        async with websockets.connect(url, open_timeout=15) as ws:
            await ws.send(json.dumps({"key": key, "room": "probe", "name": "check"}))
            r = json.loads(await asyncio.wait_for(ws.recv(), 10))
            print("PASS" if r.get("t") == "ok" else "FAIL", name, url)
    except Exception as e:
        print("FAIL", name, url, type(e).__name__)

async def main():
    key = open(H + "server.key").read().strip()
    raw = urllib.request.urlopen("https://raw.githubusercontent.com/rasulavaev-beep/robochat/main/servers.txt?t=%d" % time.time(), timeout=15).read().decode().split()
    print("каналов на GitHub:", len(raw))
    await one("локальный", "ws://127.0.0.1:8765", key)
    for u in raw:
        await one("туннель", u, key)

asyncio.run(main())
