package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.danhvan.tieuhoc.audio.VietnameseTts
import vn.danhvan.tieuhoc.data.NoRepeatPicker
import vn.danhvan.tieuhoc.data.WordBank

/**
 * SỬA 2026-10-29: 4 đáp án thành 2 HÀNG (2x2), hình nhỏ lại để vừa 1 màn hình,
 * chọn đáp án xong là dừng đọc luôn (StageScreen.answer đã tts.stop, ở đây
 * cũng stop trước khi chấm để giọng không đọc chồng).
 * Câu trước: chỉ xem lại, nút đáp án tắt, hiện đáp án đúng, không chấm lại.
 */
@Composable
fun Stage1Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    questionIndex: Int,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
) {
    val pick = remember {
        picker.pickAt(1, questionIndex, WordBank.words) { it.id }
    }
    LaunchedEffect(pick?.newCycle) { if (pick?.newCycle == true) onNewCycle() }
    val word = pick?.item
    if (word == null) {
        Text("Chưa có từ nào.", fontSize = 24.sp)
        return
    }
    if (!reviewOnly) {
        LaunchedEffect(word.id) { tts.speak(word.phatAm) }
    }
    var answered by remember { mutableStateOf(false) }
    var pickedOpt by remember { mutableStateOf<String?>(null) }
    val options = remember(word.id) {
        shuffledOptions(word.tu, distractWords(word.tu, WordBank.words.map { it.tu }))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nghe rồi chọn đúng từ nhé!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        // Nghe lại nằm sát ngay trên đáp án để bé bấm dễ.
        SpeakButton(textToSpeak = word.phatAm, tts = tts)
        Spacer(modifier = Modifier.height(12.dp))
        AnswerGrid2x2(
            options = options,
            enabled = !answered && !reviewOnly,
            selected = pickedOpt,
            correctOption = word.tu,
            answered = answered || reviewOnly,
            onPick = { opt ->
                if (!answered && !reviewOnly) {
                    answered = true
                    pickedOpt = opt
                    tts.stop()
                    onAnswer(opt == word.tu, word.tu)
                }
            },
        )
        // Xem lại: đáp án đúng hiện màu xanh lá ở ô đáp án, không viết chữ.
    }
}

/** Chặng 2: Hình chọn đúng từ. Chỉ dùng từ có hình, hình nhỏ lại vừa màn hình. */
@Composable
fun Stage2Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    questionIndex: Int,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
) {
    val pick = remember {
        picker.pickAt(2, questionIndex, WordBank.pictureWords) { it.id }
    }
    LaunchedEffect(pick?.newCycle) { if (pick?.newCycle == true) onNewCycle() }
    val word = pick?.item
    if (word == null) {
        Text("Chưa có từ có hình nào.", fontSize = 24.sp)
        return
    }
    var answered by remember { mutableStateOf(false) }
    var pickedOpt by remember { mutableStateOf<String?>(null) }
    val options = remember(word.id) {
        shuffledOptions(word.tu, distractWords(word.tu, WordBank.pictureWords.map { it.tu }))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nhìn hình rồi chọn đúng từ nhé!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        WordImage(wordId = word.id, modifier = Modifier.size(200.dp))
        Spacer(modifier = Modifier.height(10.dp))
        AnswerGrid2x2(
            options = options,
            enabled = !answered && !reviewOnly,
            selected = pickedOpt,
            correctOption = word.tu,
            answered = answered || reviewOnly,
            onPick = { opt ->
                if (!answered && !reviewOnly) {
                    answered = true
                    pickedOpt = opt
                    tts.stop()
                    onAnswer(opt == word.tu, word.tu)
                }
            },
        )
    }
}

/** Chặng 6: Nghe câu 3 từ, chọn đoạn chữ đúng. 4 đáp án thành 2 hàng. */
@Composable
fun Stage6Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    questionIndex: Int,
    reviewOnly: Boolean,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
) {
    val pick = remember {
        picker.pickAt(6, questionIndex, WordBank.sentences) { it.id }
    }
    LaunchedEffect(pick?.newCycle) { if (pick?.newCycle == true) onNewCycle() }
    val sentence = pick?.item
    if (sentence == null) {
        Text("Chưa có câu nào.", fontSize = 24.sp)
        return
    }
    if (!reviewOnly) {
        LaunchedEffect(sentence.id) { tts.speak(sentence.cauDung) }
    }
    var answered by remember { mutableStateOf(false) }
    var pickedOpt by remember { mutableStateOf<String?>(null) }
    val options = remember(sentence.id) { sentence.dapAnNhieu.shuffled() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nghe câu rồi chọn đoạn chữ đúng nhé!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        SpeakButton(textToSpeak = sentence.cauDung, tts = tts)
        Spacer(modifier = Modifier.height(12.dp))
        AnswerGrid2x2(
            options = options,
            enabled = !answered && !reviewOnly,
            selected = pickedOpt,
            correctOption = sentence.cauDung,
            answered = answered || reviewOnly,
            onPick = { opt ->
                if (!answered && !reviewOnly) {
                    answered = true
                    pickedOpt = opt
                    tts.stop()
                    onAnswer(opt == sentence.cauDung, sentence.cauDung)
                }
            },
        )
    }
}
