package com.noom.interview.fullstack.sleep.model

import java.time.Instant
import java.time.LocalDate

/**
 * Response DTO for a single night's sleep log.
 * Excludes internal fields like `userId` and `createdAt`.
 *
 * @property id unique identifier of the sleep log.
 * @property sleepDate the calendar date this sleep is associated with.
 * @property bedTime when the user went to bed.
 * @property wakeTime when the user woke up.
 * @property totalMinutesInBed duration of sleep in minutes (derived from bed/wake times).
 * @property feeling how the user felt in the morning.
 */
data class SleepLogResponse(
    val id: Long,
    val sleepDate: LocalDate,
    val bedTime: Instant,
    val wakeTime: Instant,
    val totalMinutesInBed: Int,
    val feeling: MorningFeeling
)
