package com.noom.interview.fullstack.sleep.model

import java.time.Instant
import java.time.LocalDate

data class SleepLog(
    val id: Long,
    val userId: Long,
    val bedTime: Instant,
    val wakeTime: Instant,
    val feeling: MorningFeeling,
    val sleepDate: LocalDate,
    val totalMinutesInBed: Int,
    val createdAt: Instant
)
