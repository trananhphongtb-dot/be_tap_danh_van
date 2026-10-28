package vn.danhvan.tieuhoc.data

/**
 * Luật điểm chung mọi chặng, theo đúng chốt của người dùng:
 * - 10 điểm thì qua chặng.
 * - Đúng +1 điểm.
 * - Sai 2 câu trừ 1 điểm, và chỉ trừ ở câu sai thứ 2 (câu sai thứ 1 chưa trừ).
 *
 * TODO (chưa được người dùng xác nhận, đang dùng tạm cách dưới đây, có thể đổi
 * bằng cách sửa đúng 2 hằng số / hàm này, không cần sửa từng màn hình):
 * - Bộ đếm sai đang là CỘNG DỒN trong 1 lần vào chặng (sai thứ 2, 4, 6... đều trừ),
 *   chưa xác nhận là cộng dồn hay chỉ tính sai liên tiếp.
 * - Điểm đang có SÀN 0 (bé không bao giờ âm điểm), chưa xác nhận sàn 0 hay không.
 * Đổi FLOOR_AT_ZERO thành false nếu sau này người dùng muốn cho điểm âm.
 */
object GameRules {
    const val POINTS_TO_PASS = 10
    const val WRONG_PER_DEDUCTION = 2
    const val FLOOR_AT_ZERO = true

    /** Trả về (điểm mới, số câu sai mới) sau 1 câu trả lời. */
    fun applyAnswer(score: Int, wrongCount: Int, correct: Boolean): Pair<Int, Int> {
        if (correct) return (score + 1) to wrongCount
        val newWrong = wrongCount + 1
        var newScore = score
        if (newWrong % WRONG_PER_DEDUCTION == 0) {
            newScore = score - 1
            if (FLOOR_AT_ZERO && newScore < 0) newScore = 0
        }
        return newScore to newWrong
    }
}

/** Mở khóa chặng: chặng sau mở khi chặng trước đã đủ 10 điểm 1 lần. */
object UnlockRules {
    // TODO: nếu người dùng sau này muốn mở hết mọi chặng ngay từ đầu,
    // chỉ cần đổi hằng số này thành true, không sửa màn hình chính.
    const val UNLOCK_ALL = false
}
