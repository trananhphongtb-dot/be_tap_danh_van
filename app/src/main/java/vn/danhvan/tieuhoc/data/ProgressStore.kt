package vn.danhvan.tieuhoc.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Hồ sơ trên máy, vì app KHÔNG đăng nhập, KHÔNG tài khoản, KHÔNG online.
 * Lưu: chặng nào đã qua, và câu nào đã dùng ở từng chặng (luật không lặp).
 */
class ProgressStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("danhvan_offline", Context.MODE_PRIVATE)

    fun isPassed(stage: Int): Boolean = prefs.getBoolean("passed_$stage", false)

    fun markPassed(stage: Int) {
        prefs.edit().putBoolean("passed_$stage", true).apply()
    }

    fun allPassed(): Boolean = (1..7).all { isPassed(it) }

    /** Reset về như ban đầu khi đã qua hết 7 phần: xóa dấu qua và
     *  xóa lịch sử câu đã dùng của mọi phần, để chơi lại từ đầu. */
    fun resetAll() {
        val editor = prefs.edit()
        for (i in 1..7) {
            editor.remove("passed_$i")
            editor.remove(usedKey(i))
        }
        editor.apply()
    }

    fun isUnlocked(stage: Int): Boolean {
        if (UnlockRules.UNLOCK_ALL) return true
        if (stage <= 1) return true
        return isPassed(stage - 1)
    }

    private fun usedKey(stage: Int) = "used_stage_$stage"

    fun usedIds(stage: Int): Set<String> =
        prefs.getStringSet(usedKey(stage), emptySet())?.toSet() ?: emptySet()

    fun markUsed(stage: Int, id: String) {
        val set = usedIds(stage).toMutableSet()
        set += id
        prefs.edit().putStringSet(usedKey(stage), set).apply()
    }

    fun clearUsed(stage: Int) {
        prefs.edit().remove(usedKey(stage)).apply()
    }
}

/**
 * Chọn câu không lặp theo từng chặng, theo từng hồ sơ trên máy.
 * - Câu vừa hiện ra là đã tính là dùng rồi, kể cả đúng hay sai.
 * - Chỉ reset khi đã dùng hết toàn bộ ngân hàng của chặng đó, và phải báo
 *   "Đã hết câu mới, bắt đầu vòng mới!" (trả về newCycle = true).
 * - MỚI 2026-10-29: giữ lịch sử câu hỏi trong 1 lần vào chặng để nút
 *   "Câu trước" quay lại đúng câu cũ, không phải câu mới. Lịch sử chỉ sống
 *   trong bộ nhớ của lần vào chặng này, còn luật không lặp vẫn lưu trên máy
 *   qua ProgressStore nên sang lần thi sau vẫn không trùng câu cũ.
 */
class NoRepeatPicker(private val store: ProgressStore) {
    data class Pick<T>(val item: T, val newCycle: Boolean)

    // Lịch sử theo chặng, theo thứ tự câu đã hiện trong lần vào chặng này.
    private val history = mutableMapOf<Int, MutableList<Any>>()
    private val historyNewCycle = mutableMapOf<Int, MutableList<Boolean>>()

    fun historySize(stage: Int): Int = history[stage]?.size ?: 0

    @Suppress("UNCHECKED_CAST")
    fun <T> pickAt(stage: Int, index: Int, pool: List<T>, idOf: (T) -> String): Pick<T>? {
        if (pool.isEmpty()) return null
        val list = history.getOrPut(stage) { mutableListOf() }
        val cycles = historyNewCycle.getOrPut(stage) { mutableListOf() }
        if (index < list.size) {
            return Pick(list[index] as T, cycles[index])
        }
        // Chỉ tạo câu mới khi index đúng bằng cuối lịch sử. Nếu nhảy cóc
        // thì coi như câu mới tiếp theo, tránh tạo lỗ hổng trong lịch sử.
        val picked = pick(stage, pool, idOf) ?: return null
        list.add(picked.item as Any)
        cycles.add(picked.newCycle)
        return picked
    }

    fun <T> pick(stage: Int, pool: List<T>, idOf: (T) -> String): Pick<T>? {
        if (pool.isEmpty()) return null
        val used = store.usedIds(stage)
        val unused = pool.filter { idOf(it) !in used }
        if (unused.isNotEmpty()) {
            val item = unused.random()
            store.markUsed(stage, idOf(item))
            return Pick(item, false)
        }
        // Hết câu mới: xóa danh sách đã dùng, bắt đầu vòng mới.
        store.clearUsed(stage)
        val item = pool.random()
        store.markUsed(stage, idOf(item))
        return Pick(item, true)
    }

    /** Xóa lịch sử trong bộ nhớ khi rời chặng, để lần vào sau bắt đầu mới. */
    fun clearHistory(stage: Int) {
        history.remove(stage)
        historyNewCycle.remove(stage)
    }
}
