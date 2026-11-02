package com.rat.autotyper
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.cgutman.adblib.AdbConnection
import com.cgutman.adblib.AdbStream
import java.net.Socket
import kotlin.concurrent.thread

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private var expanded = false
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showFloatingButton()
    }

    private fun lp() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START; x = 20; y = 200 }

    private fun showFloatingButton() {
        val btn = Button(this).apply { text = "T"; textSize = 10f; setPadding(14,10,14,10) }
        btn.setOnClickListener { toggle() }
        wm.addView(btn, lp())
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(btn) }
    }

    private fun toggle() {
        if (expanded) { collapse(); return }
        expanded = true
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xEEFFFFFF.toInt())
            setPadding(16,16,16,16)
        }
        val title = TextView(this).apply { text = "AutoTyper — chọn tin:"; textSize = 12f }
        panel.addView(title)
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        Prefs.messages(this).forEachIndexed { i, msg ->
            val b = Button(this).apply { text = "${i+1}. ${msg.take(28)}"; textSize = 11f; isAllCaps = false }
            b.setOnClickListener { if (!running) startTyping(listOf(msg)) }
            list.addView(b)
        }
        scroll.addView(list)
        panel.addView(scroll, LinearLayout.LayoutParams(600, 500))
        val row = LinearLayout(this)
        val btnAll = Button(this).apply { text = "Gửi TẤT CẢ"; textSize = 10f }
        btnAll.setOnClickListener { if (!running) startTyping(Prefs.messages(this)) }
        val btnHide = Button(this).apply { text = "Ẩn"; textSize = 10f }
        btnHide.setOnClickListener { collapse() }
        row.addView(btnAll); row.addView(btnHide)
        panel.addView(row)
        wm.addView(panel, lp())
        (root?.parent?.let { wm.removeView(root) })
        root = panel
    }

    private fun collapse() {
        expanded = false
        root?.let { wm.removeView(it) }
        showFloatingButton()
    }

    private fun startTyping(msgs: List<String>) {
        running = true
        thread {
            try {
                val (host, portStr) = Prefs.host(this).split(":")
                val port = portStr.toInt()
                val sock = Socket(host, port)
                val conn = AdbConnection.create(sock, "autotyper")
                conn.connect()
                val stream: AdbStream = conn.open("shell:")
                val delay = Prefs.delayMs(this) / 1000.0
                msgs.forEachIndexed { idx, msg ->
                    msg.forEach { ch ->
                        val cmd = when (ch) {
                            ' ' -> "input keyevent 62\n"
                            '\n' -> "input keyevent 66\n"
                            else -> "input text '${ch.toString().replace("'","")}'\n"
                        }
                        stream.write(cmd.toByteArray())
                        Thread.sleep((delay * 1000).toLong())
                    }
                    stream.write("input keyevent 66\n".toByteArray()) // Enter = gửi
                    if (idx < msgs.size - 1) Thread.sleep(800)
                }
                stream.write("exit\n".toByteArray())
                sock.close()
                ui("Đã gõ xong ${msgs.size} tin")
            } catch (e: Exception) {
                ui("Lỗi ADB: ${e.message}. Chạy: adb tcpip 5555 (từ PC/Termux) trước.")
            } finally { running = false }
        }
    }

    private fun ui(s: String) = android.os.Handler(Looper.getMainLooper()).post { Toast.makeText(this, s, Toast.LENGTH_LONG).show() }

    override fun onDestroy() { root?.let { wm.removeView(it) }; super.onDestroy() }
}
