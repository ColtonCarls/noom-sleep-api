package com.noom.interview.fullstack.sleep.model

import java.time.Instant
import java.time.LocalDate

data class SleepLogResponse(
    val id: Long,
    val sleepDate: LocalDate,
    val bedTime: Instant,
    val wakeTime: Instant,
    val totalMinutesInBed: Int,
    val feeling: MorningFeeling
)
