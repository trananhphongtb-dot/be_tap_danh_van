package vn.danhvan.tieuhoc.data

/** Một từ 2 chữ cái trong bộ từ. tach[0] = phụ âm đầu, tach[1] = nguyên âm đã có dấu. */
data class Word(
    val id: String,
    val tu: String,
    val tach: List<String>,
    val nghia: String,
    val hinh: String,
    val coHinh: Boolean,
    val phatAm: String,
) {
    val phuAm: String get() = tach.getOrNull(0) ?: ""
    val nguyenAmCoDau: String get() = tach.getOrNull(1) ?: ""
}

/** Một đoạn văn chặng 3. dapAn = đúng 2 từ đọc được, không bấm vào từ trong đoạn. */
data class Passage(
    val id: String,
    val doan: String,
    val dapAn: List<String>,
    val luaChonSai: List<String>,
)

/** Một câu 3 từ chặng 6. dapAnNhieu gồm câu đúng + câu gây nhiễu. */
data class Sentence6(
    val id: String,
    val cauDung: String,
    val dapAnNhieu: List<String>,
)

/** Thông tin 1 chặng, theo đúng meta.chang_thi trong file dữ liệu. */
data class StageInfo(
    val number: Int,
    val ten: String,
    val moTa: String,
)
