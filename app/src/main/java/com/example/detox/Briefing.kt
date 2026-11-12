package com.example.detox

import android.content.Context

object Briefing {
    // 1단계: 요약 없이 "누구, 몇 건, 마지막 내용"을 읽어줌. 2단계에서 AI 요약으로 교체 예정.
    fun build(c: Context, slot: Int): String {
        val greet = when (slot) {
            0 -> "좋은 아침입니다."
            1 -> "좋은 저녁입니다."
            else -> "테스트입니다."
        }
        val msgs = Store.all(c)
        if (msgs.isEmpty()) return "$greet\n새로 온 메시지가 없습니다."
        val groups = msgs.groupBy { Targets.apps[it.pkg].orEmpty() + "|" + it.sender }
        val sb = StringBuilder("$greet\n메시지 ${groups.size}명, 총 ${msgs.size}건이 왔습니다.\n")
        groups.values.forEachIndexed { i, list ->
            val label = Targets.apps[list.last().pkg].orEmpty()
            val last = list.last().text.take(80)
            sb.append("${i + 1}번. $label, ${list.last().sender}, ${list.size}건. $last\n")
        }
        sb.append("이상입니다.")
        return sb.toString()
    }
}
