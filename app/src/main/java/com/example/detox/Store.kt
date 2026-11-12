package com.example.detox

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Msg(val pkg: String, val sender: String, val text: String, val time: Long)

object Targets {
    // 수집할 앱 (패키지명 -> 읽어줄 때 부를 이름)
    val apps = mapOf(
        "com.kakao.talk" to "카카오톡",
        "com.google.android.apps.messaging" to "문자",
        "com.samsung.android.messaging" to "문자",
        "com.android.mms" to "문자",
        "com.instagram.android" to "인스타그램"
    )
}

object Store {
    private fun p(c: Context) = c.getSharedPreferences("detox", Context.MODE_PRIVATE)

    @Synchronized
    fun add(c: Context, m: Msg) {
        val arr = JSONArray(p(c).getString("msgs", "[]"))
        if (arr.length() > 0) {
            val last = arr.getJSONObject(arr.length() - 1)
            if (last.getString("p") == m.pkg && last.getString("s") == m.sender && last.getString("t") == m.text) return
        }
        arr.put(JSONObject().put("p", m.pkg).put("s", m.sender).put("t", m.text).put("m", m.time))
        val out = JSONArray()
        for (i in maxOf(0, arr.length() - 500) until arr.length()) out.put(arr.get(i))
        p(c).edit().putString("msgs", out.toString()).apply()
    }

    @Synchronized
    fun all(c: Context): List<Msg> {
        val arr = JSONArray(p(c).getString("msgs", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Msg(o.getString("p"), o.getString("s"), o.getString("t"), o.getLong("m"))
        }
    }

    @Synchronized
    fun clear(c: Context) { p(c).edit().putString("msgs", "[]").apply() }

    fun getTime(c: Context, slot: Int): Pair<Int, Int> {
        val def = if (slot == 0) 8 * 60 else 21 * 60
        val v = p(c).getInt("time$slot", def)
        return Pair(v / 60, v % 60)
    }

    fun setTime(c: Context, slot: Int, h: Int, m: Int) {
        p(c).edit().putInt("time$slot", h * 60 + m).apply()
    }
}
