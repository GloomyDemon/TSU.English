package university.hits.tsuenglish.core.enums

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.ZERO

enum class PauseTime(val duration: Duration) {
    none(ZERO),
    five_minutes(5.minutes),
    hour(1.hours),
    day(1.days),
    three_days(3.days),
    week(7.days),
    two_weeks(14.days),
    month(28.days),
    three_months(84.days);

    fun next(): PauseTime {
        return entries[this.ordinal + 1]
    }
}