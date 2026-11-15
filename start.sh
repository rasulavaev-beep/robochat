cd ~/robochat-app
termux-wake-lock 2>/dev/null
pkill -f "python server.py" 2>/dev/null
pkill -f "8765" 2>/dev/null
CHAT_KEY=$(cat server.key) python server.py &
sleep 2
pub() {
  new="wss://${1#https://}"
  echo "$new" > server.txt
  git add server.txt </dev/null
  git commit -qm "server address" </dev/null
  git push -q origin main </dev/null && echo "✅ адрес опубликован: $new"
}
selfcheck() {
  for i in 1 2 3 4 5 6; do
    sleep 10
    python check.py > check.out 2>&1
    if grep -q "PASS через" check.out; then echo "✅ чат работает через туннель, жми СТАРТ в приложении"; return; fi
  done
  echo "⚠ через туннель не отвечает:"; tail -3 check.out
}
try() {
  : > tunnel.log
  echo "→ пробую $1..."
  sleep 999999 | $2 > tunnel.log 2>&1 &
  TP=$!
  for i in $(seq 1 30); do
    sleep 1
    u=$(grep -o 'https://[A-Za-z0-9.-]*\.\(pinggy\.link\|lhr\.life\|serveousercontent\.com\|trycloudflare\.com\)' tunnel.log | grep -v '^https://api\.' | head -1)
    if [ -n "$u" ]; then
      pub "$u"
      selfcheck &
      wait $TP
      return 0
    fi
  done
  kill $TP 2>/dev/null
  pkill -f "sleep 999999" 2>/dev/null
  echo "✗ $1 не ответил"
  return 1
}
while true; do
  try pinggy "ssh -p 443 -T -R0:localhost:8765 -o StrictHostKeyChecking=no -o ServerAliveInterval=30 a.pinggy.io" ||
  try localhost.run "ssh -p 443 -T -R80:localhost:8765 -o StrictHostKeyChecking=no -o ServerAliveInterval=30 nokey@localhost.run" ||
  try serveo "ssh -T -R 80:localhost:8765 -o StrictHostKeyChecking=no -o ServerAliveInterval=30 serveo.net" ||
  { command -v cloudflared >/dev/null || pkg install -y cloudflared; try cloudflare "cloudflared tunnel --protocol http2 --url http://127.0.0.1:8765"; }
  echo "↻ перезапуск туннеля через 5 с"
  sleep 5
done
