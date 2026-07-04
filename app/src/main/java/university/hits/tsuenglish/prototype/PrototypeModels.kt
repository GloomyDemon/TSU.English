package university.hits.tsuenglish.prototype

import university.hits.tsuenglish.core.dataclasses.Card
import university.hits.tsuenglish.core.dataclasses.Stats

enum class PrototypeTheme { SYSTEM, LIGHT, DARK }

enum class LearningAction { POSITIVE, NEGATIVE }

data class DictionaryInfo(
    val id: Long,
    val title: String,
    val description: String,
    val version: Int
)

data class DailyContent(
    val englishText: String,
    val russianText: String,
    val source: String,
    val videoTitle: String,
    val videoUrl: String,
    val hasSubtitles: Boolean
)

data class SettingsState(
    val selectedDictionaryId: Long,
    val dictionaries: List<DictionaryInfo>,
    val manualRepeatEnabled: Boolean,
    val theme: PrototypeTheme
)

data class StatisticsState(
    val selectedDictionary: DictionaryInfo,
    val stats: Stats,
    val learnedByDay: Map<String, Int>
)

sealed interface LearningCardUi {
    val card: Card
    val title: String
    val hiddenAnswer: String
    val positiveActionText: String
    val negativeActionText: String

    data class NewWord(
        override val card: Card,
        override val title: String,
        override val hiddenAnswer: String
    ) : LearningCardUi {
        override val positiveActionText: String = "Я уже знаю это слово"
        override val negativeActionText: String = "Я не знал этого слова"
    }

    data class RotationEnglishToRussian(
        override val card: Card,
        override val title: String,
        override val hiddenAnswer: String
    ) : LearningCardUi {
        override val positiveActionText: String = "Я запомнил это слово"
        override val negativeActionText: String = "Я не запомнил это слово"
    }

    data class RotationRussianToEnglish(
        override val card: Card,
        override val title: String,
        override val hiddenAnswer: String
    ) : LearningCardUi {
        override val positiveActionText: String = "Я запомнил это слово"
        override val negativeActionText: String = "Я не запомнил это слово"
    }
}

sealed interface LearningState {
    data object Loading : LearningState
    data class Ready(
        val dictionary: DictionaryInfo,
        val card: LearningCardUi,
        val answerVisible: Boolean = false,
        val examples: List<String> = emptyList()
    ) : LearningState
    data class Empty(val message: String) : LearningState
}
