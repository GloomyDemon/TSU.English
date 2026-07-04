package university.hits.tsuenglish.prototype

import university.hits.tsuenglish.core.classes.Manager
import university.hits.tsuenglish.core.dataclasses.Card
import java.time.format.DateTimeFormatter

class EnglishLearningPrototype(
    private val repository: PrototypeRepository = PrototypeRepository(),
    private val manager: Manager = Manager()
) {
    private var selectedDictionaryId: Long = repository.dictionaries().first().id
    private var theme: PrototypeTheme = PrototypeTheme.SYSTEM
    private var currentState: LearningState = LearningState.Loading

    init {
        manager.selectedDictionaryId = selectedDictionaryId
        manager.loadDictionaries(repository.initialDictionaries())
        loadNextCard()
    }

    fun learningState(): LearningState = currentState

    fun showAnswer(): LearningState {
        currentState = when (val state = currentState) {
            is LearningState.Ready -> state.copy(answerVisible = true)
            else -> state
        }
        return currentState
    }

    fun applyAction(action: LearningAction): LearningState {
        val state = currentState as? LearningState.Ready ?: return currentState
        when (action) {
            LearningAction.POSITIVE -> manager.applyLike(state.card.card, selectedDictionaryId)
            LearningAction.NEGATIVE -> manager.applyDislike(state.card.card, selectedDictionaryId)
        }
        return loadNextCard()
    }

    fun selectDictionary(dictionaryId: Long): SettingsState {
        require(repository.dictionaries().any { it.id == dictionaryId }) { "Unknown dictionary id: $dictionaryId" }
        selectedDictionaryId = dictionaryId
        manager.selectedDictionaryId = dictionaryId
        loadNextCard()
        return settingsState()
    }

    fun setTheme(newTheme: PrototypeTheme): SettingsState {
        theme = newTheme
        return settingsState()
    }

    fun setManualRepeat(enabled: Boolean): SettingsState {
        manager.isManualRepeat = enabled
        loadNextCard()
        return settingsState()
    }

    fun resetSelectedDictionary(): LearningState {
        manager.clearDictionaryProgress(selectedDictionaryId)
        return loadNextCard()
    }

    fun resetAllProgress(): LearningState {
        manager.clearAllProgress()
        return loadNextCard()
    }

    fun settingsState(): SettingsState = SettingsState(
        selectedDictionaryId = selectedDictionaryId,
        dictionaries = repository.dictionaries(),
        manualRepeatEnabled = manager.isManualRepeat,
        theme = theme
    )

    fun statisticsState(): StatisticsState {
        val selected = repository.dictionaries().first { it.id == selectedDictionaryId }
        val learnedByDay = manager.getDailyCompletionCounts(selectedDictionaryId)
            .mapKeys { it.key.format(DateTimeFormatter.ISO_LOCAL_DATE) }
        return StatisticsState(selected, manager.getDictionaryStats(selectedDictionaryId), learnedByDay)
    }

    fun dailyContent(): DailyContent = repository.dailyContent()

    private fun loadNextCard(): LearningState {
        val card = manager.next()
        currentState = if (card == null) {
            LearningState.Empty("На сейчас карточек нет: повторения ещё не наступили или словарь завершён.")
        } else {
            val selected = repository.dictionaries().first { it.id == selectedDictionaryId }
            LearningState.Ready(
                dictionary = selected,
                card = card.toUiCard(),
                examples = card.word.examples.toList()
            )
        }
        return currentState
    }

    private fun Card.toUiCard(): LearningCardUi {
        val translation = translates.firstOrNull()?.script.orEmpty()
        return when {
            isNew -> LearningCardUi.NewWord(this, "${word.script} ${word.transcript}", translation)
            id % 2L == 0L -> LearningCardUi.RotationRussianToEnglish(this, translation, "${word.script} ${word.transcript}")
            else -> LearningCardUi.RotationEnglishToRussian(this, "${word.script} ${word.transcript}", translation)
        }
    }
}
