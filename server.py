import asyncio, json, os, time, websockets
KEY = os.environ.get("CHAT_KEY", "change-me")
PORT = int(os.environ.get("PORT", "8765"))
rooms = {}

async def handler(ws):
    room = None
    name = "anon"
    last = []
    try:
        hello = json.loads(await asyncio.wait_for(ws.recv(), 10))
        if hello.get("key") != KEY or not str(hello.get("room", "")).strip():
            await ws.send(json.dumps({"t": "err", "m": "auth"}))
            return
        room = str(hello["room"])[:64]
        name = str(hello.get("name", "anon"))[:20] or "anon"
        rooms.setdefault(room, set()).add(ws)
        await ws.send(json.dumps({"t": "ok", "online": len(rooms[room])}))
        async for raw in ws:
            now = time.time()
            last = [x for x in last if now - x < 5]
            if len(last) >= 5:
                await ws.send(json.dumps({"t": "err", "m": "slow down"}))
                continue
            last.append(now)
            text = str(json.loads(raw).get("m", ""))[:200].strip()
            if not text:
                continue
            msg = json.dumps({"t": "msg", "n": name, "m": text})
            for c in list(rooms[room]):
                try:
                    await c.send(msg)
                except Exception:
                    pass
    except Exception:
        pass
    finally:
        if room and room in rooms:
            rooms[room].discard(ws)
            if not rooms[room]:
                del rooms[room]

async def main():
    async with websockets.serve(handler, "0.0.0.0", PORT):
        await asyncio.Future()

if __name__ == "__main__":
    asyncio.run(main())
