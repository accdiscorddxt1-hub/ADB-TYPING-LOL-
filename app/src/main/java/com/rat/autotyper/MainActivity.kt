package com.rat.autotyper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val etMsgs = findViewById<EditText>(R.id.etMsgs)
        val etHost = findViewById<EditText>(R.id.etHost)
        val etDelay = findViewById<EditText>(R.id.etDelay)
        etMsgs.setText(Prefs.messages(this).joinToString("\n"))
        etHost.setText(Prefs.host(this))
        etDelay.setText(Prefs.delayMs(this).toString())

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            Prefs.save(this, etMsgs.text.toString().split("\n").filter { it.isNotBlank() })
            Prefs.saveHost(this, etHost.text.toString().trim())
            etDelay.text.toString().toIntOrNull()?.let { Prefs.saveDelay(this, it) }
            Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                Toast.makeText(this, "Cấp quyền hiển thị nổi rồi bấm lại", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            startService(Intent(this, FloatingService::class.java))
            Toast.makeText(this, "Menu nổi đã bật — chuyển sang app cần gõ", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnStop).setOnClickListener { stopService(Intent(this, FloatingService::class.java)) }
    }
}
