package com.jwjm.claudecat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
                )
            }
        }

        val prefs = getSharedPreferences("cat", Context.MODE_PRIVATE)
        val initPct = prefs.getInt("s", 100)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B1622"))
            setPadding(48, 64, 48, 48)
        }

        val title = TextView(this).apply {
            text = "전우정밀 · 상단바 Claude 고양이"
            textSize = 20f
            setTextColor(Color.WHITE)
        }
        root.addView(title)

        val editKey = EditText(this).apply {
            hint = "sessionKey 입력"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setText(prefs.getString("sk", ""))
        }
        root.addView(editKey)

        val labelSpeed = TextView(this).apply {
            text = "속도 테스트: $initPct%"
            setTextColor(Color.parseColor("#FF7B3D"))
            setPadding(0, 24, 0, 8)
        }
        root.addView(labelSpeed)

        val seekBar = SeekBar(this).apply {
            max = 100
            progress = initPct
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                    labelSpeed.text = "속도 테스트: $v%"
                    prefs.edit().putInt("s", v).apply()
                    startCatService(false)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        root.addView(seekBar)

        val btnStart = Button(this).apply {
            text = "저장 및 시작"
            setBackgroundColor(Color.parseColor("#FF7B3D"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                prefs.edit()
                    .putString("sk", editKey.text.toString().trim())
                    .putString("oid", "")
                    .apply()
                startCatService(true)
                Toast.makeText(this@MainActivity, "시작됨", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(btnStart)

        val btnStop = Button(this).apply {
            text = "끄기"
            setOnClickListener {
                stopService(Intent(this@MainActivity, CatStatusService::class.java))
            }
        }
        root.addView(btnStop)

        setContentView(root)
        startCatService(false)
    }

    private fun startCatService(sync: Boolean) {
        val intent = Intent(this, CatStatusService::class.java).apply {
            if (sync) action = CatStatusService.ACT
        }
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
