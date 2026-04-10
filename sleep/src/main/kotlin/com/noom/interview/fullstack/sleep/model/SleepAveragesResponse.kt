package com.noom.interview.fullstack.sleep.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Response DTO for the 30-day sleep averages endpoint.
 * [startDate]/[endDate] define the window, and [feelingFrequencies]
 * holds the count of each morning feeling within that range.
 */
data class SleepAveragesResponse(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val averageTotalMinutesInBed: Int,
    val averageBedTime: LocalTime,
    val averageWakeTime: LocalTime,
    val feelingFrequencies: Map<MorningFeeling, Int>
)
