package com.robochat

import android.app.*
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
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
  lateinit var bubble: TextView
  lateinit var panel: LinearLayout
  lateinit var log: TextView
  lateinit var scroll: ScrollView
  lateinit var input: EditText
  lateinit var bLp: WindowManager.LayoutParams
  lateinit var pLp: WindowManager.LayoutParams
  val ui = Handler(Looper.getMainLooper())
  val client = OkHttpClient.Builder().pingInterval(25, TimeUnit.SECONDS).build()
  var ws: WebSocket? = null
  @Volatile var gen = 0
  @Volatile var stopped = false
  @Volatile var authFail = false
  var shown = false
  var built = false
  var nick = "Игрок"
  val KEY = "rdegt3u836"
  val SRC = "https://raw.githubusercontent.com/rasulavaev-beep/robochat/main/server.txt"

  override fun onBind(i: Intent?): IBinder? = null
  fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
  fun btn(t: String, a: () -> Unit) = Button(this).apply { text = t; setOnClickListener { a() } }

  override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
    try {
      if (!built) {
        built = true
        startFg()
        val sp = getSharedPreferences("rc", MODE_PRIVATE)
        nick = sp.getString("nick", null) ?: ("Игрок" + (100..999).random())
        sp.edit().putString("nick", nick).apply()
        buildUi()
        addLine("Ник: $nick (сменить: /nick Имя)")
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

  fun buildUi() {
    wm = getSystemService(WINDOW_SERVICE) as WindowManager
    val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    bubble = TextView(this).apply {
      text = "💬"; textSize = 22f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
      background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#D32F2F")) }
    }
    bLp = WindowManager.LayoutParams(dp(56), dp(56), type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
      gravity = Gravity.TOP or Gravity.START; x = dp(8); y = dp(160)
    }
    var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false
    bubble.setOnTouchListener { _, e ->
      when (e.action) {
        MotionEvent.ACTION_DOWN -> { sx = bLp.x; sy = bLp.y; tx = e.rawX; ty = e.rawY; moved = false }
        MotionEvent.ACTION_MOVE -> {
          val dx = (e.rawX - tx).toInt(); val dy = (e.rawY - ty).toInt()
          if (abs(dx) > 8 || abs(dy) > 8) moved = true
          bLp.x = sx + dx; bLp.y = sy + dy
          wm.updateViewLayout(bubble, bLp)
        }
        MotionEvent.ACTION_UP -> if (!moved) openPanel()
      }
      true
    }
    panel = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      setPadding(dp(8), dp(8), dp(8), dp(8))
      background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(Color.parseColor("#F0121212")) }
    }
    val top = LinearLayout(this)
    val title = TextView(this).apply { text = "RoboChat"; setTextColor(Color.parseColor("#FF5252")); gravity = Gravity.CENTER_VERTICAL }
    top.addView(title, LinearLayout.LayoutParams(0, dp(44), 1f))
    top.addView(btn("—") { closePanel() }, LinearLayout.LayoutParams(dp(52), dp(44)))
    top.addView(btn("✕") { stopSelf() }, LinearLayout.LayoutParams(dp(52), dp(44)))
    log = TextView(this).apply { setTextColor(Color.WHITE); textSize = 13f }
    scroll = ScrollView(this).apply { addView(log) }
    input = EditText(this).apply { hint = "Сообщение"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setSingleLine() }
    val row = LinearLayout(this)
    row.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    row.addView(btn("➤") {
      val t = input.text.toString().trim()
      input.setText("")
      if (t.startsWith("/nick ")) {
        nick = t.substring(6).trim().take(20).ifEmpty { nick }
        getSharedPreferences("rc", MODE_PRIVATE).edit().putString("nick", nick).apply()
        addLine("Ник: $nick")
        reconnect()
      } else if (t.isNotEmpty()) {
        ws?.send(JSONObject().put("m", t).toString())
      }
    }, LinearLayout.LayoutParams(dp(56), dp(48)))
    panel.addView(top)
    panel.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
    panel.addView(row)
    pLp = WindowManager.LayoutParams(dp(300), dp(340), type, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
      gravity = Gravity.TOP or Gravity.START; x = dp(8); y = dp(60)
      softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    }
    wm.addView(bubble, bLp)
  }

  fun openPanel() { shown = true; bubble.text = "💬"; runCatching { wm.removeView(bubble) }; wm.addView(panel, pLp) }
  fun closePanel() { shown = false; runCatching { wm.removeView(panel) }; wm.addView(bubble, bLp) }

  fun addLine(s: String) {
    ui.post {
      log.append(s + "\n")
      if (log.length() > 8000) log.text = log.text.takeLast(4000)
      scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }
  }

  fun connect() {
    if (stopped) return
    val my = ++gen
    Thread {
      var u: String? = null
      try {
        val r = Request.Builder().url(SRC + "?t=" + System.currentTimeMillis()).build()
        client.newCall(r).execute().use { resp -> if (resp.isSuccessful) u = resp.body?.string()?.trim() }
      } catch (e: Exception) {}
      ui.post { if (!stopped && my == gen) open(u, my) }
    }.start()
  }

  fun open(u: String?, my: Int) {
    if (u == null || !u.startsWith("ws")) { addLine("… сервер не найден, повтор"); retry(my); return }
    val req = try { Request.Builder().url(u).build() } catch (e: Exception) { addLine("! неверный адрес сервера"); retry(my); return }
    ws = client.newWebSocket(req, object : WebSocketListener() {
      override fun onOpen(w: WebSocket, r: Response) {
        w.send(JSONObject().put("key", KEY).put("room", "all").put("name", nick).toString())
      }
      override fun onMessage(w: WebSocket, text: String) {
        if (my != gen) return
        try {
          val d = JSONObject(text)
          when (d.getString("t")) {
            "ok" -> addLine("✓ чат подключён, онлайн: ${d.getInt("online")}")
            "msg" -> { addLine("[" + d.getString("n") + "] " + d.getString("m")); ui.post { if (!shown) bubble.text = "💬!" } }
            "err" -> {
              if (d.getString("m") == "auth") { authFail = true; addLine("! неверный ключ сервера") } else addLine("! " + d.getString("m"))
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
      gen++
      addLine("… нет связи, повтор через 5 с")
      ui.postDelayed({ connect() }, 5000)
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
