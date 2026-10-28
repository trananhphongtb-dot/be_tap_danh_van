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
 */
class NoRepeatPicker(private val store: ProgressStore) {
    data class Pick<T>(val item: T, val newCycle: Boolean)

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
}
