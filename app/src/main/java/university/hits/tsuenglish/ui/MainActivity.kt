package university.hits.tsuenglish.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import university.hits.tsuenglish.prototype.DailyContent
import university.hits.tsuenglish.prototype.EnglishLearningPrototype
import university.hits.tsuenglish.prototype.LearningAction
import university.hits.tsuenglish.prototype.LearningCardUi
import university.hits.tsuenglish.prototype.LearningState
import university.hits.tsuenglish.prototype.PrototypeTheme
import university.hits.tsuenglish.prototype.SettingsState
import university.hits.tsuenglish.prototype.StatisticsState
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private val prototype = EnglishLearningPrototype()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TsuEnglishApp(
                prototype = prototype,
                onOpenUrl = { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            )
        }
    }
}

@Composable
private fun TsuEnglishApp(
    prototype: EnglishLearningPrototype,
    onOpenUrl: (String) -> Unit
) {
    var learningState by remember { mutableStateOf(prototype.learningState()) }
    var statisticsState by remember { mutableStateOf<StatisticsState?>(null) }
    var settingsState by remember { mutableStateOf<SettingsState?>(null) }
    var dailyContent by remember { mutableStateOf<DailyContent?>(null) }
    var selectedTheme by remember { mutableStateOf(prototype.settingsState().theme) }
    val colors = if (selectedTheme == PrototypeTheme.DARK) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF7F7FB)
        ) {
            LearningScreen(
                state = learningState,
                onShowAnswer = { learningState = prototype.showAnswer() },
                onAction = { action -> learningState = prototype.applyAction(action) },
                onOpenStatistics = { statisticsState = prototype.statisticsState() },
                onOpenSettings = { settingsState = prototype.settingsState() },
                onOpenDailyContent = { dailyContent = prototype.dailyContent() }
            )
        }
    }

    statisticsState?.let { stats ->
        StatisticsDialog(stats = stats, onDismiss = { statisticsState = null })
    }

    settingsState?.let { settings ->
        SettingsDialog(
            settings = settings,
            onSelectDictionary = { id ->
                settingsState = prototype.selectDictionary(id)
                learningState = prototype.learningState()
            },
            onToggleManualRepeat = {
                settingsState = prototype.setManualRepeat(!settings.manualRepeatEnabled)
                learningState = prototype.learningState()
            },
            onSetTheme = {
                selectedTheme = it
                settingsState = prototype.setTheme(it)
            },
            onResetDictionary = {
                learningState = prototype.resetSelectedDictionary()
                settingsState = null
            },
            onResetAll = {
                learningState = prototype.resetAllProgress()
                settingsState = null
            },
            onDismiss = { settingsState = null }
        )
    }

    dailyContent?.let { content ->
        DailyContentDialog(
            content = content,
            onOpenUrl = onOpenUrl,
            onDismiss = { dailyContent = null }
        )
    }
}

@Composable
private fun LearningScreen(
    state: LearningState,
    onShowAnswer: () -> Unit,
    onAction: (LearningAction) -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDailyContent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TSU English",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )
        Text(
            text = "Карточки, интервальные повторения и локальный прогресс",
            textAlign = TextAlign.Center,
            color = Color(0xFF5A5A62),
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )
        Row(horizontalArrangement = Arrangement.Center) {
            Button(onClick = onOpenStatistics) { Text("Статистика") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onOpenSettings) { Text("Настройки") }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onOpenDailyContent) { Text("Контент дня") }
        Spacer(Modifier.height(20.dp))

        when (state) {
            LearningState.Loading -> InfoCard("Загружаем словарь…")
            is LearningState.Empty -> InfoCard(state.message)
            is LearningState.Ready -> LearningCard(
                state = state,
                onShowAnswer = onShowAnswer,
                onAction = onAction
            )
        }
    }
}

@Composable
private fun LearningCard(
    state: LearningState.Ready,
    onShowAnswer: () -> Unit,
    onAction: (LearningAction) -> Unit
) {
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val card = state.card

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(card.card.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(dragDistance) > 120f) {
                            onAction(if (dragDistance > 0) LearningAction.POSITIVE else LearningAction.NEGATIVE)
                        }
                        dragDistance = 0f
                    },
                    onHorizontalDrag = { _, dragAmount -> dragDistance += dragAmount }
                )
            },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Словарь: ${state.dictionary.title}", color = Color(0xFF5A5A62))
            Spacer(Modifier.height(10.dp))
            Text(card.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(directionLabel(card), color = Color(0xFF5A5A62), modifier = Modifier.padding(top = 6.dp))
            Text(
                text = if (state.answerVisible) card.hiddenAnswer else "••••••",
                fontSize = 34.sp,
                color = Color(0xFF3F51B5),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 24.dp)
            )
            Button(onClick = onShowAnswer, enabled = !state.answerVisible) {
                Text(if (state.answerVisible) "Ответ открыт" else "Показать слово")
            }
            if (state.answerVisible && state.examples.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text("Примеры:", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                state.examples.forEach { example ->
                    Text("• $example", modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                    onClick = { onAction(LearningAction.NEGATIVE) }
                ) { Text(card.negativeActionText) }
                Button(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    onClick = { onAction(LearningAction.POSITIVE) }
                ) { Text(card.positiveActionText) }
            }
            Text(
                text = "Свайп влево = отрицательный ответ, свайп вправо = положительный.",
                color = Color(0xFF5A5A62),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun StatisticsDialog(stats: StatisticsState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        title = { Text("Статистика") },
        text = {
            Column {
                Text("Словарь: ${stats.selectedDictionary.title}")
                Text("Всего слов: ${stats.stats.wordsTotal}")
                Text("Выучено: ${stats.stats.completed}")
                Text("В ротации: ${stats.stats.inProcess}")
                Text("Уже знал: ${stats.stats.alreadyKnown}")
                Text("Осталось: ${stats.stats.wordsLeft}")
                Spacer(Modifier.height(12.dp))
                Text("Выучено по дням:", fontWeight = FontWeight.Bold)
                if (stats.learnedByDay.isEmpty()) {
                    Text("Пока нет завершённых повторений")
                } else {
                    stats.learnedByDay.forEach { (day, count) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(day, modifier = Modifier.width(96.dp))
                            Box(
                                Modifier
                                    .height(16.dp)
                                    .width((24 * count).dp)
                                    .background(Color(0xFF3F51B5), RoundedCornerShape(8.dp))
                            )
                            Text(" $count")
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun SettingsDialog(
    settings: SettingsState,
    onSelectDictionary: (Long) -> Unit,
    onToggleManualRepeat: () -> Unit,
    onSetTheme: (PrototypeTheme) -> Unit,
    onResetDictionary: () -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
        title = { Text("Настройки") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Выбор словаря", fontWeight = FontWeight.Bold)
                settings.dictionaries.forEach { dictionary ->
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelectDictionary(dictionary.id) }
                    ) {
                        Text(if (dictionary.id == settings.selectedDictionaryId) "✓ ${dictionary.title}" else dictionary.title)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(modifier = Modifier.fillMaxWidth(), onClick = onToggleManualRepeat) {
                    Text(if (settings.manualRepeatEnabled) "Выключить повторение" else "Повторить выученные")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onSetTheme(PrototypeTheme.LIGHT) }) { Text("День") }
                    TextButton(onClick = { onSetTheme(PrototypeTheme.DARK) }) { Text("Ночь") }
                    TextButton(onClick = { onSetTheme(PrototypeTheme.SYSTEM) }) { Text("Система") }
                }
                Button(modifier = Modifier.fillMaxWidth(), onClick = onResetDictionary) { Text("Сбросить словарь") }
                Button(modifier = Modifier.fillMaxWidth(), onClick = onResetAll) { Text("Сбросить всё") }
            }
        }
    )
}

@Composable
private fun DailyContentDialog(
    content: DailyContent,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onOpenUrl(content.videoUrl) }) { Text("Открыть видео") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
        title = { Text("Контент дня") },
        text = {
            Column {
                Text(content.englishText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(content.russianText)
                Spacer(Modifier.height(10.dp))
                Text("Источник: ${content.source}")
                Text("Видео: ${content.videoTitle}")
                Text("Субтитры: ${if (content.hasSubtitles) "желательно/доступны" else "не указаны"}")
                Text(content.videoUrl, color = Color(0xFF3F51B5))
            }
        }
    )
}

@Composable
private fun InfoCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(
            text = message,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp).fillMaxWidth()
        )
    }
}

private fun directionLabel(card: LearningCardUi): String = when (card) {
    is LearningCardUi.NewWord -> "Новое слово"
    is LearningCardUi.RotationEnglishToRussian -> "Повторение: английский → русский"
    is LearningCardUi.RotationRussianToEnglish -> "Повторение: русский → английский"
}
