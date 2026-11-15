package com.robochat

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
  override fun onCreate(b: Bundle?) {
    super.onCreate(b)
    window.statusBarColor = C.BG
    window.navigationBarColor = C.BG
    if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
    val screenW = resources.displayMetrics.widthPixels
    val root = FrameLayout(this)
    root.addView(BgView(this), FrameLayout.LayoutParams(-1, -1))
    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    col.gravity = Gravity.CENTER
    col.setPadding(dp(24), dp(24), dp(24), dp(24))
    val logo = ImageView(this)
    logo.setImageResource(R.drawable.logo)
    logo.adjustViewBounds = true
    col.addView(logo, LinearLayout.LayoutParams((screenW * 0.72).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT))
    val title = TextView(this)
    val sp = SpannableString("RoboChat")
    sp.setSpan(ForegroundColorSpan(Color.WHITE), 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    sp.setSpan(ForegroundColorSpan(C.CYAN), 4, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    title.text = sp
    title.textSize = 42f
    title.typeface = Typeface.DEFAULT_BOLD
    title.setShadowLayer(22f, 0f, 0f, C.CYAN)
    col.addView(title)
    val tag = TextView(this)
    tag.text = "Общайся. Спрашивай. Узнавай."
    tag.setTextColor(C.DIM)
    tag.textSize = 15f
    tag.setPadding(0, dp(6), 0, dp(36))
    col.addView(tag)
    val btn = TextView(this)
    btn.text = "Старт  →"
    btn.textSize = 20f
    btn.typeface = Typeface.DEFAULT_BOLD
    btn.setTextColor(Color.WHITE)
    btn.gravity = Gravity.CENTER
    btn.setShadowLayer(14f, 0f, 0f, C.CYAN)
    btn.background = LayerDrawable(arrayOf<Drawable>(neon(Color.TRANSPARENT, Color.argb(70, 0, 229, 255), 30, 7), neon(C.BG, C.CYAN, 30, 2)))
    col.addView(btn, LinearLayout.LayoutParams((screenW * 0.62).toInt(), dp(60)))
    val foot = TextView(this)
    foot.text = "● общий чат для всех, у кого есть RoboChat"
    foot.setTextColor(C.DIM)
    foot.textSize = 12f
    foot.typeface = Typeface.MONOSPACE
    foot.setPadding(0, dp(28), 0, 0)
    col.addView(foot)
    root.addView(col, FrameLayout.LayoutParams(-1, -1))
    setContentView(root)
    btn.setOnClickListener {
      if (!Settings.canDrawOverlays(this)) {
        Toast.makeText(this, "Разреши «Поверх других приложений» и нажми Старт снова", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
      } else {
        startForegroundService(Intent(this, OverlayService::class.java))
        moveTaskToBack(true)
      }
    }
  }
}
