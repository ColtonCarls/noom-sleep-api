package com.noom.interview.fullstack.sleep.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Response DTO for the 30-day sleep averages endpoint.
 *
 * @property startDate beginning of the 30-day window (inclusive).
 * @property endDate end of the 30-day window (inclusive, typically today).
 * @property averageTotalMinutesInBed mean sleep duration across the window.
 * @property averageBedTime average time the user went to bed.
 * @property averageWakeTime average time the user woke up.
 * @property feelingFrequencies count of each [MorningFeeling] within the window;
 *           all enum values are present even if count is 0.
 */
data class SleepAveragesResponse(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val averageTotalMinutesInBed: Int,
    val averageBedTime: LocalTime,
    val averageWakeTime: LocalTime,
    val feelingFrequencies: Map<MorningFeeling, Int>
)
