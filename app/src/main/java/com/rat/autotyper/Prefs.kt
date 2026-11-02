package com.rat.autotyper
import android.content.Context

object Prefs {
    private const val P = "autotyper"
    fun messages(ctx: Context): MutableList<String> {
        val raw = ctx.getSharedPreferences(P, 0).getString("msgs", "Xin chào|Bạn còn hàng không?|Giá bao nhiêu?|Cảm ơn!|Hẹn gặp lại") ?: ""
        return raw.split("|").filter { it.isNotBlank() }.toMutableList()
    }
    fun save(ctx: Context, list: List<String>) {
        ctx.getSharedPreferences(P, 0).edit().putString("msgs", list.joinToString("|")).apply()
    }
    fun host(ctx: Context) = ctx.getSharedPreferences(P,0).getString("host","127.0.0.1:5555") ?: "127.0.0.1:5555"
    fun saveHost(ctx: Context, h: String) = ctx.getSharedPreferences(P,0).edit().putString("host",h).apply()
    fun delayMs(ctx: Context) = ctx.getSharedPreferences(P,0).getInt("delay", 15)
    fun saveDelay(ctx: Context, d: Int) = ctx.getSharedPreferences(P,0).edit().putInt("delay",d).apply()
}
