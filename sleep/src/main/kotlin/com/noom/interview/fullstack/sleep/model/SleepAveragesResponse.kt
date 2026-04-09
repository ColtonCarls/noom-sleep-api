package com.noom.interview.fullstack.sleep.model

import java.time.LocalDate
import java.time.LocalTime

data class SleepAveragesResponse(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val averageTotalMinutesInBed: Int,
    val averageBedTime: LocalTime,
    val averageWakeTime: LocalTime,
    val feelingFrequencies: Map<MorningFeeling, Int>
)
