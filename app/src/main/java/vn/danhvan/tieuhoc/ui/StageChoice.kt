package vn.danhvan.tieuhoc.ui

import androidx.compose.foundation.layout.Arrangement
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

/** Chặng 1: Nghe chọn đúng từ. Giọng đọc tiếng Việt, bé chọn đúng từ trong các lựa chọn. */
@Composable
fun Stage1Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
) {
    val pick = remember {
        picker.pick(1, WordBank.words) { it.id }
    }
    LaunchedEffect(pick?.newCycle) { if (pick?.newCycle == true) onNewCycle() }
    val word = pick?.item
    if (word == null) {
        Text("Chưa có từ nào.", fontSize = 24.sp)
        return
    }
    LaunchedEffect(word.id) { tts.speak(word.phatAm) }
    var answered by remember { mutableStateOf(false) }
    val options = remember(word.id) {
        shuffledOptions(word.tu, distractWords(word.tu, WordBank.words.map { it.tu }))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nghe rồi chọn đúng từ nhé!", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        SpeakButton(textToSpeak = word.phatAm, tts = tts)
        Spacer(modifier = Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (opt in options) {
                BigButton(
                    text = opt,
                    onClick = {
                        if (!answered) {
                            answered = true
                            onAnswer(opt == word.tu, word.tu)
                        }
                    },
                    enabled = !answered,
                    color = KidBlue,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Chặng 2: Hình chọn đúng từ. Chỉ dùng từ có hình, hiện hình, bé chọn đúng từ. */
@Composable
fun Stage2Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
) {
    val pick = remember {
        picker.pick(2, WordBank.pictureWords) { it.id }
    }
    LaunchedEffect(pick?.newCycle) { if (pick?.newCycle == true) onNewCycle() }
    val word = pick?.item
    if (word == null) {
        Text("Chưa có từ có hình nào.", fontSize = 24.sp)
        return
    }
    var answered by remember { mutableStateOf(false) }
    val options = remember(word.id) {
        shuffledOptions(word.tu, distractWords(word.tu, WordBank.pictureWords.map { it.tu }))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nhìn hình rồi chọn đúng từ nhé!", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        // Không hiện nghĩa bằng chữ ở đây, vì với nhiều từ nghĩa chính là từ đó,
        // hiện ra sẽ lộ đáp án. Bé chỉ nhìn hình để chọn từ.
        WordImage(wordId = word.id, modifier = Modifier.size(280.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (opt in options) {
                BigButton(
                    text = opt,
                    onClick = {
                        if (!answered) {
                            answered = true
                            onAnswer(opt == word.tu, word.tu)
                        }
                    },
                    enabled = !answered,
                    color = KidBlue,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Chặng 6: Nghe câu 3 từ, chọn đoạn chữ đúng. Câu gây nhiễu đổi chủ ngữ / tân ngữ / động từ. */
@Composable
fun Stage6Content(
    tts: VietnameseTts,
    picker: NoRepeatPicker,
    onAnswer: (Boolean, String) -> Unit,
    onNewCycle: () -> Unit,
) {
    val pick = remember {
        picker.pick(6, WordBank.sentences) { it.id }
    }
    LaunchedEffect(pick?.newCycle) { if (pick?.newCycle == true) onNewCycle() }
    val sentence = pick?.item
    if (sentence == null) {
        Text("Chưa có câu nào.", fontSize = 24.sp)
        return
    }
    LaunchedEffect(sentence.id) { tts.speak(sentence.cauDung) }
    var answered by remember { mutableStateOf(false) }
    val options = remember(sentence.id) { sentence.dapAnNhieu.shuffled() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nghe câu rồi chọn đoạn chữ đúng nhé!", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        SpeakButton(textToSpeak = sentence.cauDung, tts = tts)
        Spacer(modifier = Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (opt in options) {
                BigButton(
                    text = opt,
                    onClick = {
                        if (!answered) {
                            answered = true
                            onAnswer(opt == sentence.cauDung, sentence.cauDung)
                        }
                    },
                    enabled = !answered,
                    color = KidBlue,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
