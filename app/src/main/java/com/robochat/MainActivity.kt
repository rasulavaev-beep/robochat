package com.robochat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
  override fun onCreate(b: Bundle?) {
    super.onCreate(b)
    if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
    val sp = getSharedPreferences("rc", MODE_PRIVATE)
    val pad = (16 * resources.displayMetrics.density).toInt()
    val root = LinearLayout(this)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(pad, pad, pad, pad)
    fun field(h: String, k: String, d: String): EditText {
      val e = EditText(this)
      e.hint = h
      e.setText(sp.getString(k, d))
      e.setSingleLine()
      root.addView(e)
      return e
    }
    val url = field("Адрес сервера (wss://...)", "url", "wss://")
    val key = field("Ключ сервера", "key", "")
    val nick = field("Ник", "nick", "")
    val link = field("Ссылка на игру Roblox", "link", "")
    val job = field("ID сервера (необязательно)", "job", "")
    val btn = Button(this)
    btn.text = "СТАРТ"
    root.addView(btn)
    val sv = ScrollView(this)
    sv.addView(root)
    setContentView(sv)
    btn.setOnClickListener {
      val m = Regex("\\d{5,}").find(link.text.toString())
      if (m == null) {
        Toast.makeText(this, "В ссылке нет placeId", Toast.LENGTH_LONG).show()
        return@setOnClickListener
      }
      if (!Settings.canDrawOverlays(this)) {
        Toast.makeText(this, "Разреши «Поверх других приложений» и нажми СТАРТ снова", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        return@setOnClickListener
      }
      sp.edit().putString("url", url.text.toString()).putString("key", key.text.toString()).putString("nick", nick.text.toString()).putString("link", link.text.toString()).putString("job", job.text.toString()).apply()
      val j = job.text.toString().trim()
      val room = m.value + (if (j.isEmpty()) "" else ":$j")
      startForegroundService(Intent(this, OverlayService::class.java).putExtra("url", url.text.toString().trim()).putExtra("key", key.text.toString().trim()).putExtra("room", room).putExtra("nick", nick.text.toString().trim().ifEmpty { "anon" }))
      moveTaskToBack(true)
    }
  }
}
