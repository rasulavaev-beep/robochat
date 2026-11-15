import asyncio, json, sys, websockets

async def main(u, key):
    async with websockets.connect(u, open_timeout=8) as ws:
        await ws.send(json.dumps({"key": key, "room": "probe", "name": "probe"}))
        r = json.loads(await asyncio.wait_for(ws.recv(), 8))
        return r.get("t") == "ok"

try:
    sys.exit(0 if asyncio.run(main(sys.argv[1], sys.argv[2])) else 1)
except Exception:
    sys.exit(1)
