cd ~/robochat-app
termux-wake-lock 2>/dev/null
command -v cloudflared >/dev/null || pkg install -y cloudflared
CHAT_KEY=$(cat server.key) python server.py &
sleep 2
echo "=== Найди адрес https://XXXX.trycloudflare.com ==="
echo "=== В приложении: wss://XXXX.trycloudflare.com ==="
cloudflared tunnel --protocol http2 --url http://127.0.0.1:8765
