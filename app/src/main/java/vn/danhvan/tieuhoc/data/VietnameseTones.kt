package vn.danhvan.tieuhoc.data

/**
 * Bảng dấu tiếng Việt: mỗi nguyên âm gốc có 6 dạng, theo đúng thứ tự TONE_NAMES.
 * Dùng chung cho chặng 4 (ghép theo giọng đọc) và chặng 7 (nhìn hình tự xếp chữ).
 */
object VietnameseTones {
    val TONE_NAMES = listOf("không dấu", "huyền", "sắc", "hỏi", "ngã", "nặng")

    // Base -> [không dấu, huyền, sắc, hỏi, ngã, nặng]
    val TABLE: Map<String, List<String>> = mapOf(
        "a" to listOf("a", "à", "á", "ả", "ã", "ạ"),
        "ă" to listOf("ă", "ằ", "ắ", "ẳ", "ẵ", "ặ"),
        "â" to listOf("â", "ầ", "ấ", "ẩ", "ẫ", "ậ"),
        "e" to listOf("e", "è", "é", "ẻ", "ẽ", "ẹ"),
        "ê" to listOf("ê", "ề", "ế", "ể", "ễ", "ệ"),
        "i" to listOf("i", "ì", "í", "ỉ", "ĩ", "ị"),
        "o" to listOf("o", "ò", "ó", "ỏ", "õ", "ọ"),
        "ô" to listOf("ô", "ồ", "ố", "ổ", "ỗ", "ộ"),
        "ơ" to listOf("ơ", "ờ", "ớ", "ở", "ỡ", "ợ"),
        "u" to listOf("u", "ù", "ú", "ủ", "ũ", "ụ"),
        "ư" to listOf("ư", "ừ", "ứ", "ử", "ữ", "ự"),
        "y" to listOf("y", "ỳ", "ý", "ỷ", "ỹ", "ỵ"),
    )

    val BASE_VOWELS: List<String> get() = TABLE.keys.toList()

    private val reverse: Map<String, Pair<String, String>> by lazy {
        val m = mutableMapOf<String, Pair<String, String>>()
        for ((base, forms) in TABLE) {
            forms.forEachIndexed { idx, form -> m[form] = base to TONE_NAMES[idx] }
        }
        m
    }

    /** Tách nguyên âm đã có dấu thành (nguyên âm gốc, tên dấu). Ví dụ "ò" -> ("o", "huyền"). */
    fun decompose(tonedVowel: String): Pair<String, String> =
        reverse[tonedVowel] ?: (tonedVowel to "không dấu")

    /** Ghép nguyên âm gốc + dấu thành nguyên âm có dấu. Ví dụ ("o", "huyền") -> "ò". */
    fun apply(base: String, toneName: String): String {
        val idx = TONE_NAMES.indexOf(toneName).takeIf { it >= 0 } ?: 0
        return TABLE[base]?.getOrNull(idx) ?: base
    }
}
