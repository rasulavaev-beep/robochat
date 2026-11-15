cd ~/robochat-app
termux-wake-lock 2>/dev/null
command -v ssh >/dev/null || pkg install -y openssh
command -v cloudflared >/dev/null || pkg install -y cloudflared
[ -f ~/.ssh/id_ed25519 ] || ssh-keygen -q -t ed25519 -N "" -f ~/.ssh/id_ed25519
pkill -f "python server.py" 2>/dev/null
pkill -f "8765" 2>/dev/null
pkill -f "sleep 99999" 2>/dev/null
trap 'trap - EXIT; kill 0' EXIT INT TERM
KEY=$(cat server.key)
CHAT_KEY=$KEY python server.py &
sleep 2
sup() {
  n=$1; sl=$2; shift 2
  while true; do
    : > "tun_$n.log"
    sleep $sl | "$@" > "tun_$n.log" 2>&1 &
    wait $!
    pkill -f "sleep $sl" 2>/dev/null
    sleep 3
  done
}
S="-o StrictHostKeyChecking=no -o BatchMode=yes -o ServerAliveInterval=20 -o ExitOnForwardFailure=yes"
sup lhr 999991 ssh -p 443 -T -R80:localhost:8765 $S nokey@localhost.run &
sup pinggy 999992 ssh -p 443 -T -R0:localhost:8765 $S a.pinggy.io &
sup serveo 999993 ssh -T -R 80:localhost:8765 $S serveo.net &
sup cf 999994 cloudflared tunnel --protocol http2 --url http://127.0.0.1:8765 &
echo "▶ сервер запущен, туннели поднимаются (до минуты)…"
while true; do
  sleep 15
  : > new.txt
  for n in lhr pinggy serveo cf; do
    u=$(grep -o 'https://[A-Za-z0-9.-]*\.\(pinggy\.link\|lhr\.life\|serveousercontent\.com\|trycloudflare\.com\)' "tun_$n.log" 2>/dev/null | grep -v '^https://api\.' | tail -1)
    [ -n "$u" ] && python probe.py "wss://${u#https://}" "$KEY" && echo "wss://${u#https://}" >> new.txt
  done
  if [ -s new.txt ]; then
    if ! cmp -s new.txt servers.txt; then
      cp new.txt servers.txt
      head -1 new.txt > server.txt
      git add server.txt servers.txt </dev/null
      git commit -qm "addr" </dev/null
      git push -q origin main </dev/null && echo "✅ живых каналов: $(wc -l < servers.txt)"
    fi
  else
    echo "⚠ живых туннелей пока нет, жду…"
  fi
done
