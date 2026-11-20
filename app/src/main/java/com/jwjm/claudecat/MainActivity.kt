package com.jwjm.claudecat
import android.*; import android.content.*; import android.content.pm.PackageManager; import android.graphics.Color; import android.os.*; import android.widget.*
import androidx.appcompat.app.AppCompatActivity; import androidx.core.app.ActivityCompat; import androidx.core.content.ContextCompat
class MainActivity : AppCompatActivity() {
  override fun onCreate(b: Bundle?) {
    super.onCreate(b)
    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    val p = getSharedPreferences("cat", 0)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#0B1622")); setPadding(48,64,48,48) }
    root.addView(TextView(this).apply { text = "???? ? ??? Claude ???"; textSize = 20f; setTextColor(Color.WHITE) })
    val ed = EditText(this).apply { hint = "sessionKey ??"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); setText(p.getString("sk","")) }
    root.addView(ed)
    val lb = TextView(this).apply { text = "?? ???: ${p.getInt("s",100)}%"; setTextColor(Color.parseColor("#FF7B3D")); setPadding(0,24,0,8) }
    root.addView(lb)
    root.addView(SeekBar(this).apply { max = 100; progress = p.getInt("s",100); setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) { lb.text = "?? ???: $v%"; p.edit().putInt("s", v).apply(); go(false) }
      override fun onStartTrackingTouch(s: SeekBar?) {}; override fun onStopTrackingTouch(s: SeekBar?) {}
    })})
    root.addView(Button(this).apply { text = "?? ? ??"; setBackgroundColor(Color.parseColor("#FF7B3D")); setTextColor(Color.WHITE); setOnClickListener {
      p.edit().putString("sk", ed.text.toString().trim()).putString("oid","").apply(); go(true); Toast.makeText(this@MainActivity,"???",Toast.LENGTH_SHORT).show()
    }})
    root.addView(Button(this).apply { text = "??"; setOnClickListener { stopService(Intent(this@MainActivity, CatStatusService::class.java)) } })
    setContentView(root); go(false)
  }
  private fun go(sync: Boolean) {
    val i = Intent(this, CatStatusService::class.java).apply { if (sync) action = CatStatusService.ACT }
    if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
  }
}