package vn.danhvan.tieuhoc.data

import android.content.Context
import org.json.JSONObject

/** Nạp file assets/bo-tu-2-chu-cai.json nguyên vẹn, không sửa nội dung file gốc. */
object WordBank {
    var words: List<Word> = emptyList()
        private set
    var passages: List<Passage> = emptyList()
        private set
    var sentences: List<Sentence6> = emptyList()
        private set
    var stages: List<StageInfo> = emptyList()
        private set

    val pictureWords: List<Word> get() = words.filter { it.coHinh }
    val initials: List<String> get() = words.map { it.phuAm }.distinct().sorted()
    val wordById: Map<String, Word> get() = words.associateBy { it.id }

    var loaded = false
        private set

    fun load(context: Context) {
        if (loaded) return
        val text = context.assets.open("bo-tu-2-chu-cai.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)
        val tuArr = root.getJSONArray("tu")
        val list = mutableListOf<Word>()
        for (i in 0 until tuArr.length()) {
            val o = tuArr.getJSONObject(i)
            val tachArr = o.getJSONArray("tach")
            list += Word(
                id = o.getString("id"),
                tu = o.getString("tu"),
                tach = List(tachArr.length()) { tachArr.getString(it) },
                nghia = o.optString("nghia", ""),
                hinh = o.optString("hinh", ""),
                coHinh = o.optBoolean("co_hinh", false),
                phatAm = o.optString("phat_am", o.getString("tu")),
            )
        }
        words = list

        val meta = root.getJSONObject("meta")
        val changArr = meta.getJSONArray("chang_thi")
        val stageList = mutableListOf<StageInfo>()
        for (i in 0 until changArr.length()) {
            val o = changArr.getJSONObject(i)
            stageList += StageInfo(
                number = o.getInt("thu_tu"),
                ten = o.getString("ten"),
                moTa = o.optString("cach_choi", ""),
            )
        }
        // Đảm bảo đủ 7 chặng theo thứ tự, nếu file thiếu thì dùng tên chốt trong README.
        stages = stageList.sortedBy { it.number }

        val pArr = meta.getJSONArray("doan_van_chang3")
        val pList = mutableListOf<Passage>()
        for (i in 0 until pArr.length()) {
            val o = pArr.getJSONObject(i)
            val da = o.getJSONArray("dap_an")
            val sai = if (o.has("lua_chon_sai")) o.getJSONArray("lua_chon_sai") else null
            pList += Passage(
                id = o.getString("id"),
                doan = o.getString("doan"),
                dapAn = List(da.length()) { da.getString(it) },
                luaChonSai = if (sai != null) List(sai.length()) { sai.getString(it) } else emptyList(),
            )
        }
        passages = pList

        val sArr = meta.getJSONArray("cau_nghe_chang6")
        val sList = mutableListOf<Sentence6>()
        for (i in 0 until sArr.length()) {
            val o = sArr.getJSONObject(i)
            val opts = o.getJSONArray("dap_an_nhieu")
            sList += Sentence6(
                id = o.getString("id"),
                cauDung = o.getString("cau_dung"),
                dapAnNhieu = List(opts.length()) { opts.getString(it) },
            )
        }
        sentences = sList
        loaded = true
    }

    fun wordByText(tu: String): Word? = words.firstOrNull { it.tu == tu }
}
