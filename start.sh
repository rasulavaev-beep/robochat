cd ~/robochat-app
termux-wake-lock 2>/dev/null
command -v cloudflared >/dev/null || pkg install -y cloudflared
pkill -f "python server.py" 2>/dev/null
CHAT_KEY=$(cat server.key) python server.py &
sleep 2
cloudflared tunnel --protocol http2 --url http://127.0.0.1:8765 2>&1 | while read -r line; do
  echo "$line"
  u=$(echo "$line" | grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' | grep -v '^https://api\.' | head -1)
  if [ -n "$u" ]; then
    new="wss://${u#https://}"
    echo "$new" > server.txt
    git add server.txt </dev/null
    git commit -qm "server address" </dev/null
    git push -q origin main </dev/null && echo "✅ адрес опубликован: $new"
  fi
done
