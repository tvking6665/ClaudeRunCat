package com.jwjm.claudecat
import android.app.*; import android.content.*; import android.os.*; import androidx.core.app.NotificationCompat
import okhttp3.*; import org.json.*; import java.util.concurrent.TimeUnit; import kotlin.math.*
class CatStatusService : Service() {
  companion object { const val CH = "cat_ch"; const val ID = 1001; const val ACT = "UPD" }
  private val h = Handler(Looper.getMainLooper()); private var idx = 0; private var sPct = 100; private var wPct = 100; private var ok = false
  private val icons = intArrayOf(R.drawable.ic_cat_1, R.drawable.ic_cat_2, R.drawable.ic_cat_3, R.drawable.ic_cat_4)
  private val cli = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
  private val anim = object : Runnable { override fun run() {
    val ms = if (sPct <= 0) 0L else (80 + (100 - sPct.coerceIn(0,100)).toDouble().pow(1.42) * 1.1).toLong()
    if (ms > 0) { idx = (idx + 1) % 4; upd(); h.postDelayed(this, ms) } else { upd(); h.postDelayed(this, 2000L) }
  }}
  private val sync = object : Runnable { override fun run() { Thread { fetch() }.start(); h.postDelayed(this, 180000L) } }
  override fun onCreate() {
    super.onCreate()
    if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CH, "Claude ???", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
    load(); startForeground(ID, notif()); h.post(anim); h.post(sync)
  }
  override fun onStartCommand(i: Intent?, f: Int, s: Int): Int { load(); if (i?.action == ACT) Thread { fetch() }.start(); upd(); return START_STICKY }
  override fun onDestroy() { h.removeCallbacks(anim); h.removeCallbacks(sync); super.onDestroy() }
  override fun onBind(i: Intent?): IBinder? = null
  private fun load() { val p = getSharedPreferences("cat", 0); sPct = p.getInt("s", 100); wPct = p.getInt("w", 100) }
  private fun notif(): Notification {
    val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
    return NotificationCompat.Builder(this, CH).setSmallIcon(icons[idx]).setContentTitle("???? Claude (${if(ok)"???" else "??"}) ? 5?? $sPct%")
      .setContentText("5?? ??: $sPct% | ?? ??: $wPct%").setContentIntent(pi).setOngoing(true).setOnlyAlertOnce(true).setSilent(true).build()
  }
  private fun upd() { getSystemService(NotificationManager::class.java).notify(ID, notif()) }
  private fun fetch() {
    val p = getSharedPreferences("cat", 0); val sk = p.getString("sk", "")?.trim() ?: ""; if (sk.isEmpty()) { ok = false; return }
    try {
      var oid = p.getString("oid", "") ?: ""
      if (oid.isEmpty()) cli.newCall(Request.Builder().url("https://claude.ai/api/organizations").header("Cookie","sessionKey=$sk").build()).execute().use { r ->
        if (r.isSuccessful) { val a = JSONArray(r.body?.string()?:"[]"); if (a.length()>0) { oid = a.getJSONObject(0).optString("uuid",""); p.edit().putString("oid",oid).apply() } }
      }
      if (oid.isEmpty()) { ok = false; return }
      cli.newCall(Request.Builder().url("https://claude.ai/api/organizations/$oid/usage").header("Cookie","sessionKey=$sk").build()).execute().use { r ->
        if (r.isSuccessful) {
          val j = JSONObject(r.body?.string()?:"{}")
          j.optJSONObject("five_hour")?.let { if (it.has("utilization")) sPct = (100 - it.getDouble("utilization")).roundToInt().coerceIn(0,100) }
          j.optJSONObject("seven_day")?.let { if (it.has("utilization")) wPct = (100 - it.getDouble("utilization")).roundToInt().coerceIn(0,100) }
          p.edit().putInt("s", sPct).putInt("w", wPct).apply(); ok = true
        } else ok = false
      }
    } catch (_: Exception) { ok = false }
  }
}