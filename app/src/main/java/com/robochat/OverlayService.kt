package com.robochat

import android.app.*
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.*
import android.widget.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class OverlayService : Service() {
  lateinit var wm: WindowManager
  lateinit var bubble: FrameLayout
  lateinit var badge: View
  lateinit var panel: LinearLayout
  lateinit var status: TextView
  lateinit var log: TextView
  lateinit var scroll: ScrollView
  lateinit var input: EditText
  lateinit var bLp: WindowManager.LayoutParams
  lateinit var pLp: WindowManager.LayoutParams
  val ui = Handler(Looper.getMainLooper())
  val client = OkHttpClient.Builder().pingInterval(25, TimeUnit.SECONDS).connectTimeout(7, TimeUnit.SECONDS).build()
  var ws: WebSocket? = null
  @Volatile var gen = 0
  @Volatile var stopped = false
  @Volatile var authFail = false
  var shown = false
  var built = false
  var nick = "Игрок"
  val KEY = "rdegt3u836"
  val SRC = "https://raw.githubusercontent.com/rasulavaev-beep/robochat/main/servers.txt"
  val F = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

  override fun onBind(i: Intent?): IBinder? = null

  override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
    try {
      if (!built) {
        built = true
        startFg()
        val sp = getSharedPreferences("rc", MODE_PRIVATE)
        nick = sp.getString("nick", null) ?: ("Игрок" + (100..999).random())
        sp.edit().putString("nick", nick).apply()
        buildUi()
        addLine("// ник: $nick (сменить: /nick Имя)")
      }
      reconnect()
    } catch (e: Throwable) {
      Toast.makeText(applicationContext, "Ошибка: $e", Toast.LENGTH_LONG).show()
      stopSelf()
    }
    return START_STICKY
  }

  fun reconnect() {
    gen++
    val old = ws
    ws = null
    old?.cancel()
    authFail = false
    connect()
  }

  fun startFg() {
    val nm = getSystemService(NotificationManager::class.java)
    nm.createNotificationChannel(NotificationChannel("rc", "RoboChat", NotificationManager.IMPORTANCE_LOW))
    startForeground(1, Notification.Builder(this, "rc").setContentTitle("RoboChat").setContentText("Чат работает").setSmallIcon(android.R.drawable.ic_dialog_email).build())
  }

  fun lp(w: Int, h: Int, ml: Int = 0): LinearLayout.LayoutParams {
    val p = LinearLayout.LayoutParams(w, h)
    p.leftMargin = ml
    return p
  }

  fun chip(t: String, a: () -> Unit): TextView {
    val v = TextView(this)
    v.text = t
    v.setTextColor(C.CYAN)
    v.textSize = 16f
    v.gravity = Gravity.CENTER
    v.background = neon(C.BG, C.CYAN, 8, 1)
    v.setOnClickListener { a() }
    return v
  }

  fun buildUi() {
    wm = getSystemService(WINDOW_SERVICE) as WindowManager
    val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    bubble = FrameLayout(this)
    val img = ImageView(this)
    img.setImageResource(R.drawable.bubble)
    bubble.addView(img, FrameLayout.LayoutParams(dp(60), dp(60)))
    badge = View(this)
    badge.background = neon(C.CYAN, C.BG, 8, 2)
    badge.visibility = View.GONE
    bubble.addView(badge, FrameLayout.LayoutParams(dp(16), dp(16), Gravity.TOP or Gravity.END))
    bLp = WindowManager.LayoutParams(dp(60), dp(60), type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
    bLp.gravity = Gravity.TOP or Gravity.START
    bLp.x = dp(8)
    bLp.y = dp(160)
    var sx = 0
    var sy = 0
    var tx = 0f
    var ty = 0f
    var moved = false
    bubble.setOnTouchListener { _, e ->
      when (e.action) {
        MotionEvent.ACTION_DOWN -> { sx = bLp.x; sy = bLp.y; tx = e.rawX; ty = e.rawY; moved = false }
        MotionEvent.ACTION_MOVE -> {
          val dx = (e.rawX - tx).toInt()
          val dy = (e.rawY - ty).toInt()
          if (abs(dx) > 8 || abs(dy) > 8) moved = true
          bLp.x = sx + dx
          bLp.y = sy + dy
          wm.updateViewLayout(bubble, bLp)
        }
        MotionEvent.ACTION_UP -> if (!moved) openPanel()
      }
      true
    }
    panel = LinearLayout(this)
    panel.orientation = LinearLayout.VERTICAL
    panel.setPadding(dp(10), dp(8), dp(10), dp(10))
    val pg = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.parseColor("#F00B1330"), Color.parseColor("#F005070F")))
    pg.cornerRadius = dp(16).toFloat()
    pg.setStroke(dp(1), C.CYAN)
    panel.background = pg
    val title = TextView(this)
    title.text = "ROBOCHAT"
    title.setTextColor(C.CYAN)
    title.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    title.textSize = 15f
    title.letterSpacing = 0.15f
    title.setShadowLayer(10f, 0f, 0f, C.CYAN)
    status = TextView(this)
    status.text = "○ подключение…"
    status.setTextColor(C.DIM)
    status.textSize = 11f
    status.typeface = Typeface.MONOSPACE
    val head = LinearLayout(this)
    head.orientation = LinearLayout.VERTICAL
    head.addView(title)
    head.addView(status)
    val top = LinearLayout(this)
    top.gravity = Gravity.CENTER_VERTICAL
    top.addView(head, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    top.addView(chip("—") { closePanel() }, lp(dp(36), dp(36), dp(6)))
    top.addView(chip("✕") { stopSelf() }, lp(dp(36), dp(36), dp(6)))
    val line = View(this)
    line.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(C.CYAN, C.BLUE, Color.TRANSPARENT))
    log = TextView(this)
    log.setTextColor(C.TEXT)
    log.textSize = 13.5f
    log.typeface = Typeface.MONOSPACE
    scroll = ScrollView(this)
    scroll.addView(log)
    input = EditText(this)
    input.hint = "сообщение"
    input.setTextColor(C.TEXT)
    input.setHintTextColor(C.DIM)
    input.typeface = Typeface.MONOSPACE
    input.textSize = 14f
    input.setSingleLine()
    input.background = neon(C.BG, C.BLUE, 10, 1)
    input.setPadding(dp(12), dp(8), dp(12), dp(8))
    val row = LinearLayout(this)
    row.addView(input, LinearLayout.LayoutParams(0, dp(44), 1f))
    row.addView(chip("➤") { sendNow() }, lp(dp(48), dp(44), dp(6)))
    panel.addView(top)
    panel.addView(line, lp(-1, dp(2)).also { it.topMargin = dp(8); it.bottomMargin = dp(8) })
    panel.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    panel.addView(row, lp(-1, -2).also { it.topMargin = dp(8) })
    pLp = WindowManager.LayoutParams(dp(312), dp(400), type, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT)
    pLp.gravity = Gravity.TOP or Gravity.START
    pLp.x = dp(8)
    pLp.y = dp(60)
    pLp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    wm.addView(bubble, bLp)
  }

  fun sendNow() {
    val t = input.text.toString().trim()
    input.setText("")
    if (t.startsWith("/nick ")) {
      nick = t.substring(6).trim().take(20).ifEmpty { nick }
      getSharedPreferences("rc", MODE_PRIVATE).edit().putString("nick", nick).apply()
      addLine("// ник: $nick")
      reconnect()
    } else if (t.isNotEmpty()) {
      ws?.send(JSONObject().put("m", t).toString())
    }
  }

  fun openPanel() {
    shown = true
    badge.visibility = View.GONE
    runCatching { wm.removeView(bubble) }
    wm.addView(panel, pLp)
  }

  fun closePanel() {
    shown = false
    runCatching { wm.removeView(panel) }
    wm.addView(bubble, bLp)
  }

  fun setStatus(t: String, c: Int) {
    ui.post { status.text = t; status.setTextColor(c) }
  }

  fun addSpan(t: CharSequence) {
    ui.post {
      log.append(t)
      if (log.length() > 8000) log.text = log.text.takeLast(4000)
      scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }
  }

  fun addLine(s: String, col: Int = C.DIM) {
    val t = SpannableString(s + "\n")
    t.setSpan(ForegroundColorSpan(col), 0, t.length, F)
    addSpan(t)
  }

  fun addMsg(n: String, m: String) {
    val own = n == nick
    val head = (if (own) "▸ " else "◂ ") + n
    val t = SpannableStringBuilder(head)
    t.setSpan(ForegroundColorSpan(if (own) C.CYAN else C.SOFT), 0, head.length, F)
    t.setSpan(StyleSpan(Typeface.BOLD), 0, head.length, F)
    val st = t.length
    t.append("\n" + m + "\n")
    t.setSpan(ForegroundColorSpan(C.TEXT), st, t.length, F)
    addSpan(t)
  }

  var urls: List<String> = emptyList()
  var ix = 0

  fun connect() {
    if (stopped) return
    val my = ++gen
    Thread {
      var list: List<String> = emptyList()
      try {
        val r = Request.Builder().url(SRC + "?t=" + System.currentTimeMillis()).build()
        client.newCall(r).execute().use { resp ->
          if (resp.isSuccessful) list = (resp.body?.string() ?: "").lines().map { it.trim() }.filter { it.startsWith("ws") }
        }
      } catch (e: Exception) {}
      ui.post { if (!stopped && my == gen) { urls = list; ix = 0; open(my) } }
    }.start()
  }

  fun open(my: Int) {
    if (urls.isEmpty()) { addLine("… сервер не найден, повтор"); retry(my); return }
    val u = urls[ix % urls.size]
    val req = try { Request.Builder().url(u).build() } catch (e: Exception) { addLine("! неверный адрес сервера", C.RED); retry(my); return }
    ws = client.newWebSocket(req, object : WebSocketListener() {
      override fun onOpen(w: WebSocket, r: Response) {
        w.send(JSONObject().put("key", KEY).put("room", "all").put("name", nick).toString())
      }
      override fun onMessage(w: WebSocket, text: String) {
        if (my != gen) return
        try {
          val d = JSONObject(text)
          when (d.getString("t")) {
            "ok" -> { addLine("✓ чат подключён", C.CYAN); setStatus("● онлайн: " + d.getInt("online"), C.CYAN) }
            "msg" -> { addMsg(d.getString("n"), d.getString("m")); ui.post { if (!shown) badge.visibility = View.VISIBLE } }
            "err" -> {
              if (d.getString("m") == "auth") { authFail = true; addLine("! неверный ключ сервера", C.RED) } else addLine("! " + d.getString("m"), C.RED)
            }
          }
        } catch (e: Exception) {}
      }
      override fun onClosed(w: WebSocket, code: Int, reason: String) { retry(my) }
      override fun onFailure(w: WebSocket, t: Throwable, r: Response?) { retry(my) }
    })
  }

  fun retry(my: Int) {
    ui.post {
      if (stopped || authFail || my != gen) return@post
      val g = ++gen
      ix++
      setStatus("○ нет связи", C.DIM)
      if (ix < urls.size) {
        addLine("… другой канал (" + (ix + 1) + "/" + urls.size + ")")
        ui.postDelayed({ if (!stopped && g == gen) open(g) }, 700)
      } else {
        addLine("… нет связи, повтор через 5 с")
        ui.postDelayed({ if (!stopped && g == gen) connect() }, 5000)
      }
    }
  }

  override fun onDestroy() {
    stopped = true
    ws?.close(1000, null)
    ui.removeCallbacksAndMessages(null)
    runCatching { wm.removeView(bubble) }
    runCatching { wm.removeView(panel) }
    super.onDestroy()
  }
}
