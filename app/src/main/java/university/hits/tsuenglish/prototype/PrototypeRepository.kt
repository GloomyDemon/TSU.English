package university.hits.tsuenglish.prototype

import university.hits.tsuenglish.core.dataclasses.Card
import university.hits.tsuenglish.core.dataclasses.Dictionary
import university.hits.tsuenglish.core.dataclasses.Word

class PrototypeRepository {
    private val dictionaryInfos = listOf(
        DictionaryInfo(1L, "Starter English", "Базовый словарь для прототипа", 1),
        DictionaryInfo(2L, "Travel English", "Слова для поездок и общения", 1)
    )

    fun dictionaries(): List<DictionaryInfo> = dictionaryInfos

    fun initialDictionaries(): Map<Long, Dictionary> = mapOf(
        1L to Dictionary(
            cards = setOf(
                card(101, "feel", "[fiːl]", "чувствовать", "I feel confident today.", "How do you feel?"),
                card(102, "sick", "[sɪk]", "больной", "She felt sick after lunch.", "He called in sick."),
                card(103, "learn", "[lɜːn]", "учить", "I learn English every day.", "Children learn quickly.")
            )
        ),
        2L to Dictionary(
            cards = setOf(
                card(201, "ticket", "[ˈtɪkɪt]", "билет", "I bought a train ticket.", "Show your ticket, please."),
                card(202, "luggage", "[ˈlʌɡɪdʒ]", "багаж", "My luggage is heavy.", "Where is the luggage claim?"),
                card(203, "boarding", "[ˈbɔːdɪŋ]", "посадка", "Boarding starts at six.", "The boarding gate has changed.")
            )
        )
    )

    fun dailyContent(): DailyContent = DailyContent(
        englishText = "The secret of getting ahead is getting started.",
        russianText = "Секрет движения вперёд в том, чтобы начать.",
        source = "Mark Twain, public-domain quotation collection",
        videoTitle = "English Listening Practice: Clear Slow Speech",
        videoUrl = "https://www.youtube.com/results?search_query=english+listening+practice+clear+speech+subtitles",
        hasSubtitles = true
    )

    private fun card(id: Long, english: String, transcript: String, russian: String, vararg examples: String): Card = Card(
        id = id,
        word = Word(english, transcript, examples.toSet()),
        translates = setOf(Word(russian, "", emptySet()))
    )
}
