package com.robochat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
  override fun onCreate(b: Bundle?) {
    super.onCreate(b)
    if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
    val btn = Button(this)
    btn.text = "СТАРТ"
    val box = LinearLayout(this)
    box.gravity = Gravity.CENTER
    box.addView(btn, LinearLayout.LayoutParams(-2, -2))
    setContentView(box)
    btn.setOnClickListener {
      if (!Settings.canDrawOverlays(this)) {
        Toast.makeText(this, "Разреши «Поверх других приложений» и нажми СТАРТ снова", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
      } else {
        startForegroundService(Intent(this, OverlayService::class.java))
        moveTaskToBack(true)
      }
    }
  }
}
