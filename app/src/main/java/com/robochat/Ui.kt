package com.robochat

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.View

object C {
  val BG = Color.parseColor("#05070F")
  val NAVY = Color.parseColor("#0B1330")
  val CYAN = Color.parseColor("#00E5FF")
  val BLUE = Color.parseColor("#3D5AFE")
  val SOFT = Color.parseColor("#8FA4FF")
  val TEXT = Color.parseColor("#E6EDFF")
  val DIM = Color.parseColor("#7F8CB5")
  val RED = Color.parseColor("#FF5252")
}

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

fun Context.neon(fill: Int, stroke: Int, r: Int, w: Int = 1): GradientDrawable {
  val g = GradientDrawable()
  g.setColor(fill)
  g.cornerRadius = dp(r).toFloat()
  g.setStroke(maxOf(1, dp(w)), stroke)
  return g
}

class BgView(c: Context) : View(c) {
  private val p = Paint(Paint.ANTI_ALIAS_FLAG)

  override fun onDraw(cv: Canvas) {
    val w = width.toFloat()
    val h = height.toFloat()
    p.style = Paint.Style.FILL
    p.shader = LinearGradient(0f, 0f, w, h, intArrayOf(C.BG, C.NAVY, C.BG), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
    cv.drawRect(0f, 0f, w, h, p)
    p.shader = null
    p.strokeWidth = 1f
    p.color = Color.argb(26, 61, 90, 254)
    val s = context.dp(36).toFloat()
    var x = 0f
    while (x < w) { cv.drawLine(x, 0f, x, h, p); x += s }
    var y = 0f
    while (y < h) { cv.drawLine(0f, y, w, y, p); y += s }
    p.style = Paint.Style.STROKE
    p.strokeWidth = context.dp(2).toFloat()
    p.color = Color.argb(80, 0, 229, 255)
    cv.drawCircle(-w * 0.1f, h * 0.06f, w * 0.55f, p)
    p.color = Color.argb(70, 61, 90, 254)
    cv.drawCircle(w * 1.05f, h * 0.96f, w * 0.7f, p)
    p.color = C.CYAN
    val m = context.dp(14).toFloat()
    val l = context.dp(28).toFloat()
    corner(cv, m, m, l, l)
    corner(cv, w - m, m, -l, l)
    corner(cv, m, h - m, l, -l)
    corner(cv, w - m, h - m, -l, -l)
  }

  private fun corner(cv: Canvas, x: Float, y: Float, dx: Float, dy: Float) {
    cv.drawLine(x, y, x + dx, y, p)
    cv.drawLine(x, y, x, y + dy, p)
  }
}
