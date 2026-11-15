import asyncio, json, os, time, urllib.request, websockets
H = os.path.expanduser("~/robochat-app/")
def get(u):
    try:
        return urllib.request.urlopen(u, timeout=10).read().decode().strip()
    except Exception as e:
        return "ERR " + str(e)
async def main():
    key = open(H + "server.key").read().strip()
    loc = open(H + "server.txt").read().strip()
    raw = get("https://raw.githubusercontent.com/rasulavaev-beep/robochat/main/server.txt?t=%d" % time.time())
    print("адрес локально :", loc)
    print("адрес на GitHub:", raw)
    for name, url in (("локальный сервер", "ws://127.0.0.1:8765"), ("через туннель", raw)):
        try:
            async with websockets.connect(url, open_timeout=15) as ws:
                await ws.send(json.dumps({"key": key, "room": "all", "name": "check"}))
                r = json.loads(await asyncio.wait_for(ws.recv(), 10))
                print("PASS" if r.get("t") == "ok" else "FAIL", name, r)
        except Exception as e:
            print("FAIL", name, type(e).__name__, e)
asyncio.run(main())
